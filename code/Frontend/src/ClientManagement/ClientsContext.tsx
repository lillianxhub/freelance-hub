import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useAsyncData } from '../shared/useAsyncData'
import { listClients, saveClient as saveClientRequest, updateClientStatus } from '../services/client'
import { listProjects } from '../services/project'
import { listTimeEntries } from '../services/timeTracking'
import type { AsyncDataState } from '../types/asyncData'
import type { Client, ClientInput } from '../types/client'
import type { Project } from '../types/project'
import type { TimeEntry } from '../types/timeTracking'

export interface ClientsData { clients: Client[]; projects: Project[]; time_entries: TimeEntry[] }
export interface ClientsContextValue extends AsyncDataState<ClientsData> {
  saveClient: (input: ClientInput) => Promise<Client>
  archiveClient: (id: string) => Promise<void>
}
export const ClientsContext = createContext<ClientsContextValue | null>(null)
const emptyData: ClientsData = { clients: [], projects: [], time_entries: [] }

export function ClientsProvider({ children }: PropsWithChildren) {
  const load = useCallback(async (): Promise<ClientsData> => {
    const [clients, projects, time_entries] = await Promise.all([listClients(), listProjects(), listTimeEntries()])
    return { clients, projects, time_entries }
  }, [])
  const state = useAsyncData(load, emptyData)
  const value: ClientsContextValue = { ...state,
    async saveClient(input) { const saved = await saveClientRequest(input); await state.refresh(); return saved },
    async archiveClient(id) { await updateClientStatus(id, false); await state.refresh() },
  }
  return <ClientsContext.Provider value={value}>{children}</ClientsContext.Provider>
}
