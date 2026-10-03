import type { Project } from './project'
import type { TimeEntry } from './timeTracking'
import type { WorkspaceData } from './workspace'

export type AnalyticsData = Pick<WorkspaceData, 'clients' | 'projects' | 'tasks' | 'time_entries'>
export type TimeSummaryEntry = Pick<TimeEntry, 'duration_minutes' | 'billable' | 'rate_snapshot'>
export type CsvValue = string | number | boolean | null | undefined

export interface ProductivityPoint {
  key: string
  day: string
  total: number
}

export interface ProductivityChartProps {
  data: readonly ProductivityPoint[]
}

export interface ProjectTimePoint {
  key: string
  name: string
  hours: number
}

export interface ProjectTimeChartProps {
  data: readonly ProjectTimePoint[]
}

export interface RecentActivityProps {
  entries: readonly TimeEntry[]
  projects: readonly Project[]
}
