import type { ChangeEvent, FormEvent } from 'react'
import type { CurrencyCode } from './common'
import type { Project } from './project'
import type { Task } from './task'
import type { TimeEntry } from './timeTracking'
import type { TablePaginationProps } from '../components/ui/table'

export type ManualMode = 'RANGE' | 'DURATION'
export type ManualTimeFieldName =
  | 'project_id'
  | 'task_id'
  | 'entry_date'
  | 'manual_mode'
  | 'start_time'
  | 'end_time'
  | 'duration_minutes'
export type ManualTimeFieldErrors = Partial<Record<ManualTimeFieldName, string>>

export interface ManualTimeForm {
  id?: string
  owner_id?: string
  created_at?: string
  updated_at?: string
  project_id: string
  task_id: string
  description: string
  entry_date: string
  start_time: string
  end_time: string
  manual_mode: ManualMode
  duration_minutes: number | string
  billable: boolean
  rate_snapshot: number | string
  currency: CurrencyCode
}

export type RangePreset = 'DAY' | 'WEEK' | 'ALL'

export interface TimeFilters {
  client: string
  project: string
  task: string
  billable: 'ALL' | 'true' | 'false'
  from: string
  to: string
}

export interface TimeEntryFormProps {
  value: ManualTimeForm
  projects: readonly Project[]
  tasks: readonly Task[]
  error: string
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

export interface TimeEntryTableProps {
  entries: readonly TimeEntry[]
  projects: readonly Project[]
  tasks?: readonly Task[]
  pagination?: TablePaginationProps
  onEdit: (entry: TimeEntry) => void
  onDelete: (entry: TimeEntry) => void
  onDuplicate: (entry: TimeEntry) => void
}

export interface TimeSummaryProps {
  totalMinutes: number
  billableMinutes: number
  totalValue: number
  count: number
}
