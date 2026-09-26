import type { Invoice } from './billing'
import type { WorkspaceContextValue } from './workspaceContext'

export interface InvoiceEditorProps {
  workspace: WorkspaceContextValue
  existing?: Invoice | null
}

export interface ManualInvoiceItem {
  id: string
  description: string
  quantity: number | string
  unit_price: number | string
}

export interface InvoiceEditorForm {
  issue_date: string
  due_date: string
  tax_rate: number | string
  discount_amount: number | string
  notes: string
}
