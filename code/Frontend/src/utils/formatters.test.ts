import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateTimeValue, formatDuration, formatDurationSeconds, formatTimer, splitDurationSeconds } from './formatters'

test('calculateTimeValue uses duration and captured hourly rate', () => {
  assert.equal(calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: true }), 1200)
  assert.equal(calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: false }), 0)
})

test('formatDuration converts minute values to the shared time display format', () => {
  assert.equal(formatDuration(135), '02:15:00 นาที')
  assert.equal(formatDuration(45), '00:45:00 นาที')
})

test('formatTimer pads each time segment', () => {
  assert.equal(formatTimer(3661), '01:01:01')
})

test('duration seconds are split and formatted for display', () => {
  assert.deepEqual(splitDurationSeconds(8118), { hours: 2, minutes: 15, seconds: 18 })
  assert.equal(formatDurationSeconds(8118), '02:15:18 นาที')
  assert.equal(formatDurationSeconds(0), '00:00:00 นาที')
})
