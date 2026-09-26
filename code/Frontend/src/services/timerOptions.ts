import { api } from '../api/apiClient'
import type { ApiPage, ApiProject, ApiTask } from '../types/api'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import { toProject, toTask } from './workspace'

export async function listTimerProjects(): Promise<Project[]> {
  const response = await api.get<ApiPage<ApiProject>>('/projects?size=100&sort=name,asc')
  return response.content.map(toProject)
}

export async function listTimerTasks(projectId: string): Promise<Task[]> {
  const response = await api.get<ApiPage<ApiTask>>(`/projects/${encodeURIComponent(projectId)}/tasks?size=100&sort=sortOrder,asc`)
  return response.content.map(toTask)
}
