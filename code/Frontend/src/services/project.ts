import { api } from '../api/apiClient'
import type { ApiProject } from '../types/api'
import type { ProjectStatus } from '../types/project'
import type { ResourceInput } from '../types/workspace'
import { deleteResource, listProjects as loadProjects, saveResource, toProject } from './workspace'

export function listProjects() {
  return loadProjects()
}

export function saveProject(project: ResourceInput<'projects'>) {
  return saveResource('projects', project)
}

export function deleteProject(id: string) {
  return deleteResource('projects', id)
}

export async function changeProjectStatus(id: string, status: ProjectStatus) {
  const response = await api.patch<ApiProject>(`/projects/${id}/status`, { status })
  return toProject(response.data)
}
