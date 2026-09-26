import type { CurrencyCode, OwnedRecord } from './common'

export type FinanceType = 'INCOME' | 'EXPENSE'
export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'VOID'
export type PaymentMethod = 'BANK_TRANSFER' | 'CASH' | 'CARD' | 'OTHER'

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
