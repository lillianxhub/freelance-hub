import assert from 'node:assert/strict'
import test from 'node:test'
import { act, renderHook } from '../../../test-support/react'
import { success } from '../../../test-support/api'
import { useProjectTimeEntries } from './useProjectTimeEntries'

test('project time entries recover from a failed page and clear the old error', async () => {
  const originalFetch = globalThis.fetch
  let fail = true
  globalThis.fetch = async (url) => {
    if (fail) return new Response(null, { status: 500 })
    return String(url).includes('/summary')
      ? success({ entryCount: 1, totalSeconds: 3600 })
      : success(
          [{ id: 'e1', projectId: 'p1', startedAt: '2026-10-10T00:00:00Z', durationSeconds: 3600 }],
          { page: 2, limit: 5, total: 6, totalPages: 2 },
        )
  }
  const useEntries = () => useProjectTimeEntries('p1')
  const hook = await renderHook(useEntries)
  try {
    assert.ok(hook.current.error)
    fail = false
    await act(async () => {
      hook.current.setPage(2)
    })
    assert.equal(hook.current.error, '')
    assert.equal(hook.current.entries[0]?.id, 'e1')
    assert.equal(hook.current.trackedSeconds, 3600)
    assert.equal(hook.current.totalPages, 2)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
  }
})
