import assert from 'node:assert/strict'
import test from 'node:test'
import { activityChartPoints, formatChartDuration, weeklyChartPoints } from './dashboard'

test('dashboard chart keeps exact seconds, including durations shorter than one hour', () => {
  assert.equal(weeklyChartPoints([{ date: '2026-10-01', trackedSeconds: 5400 }])[0].totalSeconds, 5400)
  assert.equal(weeklyChartPoints([{ date: '2026-10-01', trackedSeconds: 15 }])[0].totalSeconds, 15)
  assert.equal(formatChartDuration(15), '15 วิ')
  assert.equal(formatChartDuration(1800), '30 นาที')
  assert.deepEqual(activityChartPoints('MONTH', [
    { date: '2026-10-01', trackedSeconds: 3600 },
    { date: '2026-10-02', trackedSeconds: 0 },
  ]).map((point) => ({ day: point.day, totalSeconds: point.totalSeconds })), [
    { day: '1', totalSeconds: 3600 },
    { day: '2', totalSeconds: 0 },
  ])
})

test('yearly chart labels each month from the API', () => {
  const points = activityChartPoints('YEAR', [
    { date: '2026-01-01', trackedSeconds: 7200 },
    { date: '2026-02-01', trackedSeconds: 0 },
  ])
  assert.equal(points.length, 2)
  assert.equal(points[0].key, '2026-01-01')
  assert.equal(points[0].totalSeconds, 7200)
  assert.equal(points[1].totalSeconds, 0)
})
