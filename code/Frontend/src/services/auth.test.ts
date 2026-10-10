import assert from 'node:assert/strict'
import test from 'node:test'
import { getCurrentSession, signIn, signOut, signUp, subscribeToAuthChanges } from './auth'
import { getApiToken, setApiToken } from '../api/apiClient'
import { success } from '../test-support/api'
import { deferred } from '../test-support/react'

test('auth broadcasts ignore this tab and synchronize login and logout from another tab', async () => {
  const originalFetch = globalThis.fetch
  const originalChannel = globalThis.BroadcastChannel
  const channels = new Set<FakeChannel>()
  class FakeChannel {
    onmessage: ((event: MessageEvent) => void) | null = null
    constructor() {
      channels.add(this)
    }
    postMessage(data: unknown) {
      for (const channel of channels)
        if (channel !== this) channel.onmessage?.({ data } as MessageEvent)
    }
    close() {
      channels.delete(this)
    }
  }
  Object.defineProperty(globalThis, 'BroadcastChannel', { configurable: true, value: FakeChannel })
  let requests = 0
  globalThis.fetch = async (url) => {
    requests++
    if (String(url).endsWith('/auth/logout')) return new Response(null, { status: 204 })
    if (String(url).endsWith('/auth/refresh')) return success({ token: 'refreshed' })
    if (String(url).endsWith('/users/me'))
      return success({ id: 'other-user', email: 'other@example.com' })
    return success({ token: 'access', user: { id: 'u1', email: 'owner@example.com' } })
  }
  let notifications = 0
  const changed = deferred<unknown>()
  const unsubscribe = subscribeToAuthChanges((session) => {
    notifications++
    changed.resolve(session)
  })
  const otherTab = new FakeChannel()
  try {
    await signIn('owner@example.com', 'password')
    assert.equal(requests, 1)
    assert.equal(notifications, 0)
    otherTab.postMessage({ action: 'login', sender: 'other-tab' })
    assert.deepEqual(await changed.promise, {
      user: {
        id: 'other-user',
        email: 'other@example.com',
        user_metadata: { full_name: 'other@example.com' },
      },
    })
    assert.equal(notifications, 1)
    otherTab.postMessage({ action: 'logout', sender: 'other-tab' })
    assert.equal(getApiToken(), null)
    assert.equal(notifications, 2)
    await signOut()
    assert.equal(notifications, 3)
  } finally {
    unsubscribe()
    otherTab.close()
    globalThis.fetch = originalFetch
    setApiToken(null)
    Object.defineProperty(globalThis, 'BroadcastChannel', {
      configurable: true,
      value: originalChannel,
    })
  }
})

test('login normalizes email and maps the user while registration does not create a session', async () => {
  const originalFetch = globalThis.fetch
  const requests: { url: string; body: Record<string, string> }[] = []
  globalThis.fetch = async (url, options) => {
    requests.push({ url: String(url), body: JSON.parse(String(options?.body)) })
    return success({ token: 'login-token', user: { id: 'u1', email: 'owner@example.com' } })
  }
  try {
    const session = await signIn(' owner@example.com ', 'password')
    assert.equal(session.user.user_metadata?.full_name, 'owner@example.com')
    assert.equal(getApiToken(), 'login-token')
    assert.equal(requests[0].body.email, 'owner@example.com')
    await signUp({
      displayName: ' Owner ',
      firstName: ' First ',
      lastName: ' Last ',
      email: ' owner@example.com ',
      phone: ' 0812345678 ',
      password: 'password',
    })
    assert.equal(requests[1].body.displayName, 'Owner')
    assert.equal(getApiToken(), 'login-token')
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('session lookup returns null after logout even if the user response arrives late', async () => {
  const originalFetch = globalThis.fetch
  const user = deferred<Response>()
  const started = deferred<void>()
  globalThis.fetch = async (url) => {
    if (String(url).endsWith('/auth/refresh')) return success({ token: 'access' })
    started.resolve()
    return user.promise
  }
  try {
    const pending = getCurrentSession()
    await started.promise
    setApiToken(null)
    user.resolve(success({ id: 'u1', email: 'owner@example.com' }))
    assert.equal(await pending, null)
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('logout invalidates listeners immediately and keeps a later login token', async () => {
  const originalFetch = globalThis.fetch
  const logout = deferred<Response>()
  globalThis.fetch = async () => logout.promise
  let session: unknown = 'signed-in'
  const unsubscribe = subscribeToAuthChanges((value) => {
    session = value
  })
  try {
    setApiToken('old-access')
    const pending = signOut()
    assert.equal(getApiToken(), null)
    assert.equal(session, null)
    setApiToken('new-login')
    logout.resolve(new Response(null, { status: 204 }))
    await pending
    assert.equal(getApiToken(), 'new-login')
    unsubscribe()
    session = 'unchanged'
    setApiToken(null)
    assert.equal(session, 'unchanged')
  } finally {
    unsubscribe()
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})
