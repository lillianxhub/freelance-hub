import { api } from '../api/apiClient'
import type {
  DashboardActivity,
  DashboardChartPeriod,
  DashboardData,
  DashboardResponse,
} from '../types/dashboard'

export async function getDashboard(): Promise<DashboardData> {
  const response = await api.get<DashboardResponse>('/dashboard')
  return {
    ...response.data,
    recentTimeEntries: Array.isArray(response.data.recentTimeEntries)
      ? response.data.recentTimeEntries
      : [],
  }
}

export async function getDashboardActivity(
  period: DashboardChartPeriod,
): Promise<DashboardActivity> {
  const response = await api.get<DashboardActivity>(`/dashboard/activity?period=${period}`)
  return response.data
}
