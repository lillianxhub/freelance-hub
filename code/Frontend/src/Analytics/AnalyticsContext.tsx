import { createContext, useCallback, type PropsWithChildren } from 'react'
import { api } from '../api/apiClient'
import { useScopedWorkspace } from '../shared/useScopedWorkspace'
import { createEmptyWorkspace, listClients, listProjects, listTasks, listTimeEntries, toProfile } from '../services/workspace'
import type { ApiUser } from '../types/api'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export const AnalyticsContext = createContext<WorkspaceContextValue | null>(null)

export function AnalyticsProvider({ children }: PropsWithChildren) {
  const load = useCallback(async () => {
    const [user, clients, projects, timeEntries] = await Promise.all([
      api.get<ApiUser>('/users/me').then((response) => response.data),
      listClients(),
      listProjects(),
      listTimeEntries(),
    ])
    const tasks = await listTasks(projects)
    return { ...createEmptyWorkspace(), profiles: [toProfile(user)], clients, projects, tasks, time_entries: timeEntries }
  }, [])
  const value = useScopedWorkspace(load)
  return <AnalyticsContext.Provider value={value}>{children}</AnalyticsContext.Provider>
}
