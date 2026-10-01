import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useAsyncData } from '../shared/useAsyncData'
import { listClientOptions } from '../services/client'
import { listTimerProjects, listTimerTasks } from '../services/timerOptions'
import { listTimeEntries } from '../services/timeTracking'
import type { AsyncDataState } from '../types/asyncData'
import type { AnalyticsData } from '../types/analytics'

export interface AnalyticsContextValue extends AsyncDataState<AnalyticsData> {}
export const AnalyticsContext = createContext<AnalyticsContextValue | null>(null)
const emptyData: AnalyticsData = { clients: [], projects: [], tasks: [], time_entries: [] }

export function AnalyticsProvider({ children }: PropsWithChildren) {
  const load = useCallback(async (): Promise<AnalyticsData> => {
    const [clients, projects, time_entries] = await Promise.all([listClientOptions(), listTimerProjects(), listTimeEntries()])
    const tasks = (await Promise.all(projects.map((project) => listTimerTasks(project.id)))).flat()
    return { clients, projects, tasks, time_entries }
  }, [])
  const value = useAsyncData(load, emptyData)
  return <AnalyticsContext.Provider value={value}>{children}</AnalyticsContext.Provider>
}
