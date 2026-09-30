import { Card } from '../../../components/ui/card'
import { Button } from '../../../components/ui/button'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../../components/ui/table'
import { Label } from '../../../components/ui/label'
import { Input } from '../../../components/ui/input'
import { NativeSelect } from '../../../components/ui/native-select'
import { Textarea } from '../../../components/ui/textarea'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { FiAlertCircle, FiArrowLeft, FiCheckCircle, FiDownload, FiPlus } from 'react-icons/fi'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../../components/ui/dialog'
import PageHeader from '../../../components/PageHeader'
import StatusBadge from '../../../components/StatusBadge'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useWorkspace } from '../../../Workspace/useWorkspace'
import { formatDate, formatMoney } from '../../../utils/formatters'
import type { PaymentMethod } from '../../../types/billing'
import type { PaymentForm } from '../../../types/invoiceDetail'
import { effectiveInvoiceStatus, invoiceBalance } from '../../../utils/invoices'

const todayValue = new Date().toISOString().slice(0, 10)

function InvoiceDetailPage() {
  const { invoiceId } = useParams()
  const navigate = useNavigate()
  const { data, loading, error, refresh, save, remove } = useWorkspace()
  const [paymentOpen, setPaymentOpen] = useState(false)
  const [payment, setPayment] = useState<PaymentForm>({ amount: '', paid_at: todayValue, method: 'BANK_TRANSFER', notes: '' })
  const [actionError, setActionError] = useState('')

  if (loading) return <LoadingState label="Loading Invoices..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const invoice = data.invoices.find((item) => item.id === invoiceId)
  if (!invoice) return <EmptyState icon={<FiAlertCircle aria-hidden="true" />} title="ไม่พบใบแจ้งหนี้" description="เอกสารนี้อาจถูกลบหรือคุณไม่มีสิทธิ์เข้าถึง" action={<Button asChild variant="default"><Link className="button button-primary" to="/invoices">กลับไป ใบแจ้งหนี้</Link></Button>} />

  const items = data.invoice_items.filter((item) => item.invoice_id === invoice.id).sort((a, b) => a.sort_order - b.sort_order)
  const payments = data.payments.filter((item) => item.invoice_id === invoice.id).sort((a, b) => b.paid_at.localeCompare(a.paid_at))
  const balance = invoiceBalance(invoice)
  const displayStatus = effectiveInvoiceStatus(invoice)
  const seller = invoice.seller_snapshot || {}
  const client = invoice.client_snapshot || {}

  const issueInvoices = () => save('invoices', { ...invoice, status: 'ISSUED', issued_at: new Date().toISOString() })
  const voidInvoices = async () => {
    await save('invoices', { ...invoice, status: 'VOID' })
    for (const entry of data.time_entries.filter((item) => item.invoice_id === invoice.id)) await save('time_entries', { ...entry, invoice_id: null })
  }
  const deleteDraft = async () => {
    for (const item of items) await remove('invoice_items', item.id)
    for (const entry of data.time_entries.filter((item) => item.invoice_id === invoice.id)) await save('time_entries', { ...entry, invoice_id: null })
    await remove('invoices', invoice.id)
    navigate('/invoices')
  }
  const openPayment = () => {
    setPayment({ amount: balance.toFixed(2), paid_at: todayValue, method: 'BANK_TRANSFER', notes: '' })
    setActionError('')
    setPaymentOpen(true)
  }
  const recordPayment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const amount = Number(payment.amount)
    if (amount <= 0 || amount > balance) {
      setActionError(`จำนวนเงินต้องมากกว่า 0 และไม่เกิน ${formatMoney(balance, invoice.currency)}`)
      return
    }
    await save('payments', { ...payment, amount, invoice_id: invoice.id, currency: invoice.currency })
    const amountPaid = Number(invoice.amount_paid || 0) + amount
    await save('invoices', { ...invoice, amount_paid: amountPaid, status: amountPaid >= Number(invoice.total) ? 'PAID' : invoice.status })
    setPaymentOpen(false)
  }

  return (
    <div className="page-view invoice-detail-page">
      <Link className="back-link no-print" to="/invoices"><FiArrowLeft aria-hidden="true" /> กลับไปใบแจ้งหนี้</Link>
      <PageHeader eyebrow="ใบแจ้งหนี้ / รายละเอียด" title={invoice.invoice_number} description={`สร้างเมื่อ ${formatDate(invoice.created_at || invoice.issue_date)}`} actions={<><Button variant="outline" className="button button-secondary" type="button" onClick={() => window.print()}><FiDownload aria-hidden="true" /> พิมพ์ / PDF</Button>{invoice.status === 'DRAFT' && <><Button variant="destructive" className="button button-danger" type="button" onClick={deleteDraft}>ลบ ฉบับร่าง</Button><Button asChild variant="outline"><Link className="button button-secondary" to={`/invoices/${invoice.id}/edit`}>แก้ไข</Link></Button><Button variant="default" className="button button-primary" type="button" onClick={issueInvoices}>ออก ใบแจ้งหนี้</Button></>}{['ISSUED', 'OVERDUE'].includes(displayStatus) && <><Button variant="outline" className="button button-secondary" type="button" onClick={voidInvoices}>ยกเลิกเอกสาร</Button><Button variant="default" className="button button-primary" type="button" onClick={openPayment}><FiPlus aria-hidden="true" /> รับชำระ</Button></>}</>} />
      <article className="invoice-paper">
        <header className="invoice-paper-header"><div><span className="brand-mark">FH</span><h2>ใบแจ้งหนี้</h2><StatusBadge status={displayStatus} /></div><dl><div><dt>เลขที่ใบแจ้งหนี้</dt><dd>{invoice.invoice_number}</dd></div><div><dt>วันที่ออกเอกสาร</dt><dd>{formatDate(invoice.issue_date)}</dd></div><div><dt>วันครบกำหนด</dt><dd>{formatDate(invoice.due_date)}</dd></div></dl></header>
        <div className="invoice-addresses"><section><span>จาก</span><strong>{seller.name}</strong><p>{seller.address}</p><p>{seller.email} · {seller.phone}</p><p>เลขประจำตัวผู้เสียภาษี {seller.tax_id || '—'}</p></section><section><span>เรียกเก็บจาก</span><strong>{client.name}</strong><p>{client.contact_name}</p><p>{client.address}</p><p>{client.email} · {client.phone}</p><p>เลขประจำตัวผู้เสียภาษี {client.tax_id || '—'}</p></section></div>
        <div className="table-wrap"><Table className="invoice-items-table"><TableHeader><TableRow><TableHead>รายละเอียด</TableHead><TableHead>จำนวน</TableHead><TableHead>ราคาต่อหน่วย</TableHead><TableHead>จำนวนเงิน</TableHead></TableRow></TableHeader><TableBody>{items.map((item) => <TableRow key={item.id}><TableCell>{item.description}</TableCell><TableCell>{Number(item.quantity).toFixed(2)}</TableCell><TableCell>{formatMoney(item.unit_price, invoice.currency)}</TableCell><TableCell>{formatMoney(item.amount, invoice.currency)}</TableCell></TableRow>)}</TableBody></Table></div>
        <div className="invoice-bottom"><section><span>หมายเหตุ</span><p>{invoice.notes || '—'}</p>{seller.bank_name && <div className="bank-card"><strong>ข้อมูลรับชำระ</strong><p>{seller.bank_name}</p><p>{seller.bank_account_name} · {seller.bank_account_number}</p></div>}</section><dl className="invoice-total-list"><div><dt>ยอดก่อนภาษี</dt><dd>{formatMoney(invoice.subtotal, invoice.currency)}</dd></div><div><dt>ส่วนลด</dt><dd>−{formatMoney(invoice.discount_amount, invoice.currency)}</dd></div><div><dt>ภาษี {invoice.tax_rate}%</dt><dd>{formatMoney(invoice.tax_amount, invoice.currency)}</dd></div><div className="total"><dt>ยอดรวม</dt><dd>{formatMoney(invoice.total, invoice.currency)}</dd></div><div><dt>ชำระแล้ว</dt><dd>{formatMoney(invoice.amount_paid, invoice.currency)}</dd></div><div className="balance"><dt>คงเหลือ</dt><dd>{formatMoney(balance, invoice.currency)}</dd></div></dl></div>
      </article>
      {payments.length > 0 && <Card asChild><section className="panel no-print"><div className="panel-heading"><div><h2>ประวัติการรับชำระ</h2><p>ประวัติการรับชำระของเอกสารนี้</p></div></div><div className="list-stack">{payments.map((item) => <div className="list-item" key={item.id}><span className="finance-icon income"><FiCheckCircle aria-hidden="true" /></span><span><strong>{formatMoney(item.amount, item.currency)}</strong><small>{item.method.replaceAll('_', ' ')} · {item.notes || 'ไม่มีหมายเหตุ'}</small></span><span>{formatDate(item.paid_at)}</span></div>)}</div></section></Card>}
      <Dialog open={paymentOpen} onOpenChange={(open) => { if (!open) setPaymentOpen(false) }}>
        <DialogContent className="workspace-dialog">
          <DialogHeader><DialogTitle>บันทึกการรับชำระ</DialogTitle></DialogHeader>
        <form onSubmit={recordPayment}>{actionError && <p className="form-error">{actionError}</p>}<div className="form-grid"><div className="form-field"><Label htmlFor="payment-amount">จำนวนเงิน</Label><Input id="payment-amount" type="number" min="0.01" max={balance} step="0.01" value={payment.amount} onChange={(event) => setPayment((current) => ({ ...current, amount: event.target.value }))} required /></div><div className="form-field"><Label htmlFor="payment-date">วันที่รับเงิน</Label><Input id="payment-date" type="date" value={payment.paid_at} onChange={(event) => setPayment((current) => ({ ...current, paid_at: event.target.value }))} required /></div><div className="form-field full"><Label htmlFor="payment-method">วิธีชำระ</Label><NativeSelect id="payment-method" value={payment.method} onChange={(event) => setPayment((current) => ({ ...current, method: event.target.value as PaymentMethod }))}><option value="BANK_TRANSFER">โอนเงิน</option><option value="CASH">เงินสด</option><option value="CARD">บัตร</option><option value="OTHER">อื่น ๆ</option></NativeSelect></div><div className="form-field full"><Label htmlFor="payment-notes">หมายเหตุ</Label><Textarea id="payment-notes" value={payment.notes} onChange={(event) => setPayment((current) => ({ ...current, notes: event.target.value }))} /></div></div><div className="form-actions"><Button variant="outline" className="button button-secondary" type="button" onClick={() => setPaymentOpen(false)}>ยกเลิก</Button><Button variant="default" className="button button-primary" type="submit">บันทึกการชำระ</Button></div></form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default InvoiceDetailPage
