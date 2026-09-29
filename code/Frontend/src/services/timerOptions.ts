import { api } from '../api/apiClient'
import type { ApiProject, ApiTask } from '../types/api'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import { toProject, toTask } from './workspace'

export async function listTimerProjects(): Promise<Project[]> {
  const response = await api.get<ApiProject[]>('/projects?size=100&sort=name,asc')
  return response.data.map(toProject)
}

export async function listTimerTasks(projectId: string): Promise<Task[]> {
  const response = await api.get<ApiTask[]>(`/projects/${encodeURIComponent(projectId)}/tasks?size=100&sort=sortOrder,asc`)
  return response.data.map(toTask)
}
