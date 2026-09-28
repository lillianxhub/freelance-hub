import type { ApiMeta, ApiOptions, ApiResponse, JsonMethod } from '../types/api'
import { ApiError } from './apiError'

const apiBase = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
let accessToken: string | null = null
let refreshInFlight: Promise<boolean> | null = null

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function readErrorMessage(body: unknown): string | undefined {
  if (!isRecord(body)) return undefined
  if (typeof body.message === 'string' && body.message) return body.message
  if (isRecord(body.error) && typeof body.error.message === 'string' && body.error.message) return body.error.message
  if (!isRecord(body.errors)) return undefined
  return Object.values(body.errors).find((error): error is string => typeof error === 'string' && Boolean(error))
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  const validMeta = value !== null && isRecord(value) && (
    value.meta === null ||
    (isRecord(value.meta) &&
      typeof value.meta.page === 'number' &&
      typeof value.meta.limit === 'number' &&
      typeof value.meta.total === 'number' &&
      typeof value.meta.totalPages === 'number')
  )
  const validError = value !== null && isRecord(value) && (value.error === null || isRecord(value.error))
  return isRecord(value) &&
    typeof value.success === 'boolean' &&
    typeof value.message === 'string' &&
    'data' in value &&
    validMeta &&
    validError
}

interface LegacyPage {
  content: unknown[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

function isLegacyPage(value: unknown): value is LegacyPage {
  return isRecord(value) &&
    Array.isArray(value.content) &&
    typeof value.totalElements === 'number' &&
    typeof value.totalPages === 'number' &&
    typeof value.size === 'number' &&
    typeof value.number === 'number'
}

function legacyPageMeta(page: LegacyPage): ApiMeta {
  return {
    page: page.number + 1,
    limit: page.size,
    total: page.totalElements,
    totalPages: page.totalPages,
  }
}

function normalizeResponse<T>(body: unknown): ApiResponse<T> {
  if (isApiResponse(body)) {
    if (isLegacyPage(body.data)) {
      return {
        ...body,
        data: body.data.content as T,
        meta: body.meta || legacyPageMeta(body.data),
      }
    }
    return body as ApiResponse<T>
  }

  if (isLegacyPage(body)) {
    return {
      success: true,
      message: '',
      data: body.content as T,
      meta: legacyPageMeta(body),
      error: null,
    }
  }

  return {
    success: true,
    message: '',
    data: body as T,
    meta: null,
    error: null,
  }
}

export function getApiToken(): string | null {
  return accessToken
}

export function setApiToken(token: string | null): void {
  accessToken = token
}

async function rotateAccessToken(): Promise<boolean> {
  const response = await fetch(`${apiBase}/auth/refresh`, { method: 'POST', credentials: 'same-origin' })
  if (!response.ok) {
    setApiToken(null)
    return false
  }
  const body: unknown = await response.json()
  if (!isRecord(body) || !isRecord(body.data) || typeof body.data.token !== 'string') {
    setApiToken(null)
    return false
  }
  setApiToken(body.data.token)
  return true
}

export function refreshApiToken(): Promise<boolean> {
  if (!refreshInFlight) {
    const rotate = async () => {
      if (typeof navigator !== 'undefined' && navigator.locks) {
        return navigator.locks.request('freelance-hub-refresh', rotateAccessToken)
      }
      return rotateAccessToken()
    }
    refreshInFlight = rotate().catch(() => {
      setApiToken(null)
      return false
    }).finally(() => { refreshInFlight = null })
  }
  return refreshInFlight
}

async function request<T>(path: string, init: RequestInit, multipart = false): Promise<ApiResponse<T>> {
  const headers = new Headers(init.headers)
  if (multipart) headers.delete('Content-Type')
  else if (typeof init.body === 'string' && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = getApiToken()
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)

  let response = await fetch(`${apiBase}${path}`, { ...init, headers, credentials: 'same-origin' })
  if (response.status === 401 && !path.startsWith('/auth/') && await refreshApiToken()) {
    headers.set('Authorization', `Bearer ${getApiToken()}`)
    response = await fetch(`${apiBase}${path}`, { ...init, headers, credentials: 'same-origin' })
  }
  if (!response.ok) {
    let message = `API error (${response.status})`
    try {
      const body: unknown = await response.json()
      message = readErrorMessage(body) || message
    } catch { /* The server did not return JSON. */ }
    throw new ApiError(message, response.status)
  }
  if (response.status === 204 || response.status === 205) {
    return { success: true, message: '', data: undefined as T, meta: null, error: null }
  }

  const body: unknown = await response.json()
  const normalized = normalizeResponse<T>(body)
  if (!normalized.success) throw new ApiError(readErrorMessage(normalized) || 'API ไม่สามารถดำเนินการได้', response.status)
  return normalized
}

export function fetchClient<T>(path: string, init: RequestInit = {}): Promise<ApiResponse<T>> {
  return request<T>(path, init)
}

export function fetchMultipartClient<T>(path: string, formData: FormData, init: Omit<RequestInit, 'body'> = {}): Promise<ApiResponse<T>> {
  return request<T>(path, { ...init, method: init.method || 'POST', body: formData }, true)
}

// Keep the existing request API available to current callers.
export const apiRequest = fetchClient

function jsonRequest<T>(method: JsonMethod, path: string, body: unknown, options: ApiOptions = {}): Promise<ApiResponse<T>> {
  return fetchClient<T>(path, {
    ...options,
    method,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export const api = {
  get: <T>(path: string, options: ApiOptions = {}) => fetchClient<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options: ApiOptions = {}) => jsonRequest<T>('POST', path, body, options),
  put: <T>(path: string, body?: unknown, options: ApiOptions = {}) => jsonRequest<T>('PUT', path, body, options),
  patch: <T>(path: string, body?: unknown, options: ApiOptions = {}) => jsonRequest<T>('PATCH', path, body, options),
  delete: <T = void>(path: string, options: ApiOptions = {}) => fetchClient<T>(path, { ...options, method: 'DELETE' }),
}
