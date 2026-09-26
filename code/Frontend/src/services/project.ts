import { api } from '../api/apiClient'
import type { ResourceInput, WorkspaceData } from '../types/workspace'
import { deleteResource, saveResource } from './workspace'

export function listProjects(): Promise<WorkspaceData['projects']> {
  return api.get<WorkspaceData['projects']>('/workspace/projects')
}

export function saveProject(project: ResourceInput<'projects'>) {
  return saveResource('projects', project)
}

export function deleteProject(id: string) {
  return deleteResource('projects', id)
}
