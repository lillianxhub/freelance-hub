import { useCallback, type PropsWithChildren } from 'react'
import { ClientsContext } from './clients-context'
import { useAsyncData } from '../hooks/useAsyncData'
import { listClients, saveClient as saveClientRequest, updateClientStatus } from '../services/client'
import { listAllProjects } from '../services/project'
import type { AsyncDataState } from '../types/asyncData'
import type { Client, ClientInput } from '../types/client'
import type { Project } from '../types/project'
import type { TimeEntry } from '../types/timeTracking'

export interface ClientsData { clients: Client[]; projects: Project[]; time_entries: TimeEntry[] }
export interface ClientsContextValue extends AsyncDataState<ClientsData> {
  saveClient: (input: ClientInput) => Promise<Client>
  archiveClient: (id: string) => Promise<void>
}
const emptyData: ClientsData = { clients: [], projects: [], time_entries: [] }

export function ClientsProvider({ children }: PropsWithChildren) {
  const load = useCallback(async (): Promise<ClientsData> => {
    const [clients, projects] = await Promise.all([listClients(), listAllProjects({ status: 'ALL' })])
    return { clients, projects, time_entries: [] }
  }, [])
  const state = useAsyncData(load, emptyData)
  const value: ClientsContextValue = { ...state,
    async saveClient(input) { const saved = await saveClientRequest(input); await state.refresh(); return saved },
    async archiveClient(id) { await updateClientStatus(id, false); await state.refresh() },
  }
  return <ClientsContext.Provider value={value}>{children}</ClientsContext.Provider>
}
