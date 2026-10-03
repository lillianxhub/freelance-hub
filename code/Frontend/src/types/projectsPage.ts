import type { ChangeEvent, FormEvent } from 'react'
import type { Client } from './client'
import type { BillingType, Project, ProjectStatus } from './project'
import type { ProjectInput } from './project'

export type ProjectFilter = 'ALL' | ProjectStatus
export type BillingFilter = 'ALL' | BillingType
export type ProjectSort = 'UPDATED_DESC' | 'NAME_ASC' | 'END_ASC'

export type ProjectDraft = Omit<ProjectInput, 'hourly_rate' | 'fixed_price' | 'budget_hours' | 'budget_amount'> & {
  hourly_rate: number | ''
  fixed_price: number | ''
  budget_hours: number | ''
  budget_amount: number | ''
}

export interface ProjectFormProps {
  value: ProjectDraft
  clients: readonly Client[]
  error: string
  dateErrors: Partial<Record<'start_date' | 'end_date', string>>
  saving: boolean
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

export interface ProjectTableProps {
  projects: readonly Project[]
  clients: readonly Client[]
}

export interface ProjectStatusProps {
  status: ProjectStatus
}
