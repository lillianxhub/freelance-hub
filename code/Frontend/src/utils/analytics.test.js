import assert from 'node:assert/strict'
import test from 'node:test'
import { groupTimeBy, inDateRange, summarizeTime, toCsv } from './analytics.js'

const entries = [
  { project_id: 'p1', duration_minutes: 120, billable: true, rate_snapshot: 1000, invoice_id: null },
  { project_id: 'p1', duration_minutes: 60, billable: false, rate_snapshot: 1000, invoice_id: null },
  { project_id: 'p2', duration_minutes: 30, billable: true, rate_snapshot: 800, invoice_id: 'inv-1' },
]

test('summarizeTime calculates tracked, billable, utilization and unbilled value', () => {
  const summary = summarizeTime(entries)
  assert.equal(summary.trackedMinutes, 210)
  assert.equal(summary.billableMinutes, 150)
  assert.equal(summary.utilization, (150 / 210) * 100)
  assert.equal(summary.unbilledValue, 2000)
})

test('groupTimeBy aggregates and sorts project time', () => {
  const groups = groupTimeBy(entries, (entry) => entry.project_id)
  assert.deepEqual(groups.map((group) => group.key), ['p1', 'p2'])
  assert.equal(groups[0].minutes, 180)
  assert.equal(groups[0].value, 2000)
})

test('inDateRange includes range boundaries', () => {
  assert.equal(inDateRange('2026-09-01T12:00:00Z', '2026-09-01', '2026-09-30'), true)
  assert.equal(inDateRange('2026-10-01T00:00:00Z', '2026-09-01', '2026-09-30'), false)
})

test('toCsv escapes quotes and commas safely', () => {
  assert.equal(toCsv([['Name', 'Notes'], ['Maya', 'Logo, "final"']]), '"Name","Notes"\n"Maya","Logo, ""final"""')
})
