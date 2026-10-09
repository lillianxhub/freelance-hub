import { useCallback, type PropsWithChildren } from 'react'
import { DashboardContext } from './dashboard-context'
import { useAsyncData } from '../hooks/useAsyncData'
import { getDashboard, getDashboardActivity } from '../services/dashboard'
import type { DashboardChartPeriod, DashboardData } from '../types/dashboard'

const emptyDashboard: DashboardData = {
  generatedAt: '',
  summary: {
    weekTrackedSeconds: 0,
    weekTrendPercent: null,
    activeProjectCount: 0,
    activeProjectTrackedSeconds: 0,
    activeProjectTargetSeconds: 0,
    targetUsagePercent: null,
    completedTaskCount: 0,
    totalTaskCount: 0,
    completedTaskPercent: 0,
  },
  dailyWork: [],
  activeProjects: [],
  openTasks: [],
  recentTimeEntries: [],
}

export function DashboardProvider({ children }: PropsWithChildren) {
  const load = useCallback(() => getDashboard(), [])
  const state = useAsyncData(load, emptyDashboard)
  const loadActivity = useCallback(
    (period: DashboardChartPeriod) => getDashboardActivity(period),
    [],
  )
  return (
    <DashboardContext.Provider value={{ ...state, loadActivity }}>
      {children}
    </DashboardContext.Provider>
  )
}
