import assert from 'node:assert/strict'
import test from 'node:test'
import { listTimerProjects, listTimerTasks } from './timerOptions'
import { success } from '../test-support/api'

test('timer options include projects and tasks from all pages in order', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url) => {
    const parsed = new URL(String(url), 'http://localhost')
    const page = Number(parsed.searchParams.get('page'))
    const tasks = parsed.pathname.includes('/tasks')
    assert.equal(parsed.searchParams.get('limit'), '100')
    if (tasks) {
      assert.equal(parsed.searchParams.get('sort'), 'sortOrder,asc')
      assert.match(parsed.pathname, /p%20%2F%201/)
    }
    return success(
      tasks
        ? [{ id: `t${page}`, projectId: 'p / 1', name: 'Task', status: 'OPEN', sortOrder: page }]
        : [{ id: `p${page}`, name: 'Project', status: 'ACTIVE' }],
      { page, limit: 100, total: 101, totalPages: 2 },
    )
  }
  try {
    assert.deepEqual(
      (await listTimerProjects()).map((item) => item.id),
      ['p1', 'p2'],
    )
    assert.deepEqual(
      (await listTimerTasks('p / 1')).map((item) => item.id),
      ['t1', 't2'],
    )
  } finally {
    globalThis.fetch = originalFetch
  }
})
