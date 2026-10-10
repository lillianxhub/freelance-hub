import assert from 'node:assert/strict'
import test from 'node:test'
import { listTimeEntries, saveTimeEntry } from './timeTracking'
import { success } from '../test-support/api'
import type { TimeEntryInput } from '../types/timeTracking'

const input: TimeEntryInput = {
  project_id: 'project-1',
  task_id: null,
  description: '',
  billable: true,
  currency: 'THB',
  rate_snapshot: 0,
  started_at: '2026-10-10T00:00:00Z',
  ended_at: null,
  duration_minutes: 60,
}

test('duplicating an entry sends only endedAt and preserves its duration', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (_url, options) => {
    const payload = JSON.parse(String(options?.body))
    assert.equal(payload.endedAt, '2026-10-10T01:00:00Z')
    assert.equal('durationSeconds' in payload, false)
    assert.equal(payload.projectId, 'project-1')
    assert.equal(payload.taskId, null)
    return success({
      id: 'copy',
      startedAt: payload.startedAt,
      endedAt: payload.endedAt,
      durationSeconds: 3600,
    })
  }
  try {
    const entry = await saveTimeEntry({ ...input, ended_at: '2026-10-10T01:00:00Z' })
    assert.equal(entry.duration_minutes, 60)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('duration-only entry converts minutes to seconds without an end time', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (_url, options) => {
    const payload = JSON.parse(String(options?.body))
    assert.equal(payload.durationSeconds, 90)
    assert.equal('endedAt' in payload, false)
    return success({ id: 'manual', startedAt: payload.startedAt, durationSeconds: 90 })
  }
  try {
    const entry = await saveTimeEntry({ ...input, duration_minutes: 1.5 })
    assert.equal(entry.duration_minutes, 1.5)
    await assert.rejects(saveTimeEntry({ ...input, project_id: '' }), /โปรเจกต์/)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('project time entries keep the requested project id when list items omit it', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () =>
    Response.json({
      success: true,
      message: 'Time entries retrieved',
      data: [
        {
          id: 'entry-1',
          task: { id: 'task-1', title: 'Task A' },
          startedAt: '2026-10-03T08:00:00Z',
          endedAt: '2026-10-03T09:00:00Z',
          durationSeconds: 3600,
        },
      ],
      meta: { page: 1, limit: 10, total: 1, totalPages: 1 },
      error: null,
    })

  try {
    const entries = await listTimeEntries('project-1')
    assert.equal(entries[0]?.project_id, 'project-1')
  } finally {
    globalThis.fetch = originalFetch
  }
})
