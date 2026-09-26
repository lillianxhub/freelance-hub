import type { FinanceType } from './billing'
import type { ResourceInput } from './workspace'

export type FinanceForm = Omit<ResourceInput<'finance_entries'>, 'amount' | 'project_id'> & {
  amount: number | ''
  project_id: string
}

export interface FinanceFilters {
  type: 'ALL' | FinanceType
  project: string
  from: string
  to: string
}
