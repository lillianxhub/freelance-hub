export function effectiveInvoiceStatus(invoice, today = new Date().toISOString().slice(0, 10)) {
  if (invoice.status === 'ISSUED' && invoice.due_date < today && Number(invoice.amount_paid || 0) < Number(invoice.total)) return 'OVERDUE'
  return invoice.status
}

export function invoiceBalance(invoice) {
  return Math.max(0, Number(invoice.total) - Number(invoice.amount_paid || 0))
}
