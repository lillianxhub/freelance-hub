export type CurrencyCode = 'THB' | 'USD' | 'EUR' | 'SGD'
export type ClientStatus = 'ACTIVE' | 'ARCHIVED'
export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export type BillingType = 'HOURLY' | 'FIXED_PRICE'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type FinanceType = 'INCOME' | 'EXPENSE'
export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'VOID'
export type PaymentMethod = 'BANK_TRANSFER' | 'CASH' | 'CARD' | 'OTHER'

export interface OwnedRecord {
  id: string
  owner_id: string
  created_at?: string
  updated_at?: string
}

export interface Profile extends OwnedRecord {
  full_name: string
  email: string
  phone: string
  address: string
  tax_id: string
  logo_url: string
  bank_name: string
  bank_account_name: string
  bank_account_number: string
  timezone: string
  currency: CurrencyCode
  date_format: string
  default_tax_rate: number
  default_hourly_rate: number
}

export interface Client extends OwnedRecord {
  name: string
  company_name: string
  email: string
  phone: string
  address: string
  tax_id: string
  notes: string
  status: ClientStatus
  color: string
}

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

export interface Task extends OwnedRecord {
  project_id: string
  name: string
  description: string
  status: TaskStatus
  sort_order: number
  due_date: string
}

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

export interface FinanceEntry extends OwnedRecord {
  project_id: string | null
  type: FinanceType
  category: string
  amount: number
  currency: CurrencyCode
  entry_date: string
  notes: string
}

export interface InvoicePartySnapshot {
  name?: string
  contact_name?: string
  email?: string
  phone?: string
  address?: string
  tax_id?: string
  bank_name?: string
  bank_account_name?: string
  bank_account_number?: string
}

export interface Invoice extends OwnedRecord {
  client_id: string
  project_id: string | null
  invoice_number: string
  issue_date: string
  due_date: string
  status: InvoiceStatus
  currency: CurrencyCode
  subtotal: number
  discount_amount: number
  tax_rate: number
  tax_amount: number
  total: number
  amount_paid: number
  notes: string
  seller_snapshot: InvoicePartySnapshot | null
  client_snapshot: InvoicePartySnapshot | null
  issued_at?: string | null
}

export interface InvoiceItem extends OwnedRecord {
  invoice_id: string
  time_entry_id: string | null
  description: string
  quantity: number
  unit_price: number
  amount: number
  sort_order: number
}

export interface Payment extends OwnedRecord {
  invoice_id: string
  amount: number
  currency: CurrencyCode
  paid_at: string
  method: PaymentMethod
  notes: string
}

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
