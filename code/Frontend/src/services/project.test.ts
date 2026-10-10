import assert from 'node:assert/strict'
import test from 'node:test'
import {
  changeProjectStatus,
  deleteProject,
  getProjectById,
  listAllProjects,
  listProjects,
  listProjectsPage,
  saveProject,
  toProject,
} from './project'
import { success } from '../test-support/api'
import type { ApiProject } from '../types/api'

const source: ApiProject = {
  id: 'p1',
  name: 'Project',
  status: 'ACTIVE',
  clientId: 'c1',
  targetMinutes: 90,
}

test('project mapping preserves budgets, nested client and progress data', () => {
  const mapped = toProject({
    ...source,
    clientId: undefined,
    client: { id: 'c1', name: 'Client' },
    taskProgress: { totalTasks: 2, completedTasks: 1, percent: 50 },
    timeTracking: { trackedSeconds: 3600, trackedHours: 1, usagePercent: 50 },
  })
  assert.equal(mapped.client_id, 'c1')
  assert.equal(mapped.budget_hours, 1.5)
  assert.equal(mapped.description, '')
  assert.equal(mapped.task_progress?.percent, 50)
  assert.equal(mapped.time_tracking?.tracked_seconds, 3600)
})

test('project list sends filters and gathers all pages', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url) => {
    const query = new URL(String(url), 'http://localhost').searchParams
    assert.equal(query.get('status'), 'ALL')
    assert.equal(query.get('search'), 'Project')
    const page = Number(query.get('page'))
    return success([{ ...source, id: `p${page}` }], { page, limit: 100, total: 101, totalPages: 2 })
  }
  try {
    assert.deepEqual(
      (await listAllProjects({ status: 'ALL', search: ' Project ' })).map((item) => item.id),
      ['p1', 'p2'],
    )
    globalThis.fetch = async () => success([source])
    assert.equal((await listProjects()).length, 1)
    assert.deepEqual((await listProjectsPage(2, 5)).meta, {
      page: 2,
      limit: 5,
      total: 1,
      totalPages: 1,
    })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('project create, replacement, status and archive use separate API operations', async () => {
  const originalFetch = globalThis.fetch
  const requests: { url: string; method?: string; body: Record<string, unknown> }[] = []
  globalThis.fetch = async (url, options) => {
    requests.push({
      url: String(url),
      method: options?.method,
      body: options?.body ? JSON.parse(String(options.body)) : {},
    })
    return options?.method === 'DELETE' ? new Response(null, { status: 204 }) : success(source)
  }
  try {
    const input = { ...toProject(source), id: undefined }
    await saveProject(input)
    assert.equal(requests[0].method, 'POST')
    assert.equal(requests[0].body.targetMinutes, 90)
    await saveProject({ ...input, id: 'p1' })
    assert.equal(requests[1].method, 'PUT')
    await changeProjectStatus('p / 1', 'ON_HOLD')
    assert.equal(requests[2].url, '/api/projects/p%20%2F%201/status')
    assert.deepEqual(requests[2].body, { status: 'ON_HOLD' })
    await deleteProject('p / 1')
    assert.equal(requests[3].method, 'DELETE')
    await getProjectById('p / 1')
    assert.equal(requests[4].url, '/api/projects/p%20%2F%201')
  } finally {
    globalThis.fetch = originalFetch
  }
})
