import type { ChangeEvent, FormEvent } from 'react'
import type { CurrencyCode } from './common'
import type { Project } from './project'
import type { Task } from './task'
import type { TimeEntry } from './timeTracking'

export type ManualMode = 'RANGE' | 'DURATION'

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
  invoice_id?: string | null
}

export type RangePreset = 'DAY' | 'WEEK' | 'ALL'

export interface TimeFilters {
  client: string
  project: string
  task: string
  billable: 'ALL' | 'true' | 'false'
  invoice: 'ALL' | 'UNBILLED' | 'INVOICED'
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
  tasks: readonly Task[]
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
