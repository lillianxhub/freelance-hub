import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateTimeValue, formatDuration, formatTimer } from './formatters.ts'

test('calculateTimeValue uses duration and captured hourly rate', () => {
  assert.equal(calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: true }), 1200)
  assert.equal(calculateTimeValue({ duration_minutes: 90, rate_snapshot: 800, billable: false }), 0)
})

test('formatDuration rounds minutes into readable hours', () => {
  assert.equal(formatDuration(135), '2ชม. 15น.')
  assert.equal(formatDuration(45), '45น.')
})

test('formatTimer pads each time segment', () => {
  assert.equal(formatTimer(3661), '01:01:01')
})
