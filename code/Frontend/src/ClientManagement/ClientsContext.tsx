import { useCallback, type PropsWithChildren } from 'react'
import { ClientsContext } from './clients-context'
import { useAsyncData } from '../hooks/useAsyncData'
import { saveClient as saveClientRequest, updateClientStatus } from '../services/client'
import { listAllProjects } from '../services/project'
import type { AsyncDataState } from '../types/asyncData'
import type { Client, ClientInput } from '../types/client'
import type { Project } from '../types/project'

export interface ClientsData {
  projects: Project[]
}
export interface ClientsContextValue extends AsyncDataState<ClientsData> {
  saveClient: (input: ClientInput) => Promise<Client>
  archiveClient: (id: string) => Promise<void>
}
const emptyData: ClientsData = { projects: [] }

export function ClientsProvider({ children }: PropsWithChildren) {
  const load = useCallback(
    async (): Promise<ClientsData> => ({
      projects: await listAllProjects({ status: 'ALL' }),
    }),
    [],
  )
  const state = useAsyncData(load, emptyData)
  const value: ClientsContextValue = {
    ...state,
    async saveClient(input) {
      const saved = await saveClientRequest(input)
      await state.refresh()
      return saved
    },
    async archiveClient(id) {
      await updateClientStatus(id, false)
      await state.refresh()
    },
  }
  return <ClientsContext.Provider value={value}>{children}</ClientsContext.Provider>
}
