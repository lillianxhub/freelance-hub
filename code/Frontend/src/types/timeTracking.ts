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
