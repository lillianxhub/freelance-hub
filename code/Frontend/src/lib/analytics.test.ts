import assert from 'node:assert/strict'
import test from 'node:test'
import { groupTimeBy, summarizeTime } from './analytics'

const entries = [
  { project_id: 'p1', duration_minutes: 120, billable: true, rate_snapshot: 1000 },
  { project_id: 'p1', duration_minutes: 60, billable: false, rate_snapshot: 1000 },
  { project_id: 'p2', duration_minutes: 30, billable: true, rate_snapshot: 800 },
]

test('summarizeTime calculates tracked, billable and utilization', () => {
  const summary = summarizeTime(entries)
  assert.equal(summary.trackedMinutes, 210)
  assert.equal(summary.billableMinutes, 150)
  assert.equal(summary.utilization, (150 / 210) * 100)
})

test('groupTimeBy aggregates and sorts project time', () => {
  const groups = groupTimeBy(entries, (entry) => entry.project_id)
  assert.deepEqual(
    groups.map((group) => group.key),
    ['p1', 'p2'],
  )
  assert.equal(groups[0].minutes, 180)
  assert.equal(groups[0].value, 2000)
})
