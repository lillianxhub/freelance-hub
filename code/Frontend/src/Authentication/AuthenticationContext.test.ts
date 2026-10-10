import assert from 'node:assert/strict'
import test from 'node:test'
import { act, deferred, renderHook } from '../test-support/react'
import { AuthProvider } from './AuthenticationContext'
import { useAuth } from './useAuthentication'
import { api, getApiToken, setApiToken } from '../api/apiClient'
import { success } from '../test-support/api'

test('late initial session lookup cannot overwrite a completed login', async () => {
  const originalFetch = globalThis.fetch
  const originalChannel = globalThis.BroadcastChannel
  Object.defineProperty(globalThis, 'BroadcastChannel', { configurable: true, value: undefined })
  const initial = deferred<Response>()
  globalThis.fetch = async (url) =>
    String(url).endsWith('/auth/refresh')
      ? initial.promise
      : success({ token: 'new-access', user: { id: 'new-user', email: 'new@example.com' } })
  const hook = await renderHook(useAuth, AuthProvider)
  try {
    await act(async () => {
      await hook.current.login('new@example.com', 'password')
    })
    initial.resolve(success({ token: 'old-access' }))
    await act(async () => {
      await initial.promise
    })
    assert.equal(hook.current.user?.id, 'new-user')
    assert.equal(getApiToken(), 'new-access')
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
    setApiToken(null)
    Object.defineProperty(globalThis, 'BroadcastChannel', {
      configurable: true,
      value: originalChannel,
    })
  }
})

test('expired refresh clears the authenticated React session', async () => {
  const originalFetch = globalThis.fetch
  let expired = false
  globalThis.fetch = async (url) => {
    if (expired) return new Response(null, { status: 401 })
    return success(
      String(url).endsWith('/auth/refresh')
        ? { token: 'access' }
        : { id: 'user-1', email: 'owner@example.com', displayName: 'Owner' },
    )
  }
  const hook = await renderHook(useAuth, AuthProvider)
  try {
    assert.equal(hook.current.user?.id, 'user-1')
    assert.equal(hook.current.loading, false)
    expired = true
    await act(async () => {
      await assert.rejects(api.get('/private-expired'))
    })
    assert.equal(hook.current.user, null)
    assert.equal(getApiToken(), null)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('failed logout still clears the local session without BroadcastChannel', async () => {
  const originalFetch = globalThis.fetch
  const originalChannel = globalThis.BroadcastChannel
  Object.defineProperty(globalThis, 'BroadcastChannel', { configurable: true, value: undefined })
  globalThis.fetch = async (url) =>
    String(url).endsWith('/auth/logout')
      ? new Response(null, { status: 500 })
      : success(
          String(url).endsWith('/auth/refresh')
            ? { token: 'access' }
            : { id: 'user-1', email: 'owner@example.com' },
        )
  const hook = await renderHook(useAuth, AuthProvider)
  try {
    assert.equal(hook.current.user?.id, 'user-1')
    await act(async () => {
      await assert.rejects(hook.current.logout())
    })
    assert.equal(hook.current.user, null)
    assert.equal(getApiToken(), null)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
    setApiToken(null)
    Object.defineProperty(globalThis, 'BroadcastChannel', {
      configurable: true,
      value: originalChannel,
    })
  }
})
