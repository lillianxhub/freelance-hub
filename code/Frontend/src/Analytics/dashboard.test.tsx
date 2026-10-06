import assert from 'node:assert/strict'
import test from 'node:test'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { setApiToken } from '../api/apiClient'
import type { DashboardContextValue } from '../types/dashboard'
import DashboardTimerCard from './components/DashboardTimerCard'
import { DashboardProvider } from './DashboardContext'
import { useDashboard } from './useDashboard'

function captureDashboard(): DashboardContextValue {
  let value: DashboardContextValue | undefined
  function Consumer() {
    value = useDashboard()
    return null
  }
  renderToStaticMarkup(createElement(DashboardProvider, null, createElement(Consumer)))
  assert.ok(value)
  return value
}

test('Dashboard provider starts with safe empty data', () => {
  const dashboard = captureDashboard()
  assert.equal(dashboard.loading, true)
  assert.deepEqual(dashboard.data.dailyWork, [])
  assert.deepEqual(dashboard.data.recentTimeEntries, [])
  assert.equal(dashboard.data.summary.weekTrackedSeconds, 0)
})

test('Dashboard activity requests the selected period and returns its data', async () => {
  const originalFetch = globalThis.fetch
  const paths: string[] = []
  setApiToken(null)
  globalThis.fetch = async (input) => {
    paths.push(String(input))
    const period = new URL(String(input), 'http://localhost').searchParams.get('period')
    assert.ok(period === 'MONTH' || period === 'YEAR')
    return Response.json({
      success: true,
      message: '',
      data: { period, points: [{ date: '2026-10-01', trackedSeconds: 90 }] },
      meta: null,
      error: null,
    })
  }

  try {
    const dashboard = captureDashboard()
    assert.deepEqual(await dashboard.loadActivity('MONTH'), {
      period: 'MONTH', points: [{ date: '2026-10-01', trackedSeconds: 90 }],
    })
    assert.equal((await dashboard.loadActivity('YEAR')).period, 'YEAR')
    assert.deepEqual(paths, ['/api/dashboard/activity?period=MONTH', '/api/dashboard/activity?period=YEAR'])
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('Dashboard activity propagates API errors', async () => {
  const originalFetch = globalThis.fetch
  setApiToken(null)
  globalThis.fetch = async () => Response.json({ message: 'โหลดกราฟไม่สำเร็จ' }, { status: 500 })
  try {
    await assert.rejects(captureDashboard().loadActivity('MONTH'), /โหลดกราฟไม่สำเร็จ/)
  } finally {
    globalThis.fetch = originalFetch
    setApiToken(null)
  }
})

test('Timer card shows an empty state when there is no running timer or recent entry', () => {
  const html = renderToStaticMarkup(createElement(DashboardTimerCard, {
    currentTimer: null,
    recentEntries: undefined,
    onStop: async () => {},
  }))
  assert.match(html, /ไม่ได้จับเวลา/)
  assert.match(html, /ยังไม่มีรายการเวลา/)
})

test('Timer card shows the latest entry and accepts a running timer without a task', () => {
  const recentEntries = [{
    id: 'entry-1', projectName: 'โปรเจกต์ล่าสุด', taskName: null,
    description: 'บันทึกงาน', startedAt: '2026-10-01T10:00:00Z', durationSeconds: 90,
  }]
  const stoppedHtml = renderToStaticMarkup(createElement(DashboardTimerCard, {
    currentTimer: null,
    recentEntries,
    onStop: async () => {},
  }))
  assert.match(stoppedHtml, /โปรเจกต์ล่าสุด/)
  assert.match(stoppedHtml, /ไม่ระบุงาน/)

  const runningHtml = renderToStaticMarkup(createElement(DashboardTimerCard, {
    currentTimer: {
      running: true,
      timeEntry: {
        id: 'running-1', project: { id: 'project-1', name: 'โปรเจกต์ปัจจุบัน' },
        task: null, startedAt: new Date().toISOString(), description: null,
      },
    },
    recentEntries,
    onStop: async () => {},
  }))
  assert.match(runningHtml, /กำลังทำงาน/)
  assert.match(runningHtml, /โปรเจกต์ปัจจุบัน/)
  assert.match(runningHtml, /หยุดจับเวลา/)
})
