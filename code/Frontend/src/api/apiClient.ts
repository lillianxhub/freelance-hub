import type { ApiOptions, JsonMethod } from '../types/api'
import { ApiError } from './apiError'

const tokenKey = 'freelance-hub-api-token'
const apiBase = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function readErrorMessage(body: unknown): string | undefined {
  if (!isRecord(body)) return undefined
  if (typeof body.message === 'string' && body.message) return body.message
  if (!isRecord(body.errors)) return undefined
  return Object.values(body.errors).find((error): error is string => typeof error === 'string' && Boolean(error))
}

export function getApiToken(): string | null {
  return localStorage.getItem(tokenKey)
}

export function setApiToken(token: string | null): void {
  if (token) localStorage.setItem(tokenKey, token)
  else localStorage.removeItem(tokenKey)
}

async function request<T>(path: string, init: RequestInit, multipart = false): Promise<T> {
  const headers = new Headers(init.headers)
  if (multipart) headers.delete('Content-Type')
  else if (typeof init.body === 'string' && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = getApiToken()
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${apiBase}${path}`, { ...init, headers })
  if (!response.ok) {
    let message = `API error (${response.status})`
    try {
      const body: unknown = await response.json()
      message = readErrorMessage(body) || message
    } catch { /* The server did not return JSON. */ }
    throw new ApiError(message, response.status)
  }
  if (response.status === 204 || response.status === 205) return undefined as T
  return response.json() as Promise<T>
}

export function fetchClient<T>(path: string, init: RequestInit = {}): Promise<T> {
  return request<T>(path, init)
}

export function fetchMultipartClient<T>(path: string, formData: FormData, init: Omit<RequestInit, 'body'> = {}): Promise<T> {
  return request<T>(path, { ...init, method: init.method || 'POST', body: formData }, true)
}

// Keep the existing request API available to current callers.
export const apiRequest = fetchClient

function jsonRequest<T>(method: JsonMethod, path: string, body: unknown, options: ApiOptions = {}): Promise<T> {
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
