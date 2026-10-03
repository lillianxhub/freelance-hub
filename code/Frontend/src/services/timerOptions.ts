import { api } from '../api/apiClient'
import type { ApiTask } from '../types/api'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import { listProjectsPage } from './project'
import { toTask } from './task'

const taskCache = new Map<string, Promise<Task[]>>()

export async function listTimerProjects(): Promise<Project[]> {
  const firstPage = await listProjectsPage(1, 100, { sortBy: 'project_name', direction: 'ASC' })
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.meta.totalPages - 1) }, (_, index) =>
      listProjectsPage(index + 2, 100, { sortBy: 'project_name', direction: 'ASC' }),
    ),
  )
  return [...firstPage.projects, ...remainingPages.flatMap((page) => page.projects)]
}

export async function listTimerTasks(projectId: string): Promise<Task[]> {
  const cached = taskCache.get(projectId)
  if (cached) return cached

  const request = (async () => {
    const firstPage = await api.get<ApiTask[]>(`/projects/${encodeURIComponent(projectId)}/tasks?page=1&limit=100&sort=sortOrder,asc`)
    const remainingPages = await Promise.all(
      Array.from({ length: Math.max(0, (firstPage.meta?.totalPages ?? 1) - 1) }, (_, index) =>
        api.get<ApiTask[]>(`/projects/${encodeURIComponent(projectId)}/tasks?page=${index + 2}&limit=100&sort=sortOrder,asc`),
      ),
    )
    return [...firstPage.data, ...remainingPages.flatMap((page) => page.data)].map(toTask)
  })().catch((error: unknown) => {
    taskCache.delete(projectId)
    throw error
  })
  taskCache.set(projectId, request)
  return request
}
