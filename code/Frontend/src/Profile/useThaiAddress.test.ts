import assert from 'node:assert/strict'
import test from 'node:test'
import { renderHook } from '../test-support/react'
import { useThaiAddress } from './useThaiAddress'

test('Thai address hook reports failure and a later mount can retry successfully', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response(null, { status: 500 })
  const failed = await renderHook(useThaiAddress)
  try {
    assert.equal(failed.current.loading, false)
    assert.ok(failed.current.error)
    assert.deepEqual(failed.current.provinces, [])
  } finally {
    await failed.unmount()
  }
  globalThis.fetch = async () => Response.json([{ id: 1, name_th: 'จังหวัด', districts: [] }])
  const loaded = await renderHook(useThaiAddress)
  try {
    assert.equal(loaded.current.loading, false)
    assert.equal(loaded.current.error, '')
    assert.equal(loaded.current.provinces[0]?.name_th, 'จังหวัด')
  } finally {
    await loaded.unmount()
    globalThis.fetch = originalFetch
  }
})
