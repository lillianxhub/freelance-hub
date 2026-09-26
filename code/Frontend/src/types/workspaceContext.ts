import type { ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from './workspace'

export interface WorkspaceContextValue {
  data: WorkspaceData
  loading: boolean
  error: string
  refresh: () => Promise<void>
  save: <K extends ResourceName>(resource: K, record: ResourceInput<K>) => Promise<ResourceRecord<K>>
  remove: (resource: ResourceName, id: string) => Promise<void>
}
