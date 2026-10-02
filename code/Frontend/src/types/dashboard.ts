import type { ProjectStatus } from './project'

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
}

export type DashboardChartPeriod = 'WEEK' | 'MONTH' | 'YEAR'

export interface DashboardActivity {
  period: DashboardChartPeriod
  points: DashboardDailyWork[]
}
