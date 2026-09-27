import type { ResourceInput } from '../types/workspace'
import { deleteResource, loadAllResources, saveResource } from './workspace'

export async function listTasks(projectId?: string) {
  const tasks = (await loadAllResources()).tasks
  return projectId ? tasks.filter((task) => task.project_id === projectId) : tasks
}

export function saveTask(task: ResourceInput<'tasks'>) {
  return saveResource('tasks', task)
}

export function deleteTask(id: string) {
  return deleteResource('tasks', id)
}
