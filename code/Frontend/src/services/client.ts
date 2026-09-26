import type { ResourceInput } from '../types/workspace'
import { deleteResource, listClients as loadClients, saveResource } from './workspace'

export function listClients() {
  return loadClients()
}

export function saveClient(client: ResourceInput<'clients'>) {
  return saveResource('clients', client)
}

export function deleteClient(id: string) {
  return deleteResource('clients', id)
}
