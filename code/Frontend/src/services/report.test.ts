import assert from 'node:assert/strict'
import test from 'node:test'
import { getReportSummary } from './report'

test('report summary mock returns one consolidated response without a network request', async () => {
  const originalFetch = globalThis.fetch
  let fetchCount = 0
  globalThis.fetch = async () => {
    fetchCount += 1
    throw new Error('mock report must not call fetch')
  }

  try {
    const report = await getReportSummary({
      from: '2026-10-01',
      to: '2026-10-31',
      clientId: 'client-northstar',
    })

    assert.equal(fetchCount, 0)
    assert.equal(report.summary.totalClients, 1)
    assert.ok(report.projectUsage.every((project) => project.clientId === 'client-northstar'))
    assert.ok(report.summary.totalTrackedSeconds > 0)
  } finally {
    globalThis.fetch = originalFetch
  }
})
