import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useScopedWorkspace } from '../shared/useScopedWorkspace'
import { createEmptyWorkspace, getClientById, getProjectById, listClientOptions, listTasks } from '../services/workspace'
import { listTimeEntries } from '../services/timeTracking'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export const ProjectsContext = createContext<WorkspaceContextValue | null>(null)

interface ProjectsProviderProps extends PropsWithChildren {
  projectId?: string
}

export function ProjectsProvider({ children, projectId }: ProjectsProviderProps) {
  const load = useCallback(async () => {
    if (!projectId) {
      const clients = await listClientOptions()
      return { ...createEmptyWorkspace(), clients }
    }

    const project = await getProjectById(projectId)
    const [tasks, timeEntries, client] = await Promise.all([
      listTasks([project]),
      listTimeEntries(projectId),
      project.client_id ? getClientById(project.client_id) : Promise.resolve(undefined),
    ])
    return { ...createEmptyWorkspace(), clients: client ? [client] : [], projects: [project], tasks, time_entries: timeEntries }
  }, [projectId])
  const value = useScopedWorkspace(load)
  return <ProjectsContext.Provider value={value}>{children}</ProjectsContext.Provider>
}
