import { api } from '../api/apiClient'
import type { ApiTask } from '../types/api'
import type { Task, TaskInput } from '../types/task'

function emptyString(value: string | null | undefined): string { return value ?? '' }

export function toTask(source: ApiTask): Task {
  return { id: source.id, owner_id: '', project_id: source.projectId, name: source.name,
    description: emptyString(source.description), status: source.status,
    sort_order: source.sortOrder, due_date: '', created_at: source.createdAt, updated_at: source.updatedAt }
}

export async function listTasks(projectId: string): Promise<Task[]> {
  const response = await api.get<ApiTask[]>(`/projects/${encodeURIComponent(projectId)}/tasks?page=1&limit=10&sort=sortOrder,asc`)
  return response.data.map(toTask)
}

export async function saveTask(input: TaskInput): Promise<Task> {
  if (!input.project_id) throw new Error('Task ต้องระบุ project_id')
  if (!input.id) return toTask((await api.post<ApiTask>(`/projects/${input.project_id}/tasks`, {
    name: input.name, description: input.description || undefined, sortOrder: input.sort_order ?? 0,
  })).data)
  const path = `/projects/${input.project_id}/tasks/${input.id}`
  const task = (await api.patch<ApiTask>(path, { name: input.name, description: input.description || undefined })).data
  if (input.status === 'COMPLETED') return toTask((await api.patch<ApiTask>(`${path}/complete`)).data)
  return toTask((await api.patch<ApiTask>(`${path}/reorder`, { sortOrder: input.sort_order ?? task.sortOrder })).data)
}

export async function changeTaskStatus(id: string, status: Task['status']): Promise<Task> {
  const response = await api.patch<ApiTask>(`/tasks/${encodeURIComponent(id)}/status`, {
    status,
  })
  return toTask(response.data)
}

export async function deleteTask(projectId: string, taskId: string): Promise<void> {
  await api.delete(`/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}`)
}
