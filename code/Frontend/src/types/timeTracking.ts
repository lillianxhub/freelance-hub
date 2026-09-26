import type { CurrencyCode, OwnedRecord } from './common'

export interface TimeEntry extends OwnedRecord {
  project_id: string
  task_id: string | null
  description: string
  started_at: string
  ended_at: string | null
  duration_minutes: number | null
  billable: boolean
  rate_snapshot: number
  currency: CurrencyCode
  invoice_id: string | null
}

export interface ManualTimeEntryPayload {
  projectId: string
  taskId: string | null
  description: string
  startedAt: string
  endedAt?: string
  durationMinutes?: number
}

export interface UpdateTimeEntryPayload {
  projectId?: string
  taskId?: string
  clearTask?: boolean
  description?: string
  startedAt?: string
  endedAt?: string
}
