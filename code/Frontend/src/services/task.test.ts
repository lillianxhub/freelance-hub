import assert from 'node:assert/strict'
import test from 'node:test'
import type { ApiTask } from '../types/api'
import { changeTaskStatus, deleteTask, toTask } from './task'

test('task mapper translates backend status and field names', () => {
  const source = {
    id: 'task-1', projectId: 'project-1', name: 'Build page', description: undefined,
    status: 'COMPLETED', sortOrder: 2, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z',
  } as ApiTask
  assert.deepEqual(toTask(source), {
    id: 'task-1', owner_id: '', project_id: 'project-1', name: 'Build page', description: '',
    status: 'DONE', sort_order: 2, due_date: '', created_at: source.createdAt, updated_at: source.updatedAt,
  })
})

test('deleteTask addresses the task under the supplied project', async () => {
  const originalFetch = globalThis.fetch
  let requestedUrl = ''
  let requestedMethod = ''
  globalThis.fetch = async (input, init) => {
    requestedUrl = String(input)
    requestedMethod = init?.method ?? ''
    return new Response(null, { status: 204 })
  }
  try {
    await deleteTask('project / 1', 'task / 2')
    assert.equal(requestedUrl, '/api/projects/project%20%2F%201/tasks/task%20%2F%202')
    assert.equal(requestedMethod, 'DELETE')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('changeTaskStatus reopens a completed task through the status endpoint', async () => {
  const originalFetch = globalThis.fetch
  let requestedUrl = ''
  let requestedMethod = ''
  let requestedBody = ''
  globalThis.fetch = async (input, init) => {
    requestedUrl = String(input)
    requestedMethod = init?.method ?? ''
    requestedBody = String(init?.body)
    return Response.json({
      success: true, message: 'Updated', meta: null, error: null,
      data: { id: 'task-1', projectId: 'project-1', name: 'Task', status: 'OPEN', sortOrder: 0 },
    })
  }
  try {
    const task = await changeTaskStatus('task-1', 'TODO')
    assert.equal(requestedUrl, '/api/tasks/task-1/status')
    assert.equal(requestedMethod, 'PATCH')
    assert.deepEqual(JSON.parse(requestedBody), { status: 'OPEN' })
    assert.equal(task.status, 'TODO')
  } finally {
    globalThis.fetch = originalFetch
  }
})
