import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useAsyncData } from '../shared/useAsyncData'
import { getClientById, listClientOptions } from '../services/client'
import { deleteProject as deleteProjectRequest, getProjectById, saveProject as saveProjectRequest } from '../services/project'
import { deleteTask as deleteTaskRequest, listTasks, saveTask as saveTaskRequest } from '../services/task'
import { listTimeEntries } from '../services/timeTracking'
import type { AsyncDataState } from '../types/asyncData'
import type { Client } from '../types/client'
import type { Project, ProjectInput } from '../types/project'
import type { Task, TaskInput } from '../types/task'
import type { TimeEntry } from '../types/timeTracking'

export interface ProjectsData { clients: Client[]; projects: Project[]; tasks: Task[]; time_entries: TimeEntry[] }
export interface ProjectsContextValue extends AsyncDataState<ProjectsData> {
  saveProject: (input: ProjectInput) => Promise<Project>
  deleteProject: (id: string) => Promise<void>
  saveTask: (input: TaskInput) => Promise<Task>
  deleteTask: (projectId: string, taskId: string) => Promise<void>
}
export const ProjectsContext = createContext<ProjectsContextValue | null>(null)
const emptyData: ProjectsData = { clients: [], projects: [], tasks: [], time_entries: [] }

interface ProjectsProviderProps extends PropsWithChildren { projectId?: string }
export function ProjectsProvider({ children, projectId }: ProjectsProviderProps) {
  const load = useCallback(async (): Promise<ProjectsData> => {
    if (!projectId) return { ...emptyData, clients: await listClientOptions() }
    const project = await getProjectById(projectId)
    const [tasks, client] = await Promise.all([
      listTasks(projectId), project.client_id ? getClientById(project.client_id) : Promise.resolve(undefined),
    ])
    return { clients: client ? [client] : [], projects: [project], tasks, time_entries: [] }
  }, [projectId])
  const state = useAsyncData(load, emptyData, projectId ?? 'project-list')
  const value: ProjectsContextValue = { ...state,
    async saveProject(input) { const saved = await saveProjectRequest(input); await state.refresh(); return saved },
    async deleteProject(id) { await deleteProjectRequest(id); await state.refresh() },
    async saveTask(input) { const saved = await saveTaskRequest(input); await state.refresh(); return saved },
    async deleteTask(projectIdValue, taskId) { await deleteTaskRequest(projectIdValue, taskId); await state.refresh() },
  }
  return <ProjectsContext.Provider value={value}>{children}</ProjectsContext.Provider>
}
