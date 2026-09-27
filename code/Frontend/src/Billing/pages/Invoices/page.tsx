import { Link } from 'react-router-dom'
import { useMemo, useState } from 'react'
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
      <PageHeader eyebrow="จัดการ / ใบแจ้งหนี้" title="ใบแจ้งหนี้" description="สร้าง ส่ง และติดตามการชำระเงินจากที่เดียว" actions={<Link className="button button-primary" to="/invoices/new">＋ สร้าง ใบแจ้งหนี้</Link>} />
      <div className="summary-grid">
        <article className="metric-card accent-orange"><div className="metric-top"><span>ยอดรอรับ</span><span className="metric-icon">◷</span></div><div className="metric-value metric-compact">{formatMoney(outstanding)}</div><div className="metric-foot">{issued.length} ฉบับที่ยังมียอดคงเหลือ</div></article>
        <article className="metric-card accent-green"><div className="metric-top"><span>รับชำระแล้ว</span><span className="metric-icon">✓</span></div><div className="metric-value metric-compact">{formatMoney(paid)}</div><div className="metric-foot">รายได้จาก ใบแจ้งหนี้ ทั้งหมด</div></article>
        <article className="metric-card accent-violet"><div className="metric-top"><span>ฉบับร่าง</span><span className="metric-icon">◇</span></div><div className="metric-value metric-compact">{formatMoney(drafts)}</div><div className="metric-foot">รอตรวจสอบก่อนออกเอกสาร</div></article>
        <article className="metric-card accent-blue"><div className="metric-top"><span>ใบแจ้งหนี้ ทั้งหมด</span><span className="metric-icon">▤</span></div><div className="metric-value">{data.invoices.length}<span className="metric-unit">ฉบับ</span></div><div className="metric-foot">รวมทุกสถานะ</div></article>
      </div>

      <section className="panel">
        <div className="panel-heading"><div><h2>รายการใบแจ้งหนี้</h2><p>ค้นหาตามเลขที่เอกสารหรือลูกค้า</p></div></div>
        <div className="filter-row"><label className="search-box"><span>⌕</span><input value={query} onChange={(event) => { setQuery(event.target.value); setPage(1) }} placeholder="ค้นหา ใบแจ้งหนี้หรือลูกค้า..." /></label><select className="select-button" value={status} onChange={(event) => { setStatus(event.target.value as 'ALL' | InvoiceStatus); setPage(1) }}><option value="ALL">ทุกสถานะ</option><option value="DRAFT">ฉบับร่าง</option><option value="ISSUED">ออกแล้ว</option><option value="PAID">ชำระแล้ว</option><option value="OVERDUE">เกินกำหนด</option><option value="VOID">ยกเลิก</option></select><select className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value as 'ISSUE_DESC' | 'DUE_ASC' | 'TOTAL_DESC'); setPage(1) }}><option value="ISSUE_DESC">ออกล่าสุด</option><option value="DUE_ASC">ครบกำหนดใกล้สุด</option><option value="TOTAL_DESC">ยอดสูงสุด</option></select></div>
        {invoices.length === 0 ? <EmptyState icon="▤" title="ไม่พบใบแจ้งหนี้" description="ลองเปลี่ยนคำค้นหาหรือสร้างใบแจ้งหนี้ ฉบับแรก" action={<Link className="button button-primary" to="/invoices/new">สร้าง ใบแจ้งหนี้</Link>} /> : (
          <div className="table-wrap"><table className="data-table"><thead><tr><th>เลขที่ ใบแจ้งหนี้</th><th>ลูกค้า</th><th>วันที่ออก</th><th>ครบกำหนด</th><th>ยอดรวม</th><th>คงเหลือ</th><th>สถานะ</th><th /></tr></thead><tbody>{visibleInvoices.map((invoice) => {
            const client = data.clients.find((item) => item.id === invoice.client_id)
            const balance = invoiceBalance(invoice)
            return <tr key={invoice.id}><td><strong>{invoice.invoice_number}</strong></td><td><div className="table-primary"><span className="client-avatar table-avatar" style={{ background: client?.color }}>{(client?.company_name || client?.name || 'CL').slice(0, 2).toUpperCase()}</span><span><strong>{client?.company_name || client?.name}</strong><small>{client?.email}</small></span></div></td><td>{formatDate(invoice.issue_date)}</td><td>{formatDate(invoice.due_date)}</td><td><strong>{formatMoney(invoice.total, invoice.currency)}</strong></td><td>{formatMoney(balance, invoice.currency)}</td><td><StatusBadge status={effectiveInvoiceStatus(invoice)} /></td><td><Link className="mini-button text-link" to={`/invoices/${invoice.id}`}>ดูรายละเอียด</Link></td></tr>
          })}</tbody></table></div>
        )}
        {totalPages > 1 && <div className="pagination"><button className="button button-secondary" type="button" disabled={safePage === 1} onClick={() => setPage((value) => value - 1)}>← ก่อนหน้า</button><span>หน้า {safePage} จาก {totalPages}</span><button className="button button-secondary" type="button" disabled={safePage === totalPages} onClick={() => setPage((value) => value + 1)}>ถัดไป →</button></div>}
      </section>
    </div>
  )
}

export default InvoicesPage
