import type { ProjectStatus } from './project'
import type { AsyncDataState } from './asyncData'

export interface DashboardSummary {
  weekTrackedSeconds: number
  weekTrendPercent: number | null
  activeProjectCount: number
  activeProjectTrackedSeconds: number
  activeProjectTargetSeconds: number
  targetUsagePercent: number | null
  completedTaskCount: number
  totalTaskCount: number
  completedTaskPercent: number
}

export interface DashboardDailyWork {
  date: string
  trackedSeconds: number
}

export interface DashboardActiveProject {
  id: string
  name: string
  clientName: string
  color: string | null
  status: ProjectStatus
  completedTaskCount: number
  totalTaskCount: number
  taskProgressPercent: number
}

export interface DashboardOpenTask {
  id: string
  projectId: string
  name: string
  projectName: string
  status: 'OPEN' | 'IN_PROGRESS' | 'COMPLETED'
}

export interface DashboardData {
  generatedAt: string
  summary: DashboardSummary
  dailyWork: DashboardDailyWork[]
  activeProjects: DashboardActiveProject[]
  openTasks: DashboardOpenTask[]
  recentTimeEntries: DashboardRecentTimeEntry[]
}

export interface DashboardRecentTimeEntry {
  id: string
  projectName: string
  taskName: string | null
  description: string | null
  startedAt: string
  durationSeconds: number
}

export type DashboardChartPeriod = 'WEEK' | 'MONTH' | 'YEAR'

export interface DashboardActivity {
  period: DashboardChartPeriod
  points: DashboardDailyWork[]
}

export type DashboardResponse = Omit<DashboardData, 'recentTimeEntries'> & {
  recentTimeEntries?: DashboardRecentTimeEntry[] | null
}

export interface DashboardContextValue extends AsyncDataState<DashboardData> {
  loadActivity: (period: DashboardChartPeriod) => Promise<DashboardActivity>
}
