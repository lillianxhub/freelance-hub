import type { CurrencyCode, OwnedRecord } from './common'

export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export type BillingType = 'HOURLY' | 'FIXED_PRICE'

export interface ProjectTaskProgress {
  total_tasks: number
  completed_tasks: number
  percent: number
}

export interface ProjectTimeTracking {
  tracked_seconds: number
  tracked_hours: number
  usage_percent: number
}

export interface Project extends OwnedRecord {
  client_id: string
  client_name?: string
  name: string
  description: string
  color: string
  status: ProjectStatus
  billing_type: BillingType
  hourly_rate: number | null
  fixed_price: number | null
  budget_hours: number | null
  budget_amount: number | null
  currency: CurrencyCode
  start_date: string
  end_date: string
  task_progress?: ProjectTaskProgress
  time_tracking?: ProjectTimeTracking | null
}
