import { api } from '../api/apiClient'
import type { ApiProject, ApiTask } from '../types/api'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import { toProject, toTask } from './workspace'

const taskCache = new Map<string, Promise<Task[]>>()

export async function listTimerProjects(): Promise<Project[]> {
  const response = await api.get<ApiProject[]>('/projects?sortBy=project_name&direction=ASC')
  return response.data.map(toProject)
}

export async function listTimerTasks(projectId: string): Promise<Task[]> {
  const cached = taskCache.get(projectId)
  if (cached) return cached

  const request = api
    .get<ApiTask[]>(`/projects/${encodeURIComponent(projectId)}/tasks?sort=sortOrder,asc`)
    .then((response) => response.data.map(toTask))
    .catch((error: unknown) => {
      taskCache.delete(projectId)
      throw error
    })
  taskCache.set(projectId, request)
  return request
}
