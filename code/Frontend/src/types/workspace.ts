import type { OwnedRecord } from './common'
import type { Profile } from './profile'
import type { Client } from './client'
import type { Project } from './project'
import type { Task } from './task'
import type { TimeEntry } from './timeTracking'
import type { FinanceEntry, Invoice, InvoiceItem, Payment } from './billing'

export interface WorkspaceData {
  profiles: Profile[]
  clients: Client[]
  projects: Project[]
  tasks: Task[]
  time_entries: TimeEntry[]
  finance_entries: FinanceEntry[]
  invoices: Invoice[]
  invoice_items: InvoiceItem[]
  payments: Payment[]
}

export type ResourceMap = WorkspaceData
export type ResourceName = keyof ResourceMap
export type ResourceRecord<K extends ResourceName> = ResourceMap[K][number]
export type ResourceInput<K extends ResourceName> = Omit<ResourceRecord<K>, keyof OwnedRecord> & Partial<OwnedRecord>
