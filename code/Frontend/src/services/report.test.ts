import assert from 'node:assert/strict'
import test from 'node:test'
import { getReportDistribution, getReportProjects, getReportSummary } from './report'

test('reports use filtered API endpoints and project pagination metadata', async () => {
  const originalFetch = globalThis.fetch
  const paths: string[] = []
  globalThis.fetch = async (input) => {
    const url = new URL(String(input), 'http://localhost')
    paths.push(`${url.pathname}${url.search}`)
    const data = url.pathname.endsWith('/summary')
      ? { generatedAt: '2026-10-05T00:00:00Z', filters: { clients: [], projects: [] }, summary: { totalTrackedSeconds: 3600 } }
      : url.pathname.endsWith('/distribution')
        ? { groupBy: 'PROJECT', items: [{ id: 'project-1', name: 'Website', trackedSeconds: 3600, percent: 100 }] }
        : []
    const meta = url.pathname.endsWith('/projects')
      ? { page: 2, limit: 10, total: 12, totalPages: 2 }
      : null
    return new Response(JSON.stringify({ success: true, message: 'ok', data, meta, error: null }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }

  try {
    const query = { from: '2026-10-01', to: '2026-10-05', clientId: 'client-1' }
    const summary = await getReportSummary(query)
    const distribution = await getReportDistribution(query, 'PROJECT')
    const projects = await getReportProjects(query, 2)

    assert.equal(summary.summary.totalTrackedSeconds, 3600)
    assert.equal(distribution.items[0].name, 'Website')
    assert.equal(projects.page, 2)
    assert.equal(projects.totalPages, 2)
    assert.equal(paths.length, 3)
    assert.ok(paths.every((path) => path.includes('clientId=client-1')))
    assert.ok(paths.some((path) => path.includes('groupBy=PROJECT')))
    assert.ok(paths.some((path) => path.includes('page=2') && path.includes('limit=10')))
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('report overview can load all time without date parameters', async () => {
  const originalFetch = globalThis.fetch
  let requestedPath = ''
  globalThis.fetch = async (input) => {
    requestedPath = String(input)
    return new Response(JSON.stringify({
      success: true,
      message: 'ok',
      data: { generatedAt: '2026-10-05T00:00:00Z', filters: { clients: [], projects: [] }, summary: { totalTrackedSeconds: 5400, trackedTimeTrendPercent: null } },
      meta: null,
      error: null,
    }), { status: 200, headers: { 'Content-Type': 'application/json' } })
  }

  try {
    const result = await getReportSummary({ from: '', to: '' })
    assert.equal(result.summary.totalTrackedSeconds, 5400)
    assert.ok(requestedPath.endsWith('/reports/summary'))
  } finally {
    globalThis.fetch = originalFetch
  }
})
