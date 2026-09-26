import type { CurrencyCode, OwnedRecord } from './common'

export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export type BillingType = 'HOURLY' | 'FIXED_PRICE'

export interface Project extends OwnedRecord {
  client_id: string
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
}
