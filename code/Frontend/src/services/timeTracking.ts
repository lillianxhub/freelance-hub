import { api } from '../api/apiClient'
import type { ResourceInput, WorkspaceData } from '../types/workspace'
import { deleteResource, saveResource } from './workspace'

export async function listTimeEntries(projectId?: string) {
  const entries = await api.get<WorkspaceData['time_entries']>('/workspace/time_entries')
  return projectId ? entries.filter((entry) => entry.project_id === projectId) : entries
}

export function saveTimeEntry(entry: ResourceInput<'time_entries'>) {
  return saveResource('time_entries', entry)
}

export function deleteTimeEntry(id: string) {
  return deleteResource('time_entries', id)
}
