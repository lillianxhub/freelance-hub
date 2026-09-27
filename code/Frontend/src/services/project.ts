import type { ResourceInput } from '../types/workspace'
import { deleteResource, listProjects as loadProjects, saveResource } from './workspace'

export function listProjects() {
  return loadProjects()
}

export function saveProject(project: ResourceInput<'projects'>) {
  return saveResource('projects', project)
}

export function deleteProject(id: string) {
  return deleteResource('projects', id)
}
