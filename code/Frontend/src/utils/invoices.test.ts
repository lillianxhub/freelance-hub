import assert from 'node:assert/strict'
import test from 'node:test'
import { effectiveInvoiceStatus, invoiceBalance } from './invoices'
import type { InvoiceStatus } from '../types/domain'

test('issued invoice becomes overdue after due date with balance remaining', () => {
  const invoice = { status: 'ISSUED' as InvoiceStatus, due_date: '2026-09-20', total: 10000, amount_paid: 2000 }
  assert.equal(effectiveInvoiceStatus(invoice, '2026-09-25'), 'OVERDUE')
  assert.equal(invoiceBalance(invoice), 8000)
})

test('paid and void invoices keep their explicit status', () => {
  assert.equal(effectiveInvoiceStatus({ status: 'PAID', due_date: '2026-01-01', total: 100, amount_paid: 100 }, '2026-09-25'), 'PAID')
  assert.equal(effectiveInvoiceStatus({ status: 'VOID', due_date: '2026-01-01', total: 100, amount_paid: 0 }, '2026-09-25'), 'VOID')
})

test('invoice balance never becomes negative', () => {
  assert.equal(invoiceBalance({ total: 100, amount_paid: 110 }), 0)
})
