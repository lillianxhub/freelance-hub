import type { Invoice, InvoiceStatus } from '../types/domain'

type InvoiceState = Pick<Invoice, 'status' | 'due_date' | 'amount_paid' | 'total'>

export function effectiveInvoiceStatus(invoice: InvoiceState, today = new Date().toISOString().slice(0, 10)): InvoiceStatus {
  if (invoice.status === 'ISSUED' && invoice.due_date < today && Number(invoice.amount_paid || 0) < Number(invoice.total)) return 'OVERDUE'
  return invoice.status
}

export function invoiceBalance(invoice: Pick<Invoice, 'total' | 'amount_paid'>) {
  return Math.max(0, Number(invoice.total) - Number(invoice.amount_paid || 0))
}
