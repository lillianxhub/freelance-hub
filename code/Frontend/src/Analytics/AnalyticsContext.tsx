import { createContext, useCallback, type PropsWithChildren } from 'react'
import { useAsyncData } from '../shared/useAsyncData'
import { listClientOptions } from '../services/client'
import { listTimerProjects, listTimerTasks } from '../services/timerOptions'
import { listTimeEntriesPage } from '../services/timeTracking'
import type { AsyncDataState } from '../types/asyncData'
import type { AnalyticsData } from '../types/analytics'

export interface AnalyticsContextValue extends AsyncDataState<AnalyticsData> {}
export const AnalyticsContext = createContext<AnalyticsContextValue | null>(null)
export const emptyAnalyticsData: AnalyticsData = { clients: [], projects: [], tasks: [], time_entries: [] }

export async function loadAnalyticsData(): Promise<AnalyticsData> {
    const [clients, projects, firstTimeEntryPage] = await Promise.all([
      listClientOptions(),
      listTimerProjects(),
      listTimeEntriesPage({ page: 1, limit: 100 }),
    ])
    const remainingTimeEntryPages = await Promise.all(
      Array.from({ length: Math.max(0, firstTimeEntryPage.meta.totalPages - 1) }, (_, index) =>
        listTimeEntriesPage({ page: index + 2, limit: 100 }),
      ),
    )
    const time_entries = [
      ...firstTimeEntryPage.entries,
      ...remainingTimeEntryPages.flatMap((page) => page.entries),
    ]
    const tasks = (await Promise.all(projects.map((project) => listTimerTasks(project.id)))).flat()
    return { clients, projects, tasks, time_entries }
}

export function AnalyticsProvider({ children }: PropsWithChildren) {
  const load = useCallback(loadAnalyticsData, [])
  const value = useAsyncData(load, emptyAnalyticsData)
  return <AnalyticsContext.Provider value={value}>{children}</AnalyticsContext.Provider>
}
