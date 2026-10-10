import assert from 'node:assert/strict'
import test from 'node:test'
import { act, deferred, renderHook } from '../test-support/react'
import { success } from '../test-support/api'
import { TimerProvider } from './TimerContext'
import { useCurrentTimer } from './useCurrentTimer'
import { useTimer } from './useTimer'
import { api } from '../api/apiClient'
import type { TimerWorkspace } from '../types/timerWorkspace'

const running = {
  running: true,
  timeEntry: {
    id: 'timer-1',
    project: { id: 'p-active', name: 'Active' },
    task: null,
    startedAt: new Date().toISOString(),
    description: null,
  },
}

test('late initial timer response cannot overwrite a refresh after a mutation', async () => {
  const originalFetch = globalThis.fetch
  const initial = deferred<Response>()
  let reads = 0
  globalThis.fetch = async (url) =>
    String(url).endsWith('/timer/current')
      ? ++reads === 1
        ? initial.promise
        : success(running)
      : success(null)
  const hook = await renderHook(useCurrentTimer, TimerProvider)
  try {
    await act(async () => {
      await api.post('/timer/start', { projectId: 'p-active' })
      await hook.current.refreshCurrentTimer()
    })
    assert.equal(hook.current.currentTimer?.running, true)
    await act(async () => {
      initial.resolve(success({ running: false, timeEntry: null }))
    })
    assert.equal(hook.current.currentTimer?.running, true)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
  }
})

test('timer hook selects only ACTIVE projects and starts without a task', async () => {
  const originalFetch = globalThis.fetch
  let active = false
  let payload: unknown
  let refreshes = 0
  const workspace: TimerWorkspace = {
    data: {
      projects: [
        { id: 'p-planned', name: 'Planned', status: 'PLANNED', color: '#000' },
        { id: 'p-active', name: 'Active', status: 'ACTIVE', color: '#000' },
        { id: 'p-hold', name: 'Hold', status: 'ON_HOLD', color: '#000' },
      ],
      tasks: [],
      time_entries: [],
    },
    refresh: async () => {
      refreshes++
    },
  }
  globalThis.fetch = async (url, options) => {
    if (String(url).endsWith('/timer/current') && options?.method === 'GET')
      return success(active ? running : { running: false, timeEntry: null })
    if (String(url).endsWith('/timer/start')) {
      payload = JSON.parse(String(options?.body))
      active = true
    } else active = false
    return success({ id: 'timer-1', startedAt: running.timeEntry.startedAt, durationSeconds: 60 })
  }
  const useTestTimer = () => useTimer(workspace, { initialProjectId: 'p-planned' })
  const hook = await renderHook(useTestTimer, TimerProvider)
  try {
    assert.deepEqual(
      hook.current.activeProjects.map((project) => project.id),
      ['p-active'],
    )
    assert.equal(hook.current.timerProjectId, 'p-active')
    await act(async () => {
      hook.current.setDescription(' Work ')
    })
    await act(async () => {
      await hook.current.startTimer()
    })
    assert.deepEqual(payload, { projectId: 'p-active', description: 'Work' })
    assert.equal(hook.current.runningEntry?.id, 'timer-1')
    await act(async () => {
      await hook.current.stopTimer()
    })
    assert.equal(hook.current.runningEntry, null)
    await act(async () => {
      await hook.current.startTimer()
    })
    await act(async () => {
      await hook.current.cancelTimer()
    })
    assert.equal(hook.current.runningEntry, null)
    assert.equal(refreshes, 4)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
  }
})
