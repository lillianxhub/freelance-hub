import assert from 'node:assert/strict'
import test from 'node:test'
import type { ApiTask } from '../types/api'
import { changeTaskStatus, deleteTask, listTasks, reorderTask, saveTask, toTask } from './task'
import { success } from '../test-support/api'

test('task listing loads every page and saving handles creation, completion and reorder', async () => {
  const originalFetch = globalThis.fetch
  const calls: { url: string; method?: string; body: Record<string, unknown> }[] = []
  const task = { id: 't1', projectId: 'p1', name: 'Task', status: 'OPEN' as const, sortOrder: 0 }
  globalThis.fetch = async (url, options) => {
    calls.push({
      url: String(url),
      method: options?.method,
      body: options?.body ? JSON.parse(String(options.body)) : {},
    })
    if (!options?.method || options.method === 'GET') {
      const page = Number(new URL(String(url), 'http://localhost').searchParams.get('page'))
      return success([{ ...task, id: `t${page}` }], { page, limit: 100, total: 101, totalPages: 2 })
    }
    return success(task)
  }
  try {
    assert.deepEqual(
      (await listTasks('p1')).map((item) => item.id),
      ['t1', 't2'],
    )
    const input = { ...toTask(task), id: undefined }
    await assert.rejects(saveTask({ ...input, project_id: '' }), /project_id/)
    await saveTask(input)
    assert.equal(calls[2].method, 'POST')
    await saveTask({ ...input, id: 't1', status: 'COMPLETED' })
    assert.equal(calls[4].url, '/api/projects/p1/tasks/t1/complete')
    await saveTask({ ...input, id: 't1', sort_order: 2 })
    assert.deepEqual(calls[6].body, { sortOrder: 2 })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('task mapper translates backend status and field names', () => {
  const source = {
    id: 'task-1',
    projectId: 'project-1',
    name: 'Build page',
    description: undefined,
    status: 'COMPLETED',
    sortOrder: 2,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  } as ApiTask
  assert.deepEqual(toTask(source), {
    id: 'task-1',
    owner_id: '',
    project_id: 'project-1',
    name: 'Build page',
    description: '',
    status: 'COMPLETED',
    sort_order: 2,
    due_date: '',
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  })
})

test('task mapper preserves every backend task status', () => {
  for (const status of ['OPEN', 'IN_PROGRESS', 'COMPLETED'] as const) {
    const source = {
      id: 'task-1',
      projectId: 'project-1',
      name: 'Task',
      status,
      sortOrder: 0,
    } as ApiTask
    assert.equal(toTask(source).status, status)
  }
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

test('changeTaskStatus reopens a completed task as in progress through the status endpoint', async () => {
  const originalFetch = globalThis.fetch
  let requestedUrl = ''
  let requestedMethod = ''
  let requestedBody = ''
  globalThis.fetch = async (input, init) => {
    requestedUrl = String(input)
    requestedMethod = init?.method ?? ''
    requestedBody = String(init?.body)
    return Response.json({
      success: true,
      message: 'Updated',
      meta: null,
      error: null,
      data: {
        id: 'task-1',
        projectId: 'project-1',
        name: 'Task',
        status: 'IN_PROGRESS',
        sortOrder: 0,
      },
    })
  }
  try {
    const task = await changeTaskStatus('task-1', 'IN_PROGRESS')
    assert.equal(requestedUrl, '/api/tasks/task-1/status')
    assert.equal(requestedMethod, 'PATCH')
    assert.deepEqual(JSON.parse(requestedBody), { status: 'IN_PROGRESS' })
    assert.equal(task.status, 'IN_PROGRESS')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('reorderTask sends one request with the destination position', async () => {
  const originalFetch = globalThis.fetch
  let calls = 0
  globalThis.fetch = async (input, init) => {
    calls++
    assert.equal(input, '/api/projects/project-1/tasks/task-1/reorder')
    assert.equal(init?.method, 'PATCH')
    assert.deepEqual(JSON.parse(String(init?.body)), { sortOrder: 3 })
    return Response.json({
      success: true,
      message: '',
      meta: null,
      error: null,
      data: { id: 'task-1', projectId: 'project-1', name: 'Task', status: 'OPEN', sortOrder: 3 },
    })
  }
  try {
    const task = await reorderTask('project-1', 'task-1', 3)
    assert.equal(task.sort_order, 3)
    assert.equal(calls, 1)
  } finally {
    globalThis.fetch = originalFetch
  }
})
