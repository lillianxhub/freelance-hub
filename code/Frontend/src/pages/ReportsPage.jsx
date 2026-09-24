import { useMemo, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import PageHeader from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { downloadCsv, groupTimeBy, inDateRange, summarizeTime } from '../utils/analytics'
import { formatDuration, formatMoney } from '../utils/formatters'
import { effectiveInvoiceStatus, invoiceBalance } from '../utils/invoices'

const reportNow = new Date()
const reportTo = new Date(reportNow.getTime() - reportNow.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const reportFrom = `${reportTo.slice(0, 8)}01`
const chartColors = ['#3867f4', '#f07b62', '#9b7bea', '#21a179', '#e6a23c', '#59708f']

function ReportsPage() {
  const { data, loading, error, refresh } = useWorkspace()
  const [range, setRange] = useState({ from: reportFrom, to: reportTo })
  const [currency, setCurrency] = useState('THB')

  const currencies = useMemo(() => [...new Set([
    ...(data?.time_entries || []).map((entry) => entry.currency),
    ...(data?.invoices || []).map((invoice) => invoice.currency),
    ...(data?.finance_entries || []).map((entry) => entry.currency),
  ].filter(Boolean))], [data])

  const filteredTime = useMemo(() => (data?.time_entries || []).filter((entry) => entry.ended_at && entry.currency === currency && inDateRange(entry.started_at, range.from, range.to)), [currency, data?.time_entries, range])
  const filteredInvoices = useMemo(() => (data?.invoices || []).filter((invoice) => invoice.currency === currency && inDateRange(invoice.issue_date, range.from, range.to)), [currency, data?.invoices, range])
  const filteredPayments = useMemo(() => (data?.payments || []).filter((payment) => payment.currency === currency && inDateRange(payment.paid_at, range.from, range.to)), [currency, data?.payments, range])

  if (loading) return <LoadingState label="กำลังประมวลผลรายงาน..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const summary = summarizeTime(filteredTime)
  const projectGroups = groupTimeBy(filteredTime, (entry) => entry.project_id).map((group) => ({ ...group, name: data.projects.find((project) => project.id === group.key)?.name || 'ไม่ทราบโปรเจกต์', hours: Number((group.minutes / 60).toFixed(2)), billableHours: Number((group.billableMinutes / 60).toFixed(2)) }))
  const clientGroups = groupTimeBy(filteredTime, (entry) => data.projects.find((project) => project.id === entry.project_id)?.client_id || 'unknown').map((group) => ({ ...group, name: data.clients.find((client) => client.id === group.key)?.company_name || data.clients.find((client) => client.id === group.key)?.name || 'ไม่ทราบลูกค้า', hours: Number((group.minutes / 60).toFixed(2)) }))
  const daily = [...filteredTime.reduce((map, entry) => {
    const day = entry.started_at.slice(0, 10)
    const current = map.get(day) || { date: day, billable: 0, nonBillable: 0 }
    current[entry.billable ? 'billable' : 'nonBillable'] += Number(entry.duration_minutes) / 60
    map.set(day, current)
    return map
  }, new Map()).values()].sort((a, b) => a.date.localeCompare(b.date)).map((item) => ({ ...item, label: new Intl.DateTimeFormat('th-TH', { day: 'numeric', month: 'short' }).format(new Date(item.date)) }))
  const invoiced = filteredInvoices.filter((invoice) => ['ISSUED', 'OVERDUE'].includes(effectiveInvoiceStatus(invoice, reportTo))).reduce((sum, invoice) => sum + Number(invoice.total), 0)
  const paid = filteredPayments.reduce((sum, payment) => sum + Number(payment.amount), 0)
  const overdue = filteredInvoices.filter((invoice) => effectiveInvoiceStatus(invoice, reportTo) === 'OVERDUE').reduce((sum, invoice) => sum + invoiceBalance(invoice), 0)
  const trackedDays = new Set(filteredTime.map((entry) => entry.started_at.slice(0, 10))).size
  const topProject = projectGroups[0]

  const exportTime = () => downloadCsv(`time-report-${range.from}-${range.to}.csv`, [
    ['Date', 'Client', 'Project', 'Task', 'Description', 'Hours', 'Billable', 'Rate', 'Currency', 'Value', 'Invoice status'],
    ...filteredTime.map((entry) => {
      const project = data.projects.find((item) => item.id === entry.project_id)
      const client = data.clients.find((item) => item.id === project?.client_id)
      const task = data.tasks.find((item) => item.id === entry.task_id)
      return [entry.started_at.slice(0, 10), client?.company_name || client?.name, project?.name, task?.name, entry.description, (Number(entry.duration_minutes) / 60).toFixed(2), entry.billable ? 'Yes' : 'No', entry.rate_snapshot, entry.currency, entry.billable ? (Number(entry.duration_minutes) / 60 * Number(entry.rate_snapshot)).toFixed(2) : '0.00', entry.invoice_id ? 'Invoiced' : 'Unbilled']
    }),
  ])
  const exportRevenue = () => downloadCsv(`revenue-report-${range.from}-${range.to}.csv`, [
    ['Invoice number', 'Client', 'Issue date', 'Due date', 'Status', 'Subtotal', 'Tax', 'Total', 'Paid', 'Balance', 'Currency'],
    ...filteredInvoices.map((invoice) => { const client = data.clients.find((item) => item.id === invoice.client_id); return [invoice.invoice_number, client?.company_name || client?.name, invoice.issue_date, invoice.due_date, invoice.status, invoice.subtotal, invoice.tax_amount, invoice.total, invoice.amount_paid, Number(invoice.total) - Number(invoice.amount_paid || 0), invoice.currency] }),
  ])

  return (
    <div className="page-view">
      <PageHeader eyebrow="Manage / Reports" title="Reports & insights" description="เข้าใจเวลา รายได้ และประสิทธิภาพจากข้อมูลที่บันทึกจริง" actions={<><button className="button button-secondary" type="button" onClick={exportTime}>↓ Time CSV</button><button className="button button-primary" type="button" onClick={exportRevenue}>↓ Revenue CSV</button></>} />
      <section className="panel report-controls"><div className="form-field"><label htmlFor="report-from">จากวันที่</label><input id="report-from" type="date" value={range.from} onChange={(event) => setRange((current) => ({ ...current, from: event.target.value }))} /></div><div className="form-field"><label htmlFor="report-to">ถึงวันที่</label><input id="report-to" type="date" value={range.to} onChange={(event) => setRange((current) => ({ ...current, to: event.target.value }))} /></div><div className="form-field"><label htmlFor="report-currency">สกุลเงิน</label><select id="report-currency" value={currency} onChange={(event) => setCurrency(event.target.value)}>{currencies.map((item) => <option key={item} value={item}>{item}</option>)}</select></div><p>จำนวนเงินจะแยกตามสกุลเงินเพื่อป้องกันการรวมยอดที่ผิดพลาด</p></section>
      <div className="summary-grid">
        <article className="metric-card accent-blue"><div className="metric-top"><span>เวลาทั้งหมด</span><span className="metric-icon">◷</span></div><div className="metric-value">{(summary.trackedMinutes / 60).toFixed(1)}<span className="metric-unit">ชม.</span></div><div className="metric-foot">เฉลี่ย {trackedDays ? (summary.trackedMinutes / 60 / trackedDays).toFixed(1) : '0.0'} ชม./วันที่ทำงาน</div></article>
        <article className="metric-card accent-violet"><div className="metric-top"><span>Billable utilization</span><span className="metric-icon">%</span></div><div className="metric-value">{summary.utilization.toFixed(0)}<span className="metric-unit">%</span></div><div className="metric-foot">{formatDuration(summary.billableMinutes)} ที่เรียกเก็บได้</div></article>
        <article className="metric-card accent-orange"><div className="metric-top"><span>Unbilled</span><span className="metric-icon">◇</span></div><div className="metric-value metric-compact">{formatMoney(summary.unbilledValue, currency)}</div><div className="metric-foot">เวลาที่ยังไม่อยู่ใน Invoice</div></article>
        <article className="metric-card accent-green"><div className="metric-top"><span>Paid</span><span className="metric-icon">✓</span></div><div className="metric-value metric-compact">{formatMoney(paid, currency)}</div><div className="metric-foot">Invoiced {formatMoney(invoiced, currency)} · Overdue {formatMoney(overdue, currency)}</div></article>
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel"><div className="panel-heading"><div><h2>เวลาทำงานรายวัน</h2><p>เปรียบเทียบ billable และ non-billable</p></div></div>{daily.length ? <ResponsiveContainer width="100%" height={270}><BarChart data={daily}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8ebf1" /><XAxis dataKey="label" tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><Tooltip /><Bar dataKey="billable" name="Billable" stackId="time" fill="#3867f4" radius={[5, 5, 0, 0]} /><Bar dataKey="nonBillable" name="Non-billable" stackId="time" fill="#cfd7e7" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer> : <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>}</section>
        <section className="panel chart-panel"><div className="panel-heading"><div><h2>สัดส่วนเวลาตามลูกค้า</h2><p>ชั่วโมงรวมของลูกค้าแต่ละราย</p></div></div>{clientGroups.length ? <><ResponsiveContainer width="100%" height={210}><PieChart><Pie data={clientGroups} dataKey="hours" nameKey="name" innerRadius={52} outerRadius={78} paddingAngle={3}>{clientGroups.map((item, index) => <Cell key={item.key} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer><div className="chart-legend">{clientGroups.map((item, index) => <span key={item.key}><i style={{ background: chartColors[index % chartColors.length] }} />{item.name}<b>{item.hours.toFixed(1)} ชม.</b></span>)}</div></> : <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>}</section>
      </div>
      <div className="lower-grid">
        <section className="panel"><div className="panel-heading"><div><h2>Project performance</h2><p>เวลา มูลค่า และ effective hourly rate</p></div></div><div className="table-wrap"><table className="data-table"><thead><tr><th>โปรเจกต์</th><th>เวลารวม</th><th>Billable</th><th>มูลค่าเกิดขึ้น</th><th>Effective rate</th></tr></thead><tbody>{projectGroups.map((group) => { const project = data.projects.find((item) => item.id === group.key); const effective = project?.billing_type === 'FIXED_PRICE' && group.hours ? Number(project.fixed_price) / group.hours : group.hours ? group.value / group.hours : 0; return <tr key={group.key}><td><div className="table-primary"><span className="color-dot" style={{ '--dot-color': project?.color }} /><strong>{group.name}</strong></div></td><td>{group.hours.toFixed(1)} ชม.</td><td>{group.billableHours.toFixed(1)} ชม.</td><td>{formatMoney(project?.billing_type === 'FIXED_PRICE' ? project.fixed_price : group.value, currency)}</td><td>{formatMoney(effective, currency)}/ชม.</td></tr> })}</tbody></table></div></section>
        <section className="panel insight-card"><div className="panel-heading"><div><h2>Productivity insights</h2><p>ข้อสังเกตจากช่วงวันที่ที่เลือก</p></div></div><div className="insight-list"><div><span>★</span><p><strong>โปรเจกต์ที่ใช้เวลาสูงสุด</strong>{topProject ? `${topProject.name} · ${topProject.hours.toFixed(1)} ชั่วโมง` : 'ยังไม่มีข้อมูล'}</p></div><div><span>◎</span><p><strong>อัตราเวลาที่เรียกเก็บได้</strong>{summary.utilization >= 75 ? 'อยู่ในระดับดี รักษาสมดุลนี้ต่อไป' : 'ลองลดงาน non-billable หรือทบทวนขอบเขตงาน'}</p></div><div><span>฿</span><p><strong>โอกาสเรียกเก็บเงิน</strong>มี {formatMoney(summary.unbilledValue, currency)} ที่พร้อมนำไปสร้าง Invoice</p></div></div></section>
      </div>
    </div>
  )
}

export default ReportsPage
