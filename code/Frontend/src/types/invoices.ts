import type { Invoice } from './billing'

export type InvoiceState = Pick<Invoice, 'status' | 'due_date' | 'amount_paid' | 'total'>
