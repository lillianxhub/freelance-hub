import { api } from '../api/apiClient'
import type { ResourceInput, WorkspaceData } from '../types/workspace'
import { deleteResource, saveResource } from './workspace'

export async function listTasks(projectId?: string) {
  const tasks = await api.get<WorkspaceData['tasks']>('/workspace/tasks')
  return projectId ? tasks.filter((task) => task.project_id === projectId) : tasks
}

export function saveTask(task: ResourceInput<'tasks'>) {
  return saveResource('tasks', task)
}

export function deleteTask(id: string) {
  return deleteResource('tasks', id)
}
