import type { CurrencyCode, OwnedRecord } from './common'

export interface TimeEntry extends OwnedRecord {
  project_id: string
  project_name?: string
  task_id: string | null
  task_name?: string
  description: string
  started_at: string
  ended_at: string | null
  duration_minutes: number | null
  duration_seconds?: number | null
  billable: boolean
  rate_snapshot: number
  currency: CurrencyCode
}

export interface ManualTimeEntryPayload {
  projectId: string
  taskId: string | null
  description: string
  startedAt: string
  endedAt?: string
  durationSeconds?: number
}

export interface StartTimerPayload {
  projectId: string
  taskId?: string
  description?: string
}

export interface UpdateTimeEntryPayload {
  projectId?: string
  taskId?: string
  clearTask?: boolean
  description?: string
  startedAt?: string
  endedAt?: string
}
