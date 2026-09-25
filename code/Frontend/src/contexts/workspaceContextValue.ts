import { createContext, useContext } from 'react'
import type { ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from '../types/domain'

export interface WorkspaceContextValue {
  data: WorkspaceData
  loading: boolean
  error: string
  isDemoMode: boolean
  refresh: () => Promise<void>
  save: <K extends ResourceName>(resource: K, record: ResourceInput<K>) => Promise<ResourceRecord<K>>
  remove: (resource: ResourceName, id: string) => Promise<void>
  resetDemo: () => Promise<void>
}

export const WorkspaceContext = createContext<WorkspaceContextValue | null>(null)

export function useWorkspace(): WorkspaceContextValue {
  const context = useContext(WorkspaceContext)
  if (!context) throw new Error('useWorkspace must be used inside WorkspaceProvider')
  return context
}
