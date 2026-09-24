import { Link, useParams } from 'react-router-dom'
import PageHeader from '../components/PageHeader'
import StatusBadge from '../components/StatusBadge'
import { ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { calculateTimeValue, formatDate, formatDuration, formatMoney } from '../utils/formatters'

function ClientDetailPage() {
  const { clientId } = useParams()
  const { data, loading, error, refresh } = useWorkspace()

  if (loading) return <LoadingState label="กำลังโหลดข้อมูลลูกค้า..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const client = data.clients.find((item) => item.id === clientId)
  if (!client) return <ErrorState message="ไม่พบลูกค้าที่ต้องการ" />

  const projects = data.projects.filter((project) => project.client_id === client.id)
  const projectIds = new Set(projects.map((project) => project.id))
  const entries = data.time_entries.filter((entry) => projectIds.has(entry.project_id))
  const invoices = data.invoices.filter((invoice) => invoice.client_id === client.id)
  const totalMinutes = entries.reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
  const revenue = entries.reduce((sum, entry) => sum + calculateTimeValue(entry), 0)
  const displayName = client.company_name || client.name

  return (
    <div className="page-view">
      <Link className="back-link" to="/clients">← กลับไปหน้าลูกค้า</Link>
      <PageHeader eyebrow="Workspace / Clients" title={displayName} description={`${client.name || 'ไม่ระบุผู้ติดต่อ'} · ${client.email || 'ไม่มีอีเมล'}`} actions={<StatusBadge status={client.status} />} />

      <div className="summary-grid">
        <article className="metric-card accent-blue"><div className="metric-top"><span>โปรเจกต์</span><span className="metric-icon">▦</span></div><div className="metric-value">{projects.length}</div><div className="metric-foot">ทั้งหมดของลูกค้ารายนี้</div></article>
        <article className="metric-card accent-green"><div className="metric-top"><span>เวลาที่บันทึก</span><span className="metric-icon">◷</span></div><div className="metric-value metric-compact">{formatDuration(totalMinutes)}</div><div className="metric-foot">รวมทุกโปรเจกต์</div></article>
        <article className="metric-card accent-violet"><div className="metric-top"><span>รายได้เกิดขึ้น</span><span className="metric-icon">฿</span></div><div className="metric-value metric-compact">{formatMoney(revenue)}</div><div className="metric-foot">จากเวลา billable</div></article>
        <article className="metric-card accent-orange"><div className="metric-top"><span>Invoice</span><span className="metric-icon">▤</span></div><div className="metric-value">{invoices.length}</div><div className="metric-foot">ยอดรวม {formatMoney(invoices.reduce((sum, invoice) => sum + invoice.total, 0))}</div></article>
      </div>

      <div className="detail-grid">
        <div className="section-stack">
          <section className="panel">
            <div className="panel-heading"><div><h2>Projects</h2><p>โปรเจกต์ทั้งหมดของลูกค้ารายนี้</p></div></div>
            <div className="list-stack">
              {projects.map((project) => {
                const minutes = entries.filter((entry) => entry.project_id === project.id).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
                return <Link className="list-item" key={project.id} to={`/projects/${project.id}`}><span className="color-dot" style={{ '--dot-color': project.color }} /><span><strong>{project.name}</strong><small>{formatDuration(minutes)} · {project.billing_type === 'HOURLY' ? 'รายชั่วโมง' : 'เหมาจ่าย'}</small></span><StatusBadge status={project.status} /><span>→</span></Link>
              })}
              {projects.length === 0 && <p className="inline-empty">ยังไม่มีโปรเจกต์</p>}
            </div>
          </section>

          <section className="panel">
            <div className="panel-heading"><div><h2>Invoices</h2><p>ประวัติการเรียกเก็บเงิน</p></div></div>
            <div className="table-wrap"><table className="data-table"><thead><tr><th>เลขที่</th><th>วันที่ออก</th><th>ครบกำหนด</th><th>ยอดรวม</th><th>สถานะ</th></tr></thead><tbody>
              {invoices.map((invoice) => <tr key={invoice.id}><td><Link to={`/invoices/${invoice.id}`}><strong>{invoice.invoice_number}</strong></Link></td><td>{formatDate(invoice.issue_date)}</td><td>{formatDate(invoice.due_date)}</td><td>{formatMoney(invoice.total, invoice.currency)}</td><td><StatusBadge status={invoice.status} /></td></tr>)}
              {invoices.length === 0 && <tr><td colSpan="5">ยังไม่มี Invoice</td></tr>}
            </tbody></table></div>
          </section>
        </div>

        <aside className="panel">
          <div className="panel-heading"><div><h2>ข้อมูลลูกค้า</h2><p>ข้อมูลสำหรับติดต่อและออก Invoice</p></div></div>
          <div className="detail-list">
            <div className="detail-item"><span>ผู้ติดต่อ</span><strong>{client.name || '—'}</strong></div>
            <div className="detail-item"><span>อีเมล</span><strong>{client.email || '—'}</strong></div>
            <div className="detail-item"><span>โทรศัพท์</span><strong>{client.phone || '—'}</strong></div>
            <div className="detail-item"><span>เลขผู้เสียภาษี</span><strong>{client.tax_id || '—'}</strong></div>
            <div className="detail-item"><span>ที่อยู่</span><p>{client.address || '—'}</p></div>
            <div className="detail-item"><span>หมายเหตุ</span><p>{client.notes || '—'}</p></div>
          </div>
        </aside>
      </div>
    </div>
  )
}

export default ClientDetailPage
