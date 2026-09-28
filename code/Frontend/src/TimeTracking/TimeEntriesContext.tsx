import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useScopedWorkspace } from '../shared/useScopedWorkspace'
import { createEmptyWorkspace, listClients, listProjects, listTasks, listTimeEntries } from '../services/workspace'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export const TimeEntriesContext = createContext<WorkspaceContextValue | null>(null)

export function TimeEntriesProvider({ children }: PropsWithChildren) {
  const load = useCallback(async () => {
    const [clients, projects, timeEntries] = await Promise.all([listClients(), listProjects(), listTimeEntries()])
    const tasks = await listTasks(projects)
    return { ...createEmptyWorkspace(), clients, projects, tasks, time_entries: timeEntries }
  }, [])
  const value = useScopedWorkspace(load)
  return <TimeEntriesContext.Provider value={value}>{children}</TimeEntriesContext.Provider>
}
