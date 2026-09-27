import { useContext } from 'react'
import { ClientsContext } from './ClientsContext'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export function useClients(): WorkspaceContextValue {
  const context = useContext(ClientsContext)
  if (!context) throw new Error('useClients must be used inside ClientsProvider')
  return context
}
