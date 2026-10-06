import assert from 'node:assert/strict'
import test from 'node:test'
import { inDateRange } from './date'

test('inDateRange includes range boundaries', () => {
  assert.equal(inDateRange('2026-09-01T12:00:00Z', '2026-09-01', '2026-09-30'), true)
  assert.equal(inDateRange('2026-10-01T00:00:00Z', '2026-09-01', '2026-09-30'), false)
})
