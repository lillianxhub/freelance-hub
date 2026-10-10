import assert from 'node:assert/strict'
import test from 'node:test'
import { act, deferred, renderHook } from '../test-support/react'
import { useAsyncData } from './useAsyncData'

test('async data ignores responses from an old key', async () => {
  const old = deferred<string>()
  const next = deferred<string>()
  const loadOld = () => old.promise
  const loadNext = () => next.promise
  const useOld = () => useAsyncData(loadOld, '', 'old')
  const useNext = () => useAsyncData(loadNext, '', 'next')
  const hook = await renderHook(useOld)
  try {
    assert.equal(hook.current.loading, true)
    await hook.rerender(useNext)
    await act(async () => {
      next.resolve('new-data')
    })
    await act(async () => {
      old.resolve('stale-data')
    })
    assert.equal(hook.current.data, 'new-data')
    assert.equal(hook.current.loading, false)
  } finally {
    await hook.unmount()
  }
})

test('refresh retains loaded data and reports errors, then recovers', async () => {
  let fail: unknown
  const load = async () => {
    if (fail) throw fail
    return 'loaded'
  }
  const useData = () => useAsyncData(load, '', 'key')
  const hook = await renderHook(useData)
  try {
    assert.equal(hook.current.data, 'loaded')
    fail = new Error('Failed to load')
    await act(async () => {
      await hook.current.refresh()
    })
    assert.equal(hook.current.data, 'loaded')
    assert.equal(hook.current.error, 'Failed to load')
    fail = 'Unexpected failure'
    await act(async () => {
      await hook.current.refresh()
    })
    assert.equal(hook.current.error, 'ไม่สามารถโหลดข้อมูลได้')
    fail = undefined
    await act(async () => {
      await hook.current.refresh()
    })
    assert.equal(hook.current.error, '')
  } finally {
    await hook.unmount()
  }
})

test('latest refresh wins and unmount safely discards a pending response', async () => {
  const first = deferred<string>()
  let calls = 0
  const load = () => (++calls === 1 ? first.promise : Promise.resolve('latest'))
  const useData = () => useAsyncData(load, 'initial')
  const hook = await renderHook(useData)
  await act(async () => {
    await hook.current.refresh()
  })
  await act(async () => {
    first.resolve('old')
  })
  assert.equal(hook.current.data, 'latest')
  await hook.unmount()
  const pending = deferred<string>()
  const loadPending = () => pending.promise
  const usePending = () => useAsyncData(loadPending, 'initial')
  const second = await renderHook(usePending)
  await second.unmount()
  await act(async () => {
    pending.resolve('late')
  })
})
