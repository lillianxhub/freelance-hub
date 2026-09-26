import { api } from '../api/apiClient'
import type { ResourceInput, WorkspaceData } from '../types/workspace'
import { deleteResource, saveResource } from './workspace'

export function listClients(): Promise<WorkspaceData['clients']> {
  return api.get<WorkspaceData['clients']>('/workspace/clients')
}

export function saveClient(client: ResourceInput<'clients'>) {
  return saveResource('clients', client)
}

export function deleteClient(id: string) {
  return deleteResource('clients', id)
}
