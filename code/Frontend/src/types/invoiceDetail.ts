import type { ResourceInput } from './workspace'

export type PaymentForm = Omit<ResourceInput<'payments'>, 'invoice_id' | 'currency' | 'amount'> & {
  amount: string
}
