import { Card } from '../../../components/ui/card'
import { Label } from '../../../components/ui/label'
import { Input } from '../../../components/ui/input'
import { NativeSelect } from '../../../components/ui/native-select'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../../components/ui/table'
import { Button } from '../../../components/ui/button'
import { Link } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { FiCheckCircle, FiClock, FiEdit3, FiFileText, FiPlus, FiSearch } from 'react-icons/fi'
import PageHeader from '../../../components/PageHeader'
import StatusBadge from '../../../components/StatusBadge'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useWorkspace } from '../../../Workspace/useWorkspace'
import { formatDate, formatMoney } from '../../../utils/formatters'
import type { InvoiceStatus } from '../../../types/billing'
import { effectiveInvoiceStatus, invoiceBalance } from '../../../utils/invoices'

function InvoicesPage() {
  const { data, loading, error, refresh } = useWorkspace()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<'ALL' | InvoiceStatus>('ALL')
  const [sortBy, setSortBy] = useState<'ISSUE_DESC' | 'DUE_ASC' | 'TOTAL_DESC'>('ISSUE_DESC')
  const [page, setPage] = useState(1)

  const invoices = useMemo(() => (data?.invoices || [])
    .filter((invoice) => status === 'ALL' || effectiveInvoiceStatus(invoice) === status)
    .filter((invoice) => {
      const client = data.clients.find((item) => item.id === invoice.client_id)
      const searchable = `${invoice.invoice_number} ${client?.name || ''} ${client?.company_name || ''}`.toLowerCase()
      return searchable.includes(query.toLowerCase())
    })
    .sort((a, b) => {
      if (sortBy === 'DUE_ASC') return a.due_date.localeCompare(b.due_date)
      if (sortBy === 'TOTAL_DESC') return Number(b.total) - Number(a.total)
      return b.issue_date.localeCompare(a.issue_date)
    }), [data, query, sortBy, status])

  if (loading) return <LoadingState label="กำลังโหลดใบแจ้งหนี้..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const issued = data.invoices.filter((invoice) => ['ISSUED', 'OVERDUE'].includes(effectiveInvoiceStatus(invoice)))
  const outstanding = issued.reduce((sum, invoice) => sum + invoiceBalance(invoice), 0)
  const paid = data.invoices.filter((invoice) => invoice.status === 'PAID').reduce((sum, invoice) => sum + Number(invoice.amount_paid || invoice.total), 0)
  const drafts = data.invoices.filter((invoice) => invoice.status === 'DRAFT').reduce((sum, invoice) => sum + Number(invoice.total), 0)
  const pageSize = 8
  const totalPages = Math.max(1, Math.ceil(invoices.length / pageSize))
  const safePage = Math.min(page, totalPages)
  const visibleInvoices = invoices.slice((safePage - 1) * pageSize, safePage * pageSize)

  return (
    <div className="page-view">
      <PageHeader eyebrow="จัดการ / ใบแจ้งหนี้" title="ใบแจ้งหนี้" description="สร้าง ส่ง และติดตามการชำระเงินจากที่เดียว" actions={<Button asChild variant="default"><Link className="button button-primary" to="/invoices/new"><FiPlus aria-hidden="true" /> สร้างใบแจ้งหนี้</Link></Button>} />
      <div className="summary-grid">
        <Card asChild><article className="metric-card accent-orange"><div className="metric-top"><span>ยอดรอรับ</span><span className="metric-icon"><FiClock aria-hidden="true" /></span></div><div className="metric-value metric-compact">{formatMoney(outstanding)}</div><div className="metric-foot">{issued.length} ฉบับที่ยังมียอดคงเหลือ</div></article></Card>
        <Card asChild><article className="metric-card accent-green"><div className="metric-top"><span>รับชำระแล้ว</span><span className="metric-icon"><FiCheckCircle aria-hidden="true" /></span></div><div className="metric-value metric-compact">{formatMoney(paid)}</div><div className="metric-foot">รายได้จาก ใบแจ้งหนี้ ทั้งหมด</div></article></Card>
        <Card asChild><article className="metric-card accent-violet"><div className="metric-top"><span>ฉบับร่าง</span><span className="metric-icon"><FiEdit3 aria-hidden="true" /></span></div><div className="metric-value metric-compact">{formatMoney(drafts)}</div><div className="metric-foot">รอตรวจสอบก่อนออกเอกสาร</div></article></Card>
        <Card asChild><article className="metric-card accent-blue"><div className="metric-top"><span>ใบแจ้งหนี้ ทั้งหมด</span><span className="metric-icon"><FiFileText aria-hidden="true" /></span></div><div className="metric-value">{data.invoices.length}<span className="metric-unit">ฉบับ</span></div><div className="metric-foot">รวมทุกสถานะ</div></article></Card>
      </div>

      <Card asChild><section className="panel">
        <div className="panel-heading"><div><h2>รายการใบแจ้งหนี้</h2><p>ค้นหาตามเลขที่เอกสารหรือลูกค้า</p></div></div>
        <div className="filter-row"><Label className="search-box"><span><FiSearch aria-hidden="true" /></span><Input value={query} onChange={(event) => { setQuery(event.target.value); setPage(1) }} placeholder="ค้นหา ใบแจ้งหนี้หรือลูกค้า..." /></Label><NativeSelect className="select-button" value={status} onChange={(event) => { setStatus(event.target.value as 'ALL' | InvoiceStatus); setPage(1) }}><option value="ALL">ทุกสถานะ</option><option value="DRAFT">ฉบับร่าง</option><option value="ISSUED">ออกแล้ว</option><option value="PAID">ชำระแล้ว</option><option value="OVERDUE">เกินกำหนด</option><option value="VOID">ยกเลิก</option></NativeSelect><NativeSelect className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value as 'ISSUE_DESC' | 'DUE_ASC' | 'TOTAL_DESC'); setPage(1) }}><option value="ISSUE_DESC">ออกล่าสุด</option><option value="DUE_ASC">ครบกำหนดใกล้สุด</option><option value="TOTAL_DESC">ยอดสูงสุด</option></NativeSelect></div>
        {invoices.length === 0 ? <EmptyState icon={<FiFileText aria-hidden="true" />} title="ไม่พบใบแจ้งหนี้" description="ลองเปลี่ยนคำค้นหาหรือสร้างใบแจ้งหนี้ ฉบับแรก" action={<Button asChild variant="default"><Link className="button button-primary" to="/invoices/new">สร้าง ใบแจ้งหนี้</Link></Button>} /> : (
          <div className="table-wrap"><Table className="data-table" pagination={{ page: safePage, totalPages, total: invoices.length, onPageChange: setPage }}><TableHeader><TableRow><TableHead>เลขที่ ใบแจ้งหนี้</TableHead><TableHead>ลูกค้า</TableHead><TableHead>วันที่ออก</TableHead><TableHead>ครบกำหนด</TableHead><TableHead>ยอดรวม</TableHead><TableHead>คงเหลือ</TableHead><TableHead>สถานะ</TableHead><TableHead /></TableRow></TableHeader><TableBody>{visibleInvoices.map((invoice) => {
            const client = data.clients.find((item) => item.id === invoice.client_id)
            const balance = invoiceBalance(invoice)
            return <TableRow key={invoice.id}><TableCell><strong>{invoice.invoice_number}</strong></TableCell><TableCell><div className="table-primary"><span className="client-avatar table-avatar" style={{ background: client?.color }}>{(client?.company_name || client?.name || 'CL').slice(0, 2).toUpperCase()}</span><span><strong>{client?.company_name || client?.name}</strong><small>{client?.email}</small></span></div></TableCell><TableCell>{formatDate(invoice.issue_date)}</TableCell><TableCell>{formatDate(invoice.due_date)}</TableCell><TableCell><strong>{formatMoney(invoice.total, invoice.currency)}</strong></TableCell><TableCell>{formatMoney(balance, invoice.currency)}</TableCell><TableCell><StatusBadge status={effectiveInvoiceStatus(invoice)} /></TableCell><TableCell><Button asChild variant="ghost"><Link className="mini-button text-link" to={`/invoices/${invoice.id}`}>ดูรายละเอียด</Link></Button></TableCell></TableRow>
          })}</TableBody></Table></div>
        )}
      </section></Card>
    </div>
  )
}

export default InvoicesPage
