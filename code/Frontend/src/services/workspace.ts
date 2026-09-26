import { api } from '../api/apiClient'
import type { ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from '../types/workspace'

export function createEmptyWorkspace(): WorkspaceData {
  return { profiles: [], clients: [], projects: [], tasks: [], time_entries: [], finance_entries: [], invoices: [], invoice_items: [], payments: [] }
}

export async function loadAllResources(): Promise<WorkspaceData> {
  const [profiles, clients, projects, tasks, time_entries, finance_entries, invoices, invoice_items, payments] = await Promise.all([
    api.get<WorkspaceData['profiles']>('/workspace/profiles'),
    api.get<WorkspaceData['clients']>('/workspace/clients'),
    api.get<WorkspaceData['projects']>('/workspace/projects'),
    api.get<WorkspaceData['tasks']>('/workspace/tasks'),
    api.get<WorkspaceData['time_entries']>('/workspace/time_entries'),
    api.get<WorkspaceData['finance_entries']>('/workspace/finance_entries'),
    api.get<WorkspaceData['invoices']>('/workspace/invoices'),
    api.get<WorkspaceData['invoice_items']>('/workspace/invoice_items'),
    api.get<WorkspaceData['payments']>('/workspace/payments'),
  ])

  return { profiles, clients, projects, tasks, time_entries, finance_entries, invoices, invoice_items, payments }
}

export function saveResource<K extends ResourceName>(resource: K, input: ResourceInput<K>): Promise<ResourceRecord<K>> {
  const id = input.id || crypto.randomUUID()
  return api.put<ResourceRecord<K>>(`/workspace/${resource}/${id}`, input)
}

export async function deleteResource(resource: ResourceName, id: string): Promise<void> {
  await api.delete(`/workspace/${resource}/${id}`)
}
