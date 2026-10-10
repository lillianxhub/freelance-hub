import assert from 'node:assert/strict'
import test from 'node:test'
import {
  changePassword,
  getStoredProfileImage,
  loadProfile,
  removeStoredProfileImage,
  saveStoredProfileImage,
  toProfile,
  updateProfile,
} from './profile'
import { success } from '../test-support/api'
import '../test-support/react'

test('profile mapping normalizes Thai phone numbers and optional fields', () => {
  const profile = toProfile({
    id: 'u1',
    email: 'owner@example.com',
    firstName: 'First',
    lastName: 'Last',
    phone: '+66 81-234-5678',
  })
  assert.equal(profile.phone, '0812345678')
  assert.equal(profile.full_name, 'First Last')
  assert.equal(profile.tax_id, '')
  assert.equal(toProfile({ id: 'u1', email: 'owner@example.com' }).full_name, 'owner@example.com')
})

test('profile images are isolated by user and can be removed', () => {
  saveStoredProfileImage('u1', 'image-one')
  saveStoredProfileImage('u2', 'image-two')
  assert.equal(getStoredProfileImage('u1'), 'image-one')
  assert.equal(getStoredProfileImage('u2'), 'image-two')
  assert.equal(getStoredProfileImage(undefined), '')
  removeStoredProfileImage('u1')
  assert.equal(getStoredProfileImage('u1'), '')
  removeStoredProfileImage('u2')
})

test('profile loading, update and password change preserve the API contract', async () => {
  const originalFetch = globalThis.fetch
  const calls: { url: string; method?: string; body: Record<string, unknown> }[] = []
  globalThis.fetch = async (url, options) => {
    calls.push({
      url: String(url),
      method: options?.method,
      body: options?.body ? JSON.parse(String(options.body)) : {},
    })
    return success({ id: 'u1', email: 'owner@example.com', displayName: 'Owner' })
  }
  try {
    const profile = await loadProfile()
    assert.equal(profile.display_name, 'Owner')
    await updateProfile({
      ...profile,
      sub_district: 'Sub',
      postal_code: '40000',
      tax_id: '1234567890123',
    })
    assert.equal(calls[1].method, 'PATCH')
    assert.equal(calls[1].body.subdistrict, 'Sub')
    assert.equal(calls[1].body.postalCode, '40000')
    await changePassword('old-password', 'new-password')
    assert.equal(calls[2].url, '/api/users/me/password')
    assert.deepEqual(calls[2].body, { oldPassword: 'old-password', newPassword: 'new-password' })
  } finally {
    globalThis.fetch = originalFetch
  }
})
