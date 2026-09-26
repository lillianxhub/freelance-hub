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

test('api helpers send JSON and attach the stored token', async () => {
  const originalFetch = globalThis.fetch
  setApiToken('test-token')
  globalThis.fetch = async (input, init) => {
    assert.equal(input, '/api/items')
    assert.equal(init?.method, 'POST')
    assert.equal(init?.body, JSON.stringify({ name: 'Acme' }))
    const headers = new Headers(init?.headers)
    assert.equal(headers.get('Content-Type'), 'application/json')
    assert.equal(headers.get('Authorization'), 'Bearer test-token')
    return new Response(JSON.stringify({ id: '1' }), { status: 201 })
  }
  try {
    assert.deepEqual(await api.post<{ id: string }>('/items', { name: 'Acme' }), { id: '1' })
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
    return new Response(JSON.stringify({ uploaded: true }), { status: 200 })
  }
  try {
    assert.deepEqual(await fetchMultipartClient<{ uploaded: boolean }>('/upload', formData, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
    }), { uploaded: true })
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
    assert.equal(await api.delete('/items/1'), undefined)
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
