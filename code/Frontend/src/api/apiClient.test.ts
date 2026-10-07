import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError } from './apiError'
import { api, fetchClient, fetchMultipartClient, setApiToken } from './apiClient'

const values = new Map<string, string>()
Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => { values.set(key, value) },
    removeItem: (key: string) => { values.delete(key) },
  },
})

test('api helpers send JSON and attach the in-memory token', async () => {
  const originalFetch = globalThis.fetch
  setApiToken('test-token')
  globalThis.fetch = async (input, init) => {
    assert.equal(input, '/api/items')
    assert.equal(init?.method, 'POST')
    assert.equal(init?.body, JSON.stringify({ name: 'Acme' }))
    const headers = new Headers(init?.headers)
    assert.equal(headers.get('Content-Type'), 'application/json')
    assert.equal(headers.get('Authorization'), 'Bearer test-token')
    return new Response(JSON.stringify({ success: true, message: 'สร้างสำเร็จ', data: { id: '1' }, meta: null, error: null }), { status: 201 })
  }
  try {
    assert.deepEqual((await api.post<{ id: string }>('/items', { name: 'Acme' })).data, { id: '1' })
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('401 rotates the refresh cookie once and retries with the new access token', async () => {
  const originalFetch = globalThis.fetch
  let calls = 0
  setApiToken('expired-access')
  globalThis.fetch = async (input, init) => {
    calls++
    assert.equal(init?.credentials, 'same-origin')
    if (input === '/api/auth/refresh') {
      return new Response(JSON.stringify({ data: { token: 'renewed-access' } }), { status: 200 })
    }
    const authorization = new Headers(init?.headers).get('Authorization')
    if (authorization === 'Bearer expired-access') return new Response(null, { status: 401 })
    assert.equal(authorization, 'Bearer renewed-access')
    return new Response(JSON.stringify({ success: true, message: '', data: { id: '1' }, meta: null, error: null }), { status: 200 })
  }
  try {
    assert.deepEqual((await api.get<{ id: string }>('/items')).data, { id: '1' })
    assert.equal(calls, 3)
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('multipart wrapper passes FormData and leaves Content-Type to fetch', async () => {
  const originalFetch = globalThis.fetch
  const formData = new FormData()
  formData.append('file', new Blob(['hello']), 'note.txt')
  globalThis.fetch = async (_input, init) => {
    assert.equal(init?.method, 'PATCH')
    assert.equal(init?.body, formData)
    assert.equal(new Headers(init?.headers).has('Content-Type'), false)
    return new Response(JSON.stringify({ success: true, message: '', data: { uploaded: true }, meta: null, error: null }), { status: 200 })
  }
  try {
    const response = await fetchMultipartClient<{ uploaded: boolean }>('/upload', formData, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
    })
    assert.deepEqual(response.data, { uploaded: true })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('request wrappers keep ApiError status and handle empty responses', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Denied' }), { status: 401 })
  try {
    await assert.rejects(fetchClient('/private'), (error: unknown) => error instanceof ApiError && error.status === 401 && error.message === 'Denied')
  } finally {
    globalThis.fetch = originalFetch
  }

  globalThis.fetch = async (_input, init) => {
    assert.equal(init?.method, 'DELETE')
    return new Response(null, { status: 204 })
  }
  try {
    const response = await api.delete('/items/1')
    assert.equal(response.success, true)
    assert.equal(response.data, undefined)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('error responses ignore fields with unexpected types', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({ message: { text: 'bad' }, errors: { email: 123 } }), { status: 400 })
  try {
    await assert.rejects(fetchClient('/invalid'), (error: unknown) => error instanceof ApiError && error.message === 'API error (400)')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('response exposes pagination metadata from the shared API format', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: true,
    message: '',
    data: [{ id: '1' }],
    meta: { page: 1, limit: 20, total: 125, totalPages: 7 },
    error: null,
  }), { status: 200 })
  try {
    const response = await api.get<Array<{ id: string }>>('/items')
    assert.deepEqual(response.data, [{ id: '1' }])
    assert.deepEqual(response.meta, { page: 1, limit: 20, total: 125, totalPages: 7 })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('concurrent identical GETs share one request, then a later GET is fresh', async () => {
  const originalFetch = globalThis.fetch
  let calls = 0
  let finishFirst: ((response: Response) => void) | undefined
  const success = () => Response.json({ success: true, message: '', data: { id: '1' }, meta: null, error: null })
  globalThis.fetch = async () => {
    calls++
    if (calls === 1) return new Promise<Response>((resolve) => { finishFirst = resolve })
    return success()
  }
  try {
    const first = api.get<{ id: string }>('/shared-items')
    const second = api.get<{ id: string }>('/shared-items')
    assert.equal(calls, 1)
    finishFirst?.(success())
    assert.deepEqual((await Promise.all([first, second])).map((response) => response.data.id), ['1', '1'])
    await api.get('/shared-items')
    assert.equal(calls, 2)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('a mutation does not reuse an older in-flight GET', async () => {
  const originalFetch = globalThis.fetch
  let getCalls = 0
  let finishFirst: ((response: Response) => void) | undefined
  const success = () => Response.json({ success: true, message: '', data: {}, meta: null, error: null })
  globalThis.fetch = async (_input, init) => {
    if (init?.method === 'POST') return success()
    getCalls++
    if (getCalls === 1) return new Promise<Response>((resolve) => { finishFirst = resolve })
    return success()
  }
  try {
    const beforeWrite = api.get('/changed-items')
    await api.post('/changed-items', { name: 'new' })
    await api.get('/changed-items')
    assert.equal(getCalls, 2)
    finishFirst?.(success())
    await beforeWrite
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('success false is rejected even when HTTP status is successful', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false,
    message: '',
    data: {},
    meta: null,
    error: { code: 'INVALID_REQUEST', message: 'ข้อมูลไม่ถูกต้อง' },
  }), { status: 200 })
  try {
    await assert.rejects(api.get('/items'), (error: unknown) =>
      error instanceof ApiError && error.message === 'ข้อมูลไม่ถูกต้อง')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('legacy Spring pages are normalized during API response migration', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({
    content: [{ id: 'legacy-1' }],
    totalElements: 21,
    totalPages: 2,
    size: 20,
    number: 0,
  }), { status: 200 })
  try {
    const response = await api.get<Array<{ id: string }>>('/legacy-items')
    assert.deepEqual(response.data, [{ id: 'legacy-1' }])
    assert.deepEqual(response.meta, { page: 1, limit: 20, total: 21, totalPages: 2 })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('legacy direct objects are normalized during API response migration', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(JSON.stringify({ id: 'legacy-user' }), { status: 200 })
  try {
    const response = await api.get<{ id: string }>('/legacy-user')
    assert.deepEqual(response.data, { id: 'legacy-user' })
    assert.equal(response.meta, null)
  } finally {
    globalThis.fetch = originalFetch
  }
})
