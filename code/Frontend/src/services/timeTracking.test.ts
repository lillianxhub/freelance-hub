import assert from 'node:assert/strict'
import test from 'node:test'
import { listTimeEntries } from './timeTracking'

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
