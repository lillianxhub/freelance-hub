import { useCallback, type PropsWithChildren } from 'react'
import { TimeEntriesContext } from './time-entries-context'
import { useAsyncData } from '../hooks/useAsyncData'
import { listClientOptions } from '../services/client'
import { listTimerProjects } from '../services/timerOptions'
import { deleteTimeEntry as deleteTimeEntryRequest, listTimeEntries, saveTimeEntry as saveTimeEntryRequest } from '../services/timeTracking'
import type { AsyncDataState } from '../types/asyncData'
import type { Client } from '../types/client'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import type { TimeEntry, TimeEntryInput } from '../types/timeTracking'

export interface TimeEntriesData { clients: Client[]; projects: Project[]; tasks: Task[]; time_entries: TimeEntry[] }
export interface TimeEntriesContextValue extends AsyncDataState<TimeEntriesData> {
  saveTimeEntry: (input: TimeEntryInput) => Promise<TimeEntry>
  deleteTimeEntry: (id: string) => Promise<void>
}
const emptyData: TimeEntriesData = { clients: [], projects: [], tasks: [], time_entries: [] }

export function TimeEntriesProvider({ children }: PropsWithChildren) {
  const load = useCallback(async () => {
    const [clients, projects, time_entries] = await Promise.all([listClientOptions(), listTimerProjects(), listTimeEntries()])
    return { clients, projects, tasks: [], time_entries }
  }, [])
  const state = useAsyncData(load, emptyData)
  const value: TimeEntriesContextValue = { ...state,
    async saveTimeEntry(input) { const result = await saveTimeEntryRequest(input); await state.refresh(); return result },
    async deleteTimeEntry(id) { await deleteTimeEntryRequest(id); await state.refresh() },
  }
  return <TimeEntriesContext.Provider value={value}>{children}</TimeEntriesContext.Provider>
}
