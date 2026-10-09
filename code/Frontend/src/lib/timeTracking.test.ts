import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateTimeValue } from './timeTracking'

test('calculateTimeValue uses duration and captured hourly rate', () => {
  assert.equal(
    calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: true }),
    1200,
  )
  assert.equal(calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: false }), 0)
})
