import { api } from '../api/apiClient'
import { DASHBOARD_LIMITS } from './dashboard.constants'
import type { DashboardData, DashboardQuery } from './dashboard.contract'

/**
 * API-ready loader for the future consolidated dashboard endpoint.
 * This function is intentionally not called while the dashboard uses mock data.
 * `api.get` already adds the `/api` base URL, auth token, refresh handling, and
 * unwraps the standard response envelope.
 */
export async function getDashboard(query: DashboardQuery): Promise<DashboardData> {
  const params = new URLSearchParams({
    from: query.from,
    to: query.to,
    recentTimeEntryLimit: String(query.recentTimeEntryLimit ?? DASHBOARD_LIMITS.recentTimeEntries),
    activeProjectLimit: String(query.activeProjectLimit ?? DASHBOARD_LIMITS.activeProjects),
    pendingTaskLimit: String(query.pendingTaskLimit ?? DASHBOARD_LIMITS.pendingTasks),
  })
  const response = await api.get<DashboardData>(`/dashboard?${params.toString()}`)
  return response.data
}
