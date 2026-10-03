import { createContext, useCallback, type PropsWithChildren } from 'react'
import { api } from '../api/apiClient'
import { useAsyncData } from '../shared/useAsyncData'
import type { DashboardActivity, DashboardChartPeriod, DashboardContextValue, DashboardData, DashboardResponse } from '../types/dashboard'

export const DashboardContext = createContext<DashboardContextValue | null>(null)

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

async function loadDashboard(): Promise<DashboardData> {
  const response = await api.get<DashboardResponse>('/dashboard')
  return {
    ...response.data,
    recentTimeEntries: Array.isArray(response.data.recentTimeEntries)
      ? response.data.recentTimeEntries
      : [],
  }
}

async function loadDashboardActivity(period: DashboardChartPeriod): Promise<DashboardActivity> {
  const response = await api.get<DashboardActivity>(`/dashboard/activity?period=${period}`)
  return response.data
}

export function DashboardProvider({ children }: PropsWithChildren) {
  const load = useCallback(loadDashboard, [])
  const state = useAsyncData(load, emptyDashboard)
  const loadActivity = useCallback(loadDashboardActivity, [])
  return <DashboardContext.Provider value={{ ...state, loadActivity }}>{children}</DashboardContext.Provider>
}
