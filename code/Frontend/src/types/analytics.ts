import type { Project } from './project'
import type { TimeEntry } from './timeTracking'
import type { Client } from './client'
import type { Task } from './task'
import type { ProjectStatus } from './project'

export interface AnalyticsData {
  clients: Client[]
  projects: Project[]
  tasks: Task[]
  time_entries: TimeEntry[]
}
export type TimeSummaryEntry = Pick<TimeEntry, 'duration_minutes' | 'billable' | 'rate_snapshot'>
export type CsvValue = string | number | boolean | null | undefined

export interface ProductivityPoint {
  key: string
  day: string
  totalSeconds: number
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

export interface ReportSummaryQuery {
  from: string
  to: string
  clientId?: string
  projectId?: string
}

export interface ReportFilterOption {
  id: string
  name: string
}

export interface ReportProjectOption extends ReportFilterOption {
  clientId: string
}

export interface ReportSummaryKpi {
  totalTrackedSeconds: number
  trackedTimeTrendPercent: number
  timeEntryCount: number
  projectsWithTime: number
  totalProjects: number
  clientsWithTime: number
  totalClients: number
}

export interface ReportClientTime {
  clientId: string
  clientName: string
  trackedSeconds: number
  percent: number
}

export interface ReportProjectUsage {
  projectId: string
  projectName: string
  clientId: string
  clientName: string
  color: string
  targetSeconds: number | null
  trackedSeconds: number
  usagePercent: number | null
  taskProgressPercent: number
  status: ProjectStatus
}

export interface ReportSummaryData {
  generatedAt: string
  filters: {
    clients: ReportFilterOption[]
    projects: ReportProjectOption[]
  }
  summary: ReportSummaryKpi
  timeByClient: ReportClientTime[]
  projectUsage: ReportProjectUsage[]
}
