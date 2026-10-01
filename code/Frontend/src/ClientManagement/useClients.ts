import { useContext } from 'react'
import { ClientsContext } from './ClientsContext'
import type { ClientsContextValue } from './ClientsContext'

export function useClients(): ClientsContextValue {
  const context = useContext(ClientsContext)
  if (!context) throw new Error('useClients must be used inside ClientsProvider')
  return context
}
