import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useScopedWorkspace } from '../shared/useScopedWorkspace'
import { createEmptyWorkspace, listClientOptions, listTimeEntries } from '../services/workspace'
import { listTimerProjects } from '../services/timerOptions'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export const TimeEntriesContext = createContext<WorkspaceContextValue | null>(null)

export function TimeEntriesProvider({ children }: PropsWithChildren) {
  const load = useCallback(async () => {
    const [clients, projects, timeEntries] = await Promise.all([listClientOptions(), listTimerProjects(), listTimeEntries()])
    return { ...createEmptyWorkspace(), clients, projects, time_entries: timeEntries }
  }, [])
  const value = useScopedWorkspace(load)
  return <TimeEntriesContext.Provider value={value}>{children}</TimeEntriesContext.Provider>
}
