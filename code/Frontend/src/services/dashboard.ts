import { api } from '../api/apiClient'
import type { DashboardActivity, DashboardChartPeriod, DashboardData } from '../types/dashboard'

export async function getDashboard(): Promise<DashboardData> {
  const response = await api.get<DashboardData>('/dashboard')
  return response.data
}

export async function getDashboardActivity(period: DashboardChartPeriod): Promise<DashboardActivity> {
  const response = await api.get<DashboardActivity>(`/dashboard/activity?period=${period}`)
  return response.data
}
