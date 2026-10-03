import type { ProjectStatus } from '../types/project'
import type { TaskStatus } from '../types/task'

/** Query parameters expected by GET /api/dashboard. */
export interface DashboardQuery {
  from: string
  to: string
  recentTimeEntryLimit?: number
  activeProjectLimit?: number
  pendingTaskLimit?: number
}

export interface DashboardTimeMetric {
  totalSeconds: number
  changePercent: number
}

export interface DashboardTimeEntryItem {
  id: string
  project: { id: string; name: string; color: string }
  task: { id: string; name: string } | null
  description: string | null
  startedAt: string
  endedAt: string | null
  durationSeconds: number
}

export interface DashboardProjectItem {
  id: string
  name: string
  color: string
  clientName: string | null
  status: ProjectStatus
  endDate: string | null
  trackedSeconds: number
  taskProgress: { completed: number; total: number; percent: number }
}

export interface DashboardTaskItem {
  id: string
  name: string
  status: TaskStatus
  dueDate: string | null
  project: { id: string; name: string; color: string }
}

/** Complete payload expected inside the project's standard ApiResponse.data field. */
export interface DashboardData {
  summary: {
    today: DashboardTimeMetric
    thisWeek: DashboardTimeMetric
    projects: { active: number; total: number }
    tasks: { completed: number; total: number }
  }
  dailyHours: Array<{ date: string; totalSeconds: number }>
  currentTimer: DashboardTimeEntryItem | null
  recentTimeEntries: DashboardTimeEntryItem[]
  activeProjects: DashboardProjectItem[]
  pendingTasks: DashboardTaskItem[]
}
