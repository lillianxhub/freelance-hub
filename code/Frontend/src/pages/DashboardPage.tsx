import { Link } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import PageHeader from '../components/PageHeader'
import StatusBadge from '../components/StatusBadge'
import { ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import type { TimeEntry } from '../types/domain'
import { summarizeTime } from '../utils/analytics'
import { calculateTimeValue, formatDate, formatDuration, formatMoney } from '../utils/formatters'
import { effectiveInvoiceStatus, invoiceBalance } from '../utils/invoices'

function localDateKey(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function dateAtOffset(date: Date, offset: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + offset)
  return result
}

function minutesInRange(entries: TimeEntry[], from: string, to: string): number {
  return entries.filter((entry) => { const key = entry.started_at.slice(0, 10); return key >= from && key <= to }).reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
}

function trend(current: number, previous: number): number {
  if (!previous) return current ? 100 : 0
  return ((current - previous) / previous) * 100
}

function DashboardPage() {
  const { data, loading, error, refresh } = useWorkspace()

  if (loading) return <LoadingState label="กำลังสรุป Dashboard..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const now = new Date()
  const today = localDateKey(now)
  const weekday = now.getDay() || 7
  const weekStart = localDateKey(dateAtOffset(now, 1 - weekday))
  const previousWeekStart = localDateKey(dateAtOffset(now, 1 - weekday - 7))
  const previousWeekEnd = localDateKey(dateAtOffset(now, -weekday))
  const monthStart = `${today.slice(0, 8)}01`
  const previousMonthEndDate = new Date(now.getFullYear(), now.getMonth(), 0)
  const previousMonthStart = `${previousMonthEndDate.getFullYear()}-${String(previousMonthEndDate.getMonth() + 1).padStart(2, '0')}-01`
  const previousMonthEnd = localDateKey(previousMonthEndDate)
  const finishedTime = data.time_entries.filter((entry) => entry.ended_at)
  const todayMinutes = minutesInRange(finishedTime, today, today)
  const yesterday = localDateKey(dateAtOffset(now, -1))
  const yesterdayMinutes = minutesInRange(finishedTime, yesterday, yesterday)
  const weekMinutes = minutesInRange(finishedTime, weekStart, today)
  const previousWeekMinutes = minutesInRange(finishedTime, previousWeekStart, previousWeekEnd)
  const monthMinutes = minutesInRange(finishedTime, monthStart, today)
  const previousMonthMinutes = minutesInRange(finishedTime, previousMonthStart, previousMonthEnd)
  const monthEntries = finishedTime.filter((entry) => entry.started_at.slice(0, 10) >= monthStart && entry.started_at.slice(0, 10) <= today)
  const monthSummary = summarizeTime(monthEntries)
  const profile = data.profiles[0]
  const currency = profile?.currency || 'THB'

  const chartData = Array.from({ length: 7 }, (_, index) => {
    const date = dateAtOffset(now, index - 6)
    const key = localDateKey(date)
    const entries = finishedTime.filter((entry) => entry.started_at.slice(0, 10) === key)
    return {
      key,
      day: new Intl.DateTimeFormat('th-TH', { weekday: 'short' }).format(date),
      billable: Number((entries.filter((entry) => entry.billable).reduce((sum, entry) => sum + Number(entry.duration_minutes), 0) / 60).toFixed(2)),
      total: Number((entries.reduce((sum, entry) => sum + Number(entry.duration_minutes), 0) / 60).toFixed(2)),
    }
  })

  const currentInvoices = data.invoices.filter((invoice) => invoice.currency === currency)
  const unbilled = finishedTime.filter((entry) => entry.currency === currency && entry.billable && !entry.invoice_id).reduce((sum, entry) => sum + calculateTimeValue(entry), 0)
  const invoiced = currentInvoices.filter((invoice) => ['ISSUED', 'OVERDUE'].includes(effectiveInvoiceStatus(invoice))).reduce((sum, invoice) => sum + invoiceBalance(invoice), 0)
  const paid = data.payments.filter((payment) => payment.currency === currency).reduce((sum, payment) => sum + Number(payment.amount), 0)
  const overdue = currentInvoices.filter((invoice) => effectiveInvoiceStatus(invoice) === 'OVERDUE').reduce((sum, invoice) => sum + invoiceBalance(invoice), 0)
  const activeProjects = data.projects.filter((project) => project.status === 'ACTIVE')
  const projectProgress = activeProjects.map((project) => {
    const entries = finishedTime.filter((entry) => entry.project_id === project.id)
    const tasks = data.tasks.filter((task) => task.project_id === project.id)
    const minutes = entries.reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
    const percent = project.budget_hours ? (minutes / 60 / Number(project.budget_hours)) * 100 : tasks.length ? (tasks.filter((task) => task.status === 'DONE').length / tasks.length) * 100 : 0
    return { ...project, minutes, percent }
  }).sort((a, b) => b.percent - a.percent)
  const upcomingTasks = data.tasks.filter((task) => task.status !== 'DONE').sort((a, b) => (a.due_date || '9999').localeCompare(b.due_date || '9999')).slice(0, 5)
  const recentEntries = [...finishedTime].sort((a, b) => Date.parse(b.started_at) - Date.parse(a.started_at)).slice(0, 4)

  return (
    <div className="page-view">
      <PageHeader eyebrow="Workspace / Overview" title={`สวัสดี, ${(profile?.full_name || 'Freelancer').split(' ')[0]}`} description="ภาพรวมเวลา งาน และกระแสเงินสดของคุณในวันนี้" actions={<><Link className="button button-secondary" to="/reports">ดูรายงาน</Link><Link className="button button-primary" to="/time-tracker">▶ เริ่มจับเวลา</Link></>} />
      <div className="summary-grid">
        <article className="metric-card accent-blue"><div className="metric-top"><span>Tracked today</span><span className="metric-icon">◷</span></div><div className="metric-value">{(todayMinutes / 60).toFixed(1)}<span className="metric-unit">ชม.</span></div><div className="metric-foot"><span className={trend(todayMinutes, yesterdayMinutes) >= 0 ? 'trend-positive' : 'negative-money'}>{trend(todayMinutes, yesterdayMinutes) >= 0 ? '↗' : '↘'} {Math.abs(trend(todayMinutes, yesterdayMinutes)).toFixed(0)}%</span> เทียบเมื่อวาน</div></article>
        <article className="metric-card accent-green"><div className="metric-top"><span>This week</span><span className="metric-icon">▦</span></div><div className="metric-value">{(weekMinutes / 60).toFixed(1)}<span className="metric-unit">ชม.</span></div><div className="metric-foot"><span className={trend(weekMinutes, previousWeekMinutes) >= 0 ? 'trend-positive' : 'negative-money'}>{trend(weekMinutes, previousWeekMinutes) >= 0 ? '↗' : '↘'} {Math.abs(trend(weekMinutes, previousWeekMinutes)).toFixed(0)}%</span> เทียบสัปดาห์ก่อน</div></article>
        <article className="metric-card accent-violet"><div className="metric-top"><span>This month</span><span className="metric-icon">◒</span></div><div className="metric-value">{(monthMinutes / 60).toFixed(1)}<span className="metric-unit">ชม.</span></div><div className="metric-foot"><span className={trend(monthMinutes, previousMonthMinutes) >= 0 ? 'trend-positive' : 'negative-money'}>{trend(monthMinutes, previousMonthMinutes) >= 0 ? '↗' : '↘'} {Math.abs(trend(monthMinutes, previousMonthMinutes)).toFixed(0)}%</span> เทียบเดือนก่อน</div></article>
        <article className="metric-card accent-orange"><div className="metric-top"><span>Billable utilization</span><span className="metric-icon">%</span></div><div className="metric-value">{monthSummary.utilization.toFixed(0)}<span className="metric-unit">%</span></div><div className="metric-foot">{formatDuration(monthSummary.billableMinutes)} จาก {formatDuration(monthSummary.trackedMinutes)}</div></article>
      </div>

      <div className="dashboard-grid">
        <section className="panel chart-panel"><div className="panel-heading"><div><h2>Activity overview</h2><p>ชั่วโมงที่บันทึกใน 7 วันล่าสุด</p></div><Link className="mini-button text-link" to="/reports">ดูทั้งหมด</Link></div><ResponsiveContainer width="100%" height={255}><AreaChart data={chartData}><defs><linearGradient id="totalHours" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#3867f4" stopOpacity={0.28} /><stop offset="95%" stopColor="#3867f4" stopOpacity={0.02} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8ebf1" /><XAxis dataKey="day" tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><Tooltip /><Area type="monotone" dataKey="total" name="เวลารวม" stroke="#3867f4" strokeWidth={2} fill="url(#totalHours)" /><Area type="monotone" dataKey="billable" name="Billable" stroke="#20a176" strokeWidth={2} fill="transparent" /></AreaChart></ResponsiveContainer></section>
        <section className="panel cashflow-card"><div className="panel-heading"><div><h2>Revenue snapshot</h2><p>สรุปยอดในสกุลเงิน {currency}</p></div><Link className="mini-button text-link" to="/finances">จัดการ</Link></div><div className="cashflow-total"><span>รายได้ที่รับแล้ว</span><strong>{formatMoney(paid, currency)}</strong></div><div className="summary-list"><div><span><i className="legend-dot blue" />Unbilled</span><strong>{formatMoney(unbilled, currency)}</strong></div><div><span><i className="legend-dot violet" />Invoiced</span><strong>{formatMoney(invoiced, currency)}</strong></div><div><span><i className="legend-dot green" />Paid</span><strong>{formatMoney(paid, currency)}</strong></div><div><span><i className="legend-dot red" />Overdue</span><strong className={overdue ? 'negative-money' : ''}>{formatMoney(overdue, currency)}</strong></div></div></section>
      </div>

      <div className="lower-grid">
        <section className="panel"><div className="panel-heading"><div><h2>Active projects</h2><p>ติดตามความคืบหน้าและการใช้งบประมาณ</p></div><Link className="mini-button text-link" to="/projects">ดูทั้งหมด</Link></div><div className="dashboard-projects">{projectProgress.length ? projectProgress.map((project) => { const client = data.clients.find((item) => item.id === project.client_id); return <Link to={`/projects/${project.id}`} key={project.id}><span className="project-dot" style={{ background: project.color }} /><span><strong>{project.name}</strong><small>{client?.company_name || client?.name}</small><i className="progress-track"><b style={{ width: `${Math.min(100, project.percent)}%`, background: project.percent >= 100 ? '#dc4c64' : project.percent >= 80 ? '#e97834' : project.color }} /></i></span><span><strong>{project.percent.toFixed(0)}%</strong><small>{formatDuration(project.minutes)}</small></span>{project.percent >= 80 && <em>{project.percent >= 100 ? 'เกินงบ' : 'ใกล้เต็มงบ'}</em>}</Link> }) : <p className="inline-empty">ยังไม่มีโปรเจกต์ที่กำลังทำ</p>}</div></section>
        <section className="panel"><div className="panel-heading"><div><h2>Upcoming tasks</h2><p>งานที่ยังไม่เสร็จ เรียงตามกำหนดส่ง</p></div></div><div className="dashboard-tasks">{upcomingTasks.length ? upcomingTasks.map((task) => { const project = data.projects.find((item) => item.id === task.project_id); return <Link to={`/projects/${project?.id}`} key={task.id}><span className="task-state-dot" /><span><strong>{task.name}</strong><small>{project?.name}</small></span><span><StatusBadge status={task.status} /><small>{formatDate(task.due_date)}</small></span></Link> }) : <p className="inline-empty">ไม่มีงานค้างอยู่</p>}</div></section>
      </div>

      <section className="panel recent-time-panel"><div className="panel-heading"><div><h2>Recent time entries</h2><p>กิจกรรมล่าสุดใน Workspace</p></div><Link className="mini-button text-link" to="/time-tracker">ดู Time tracker</Link></div><div className="list-stack">{recentEntries.map((entry) => { const project = data.projects.find((item) => item.id === entry.project_id); return <div className="list-item" key={entry.id}><span className="color-dot" style={{ '--dot-color': project?.color }} /><span><strong>{entry.description || project?.name}</strong><small>{project?.name} · {formatDate(entry.started_at)}</small></span><span>{formatDuration(entry.duration_minutes)}</span><strong>{entry.billable ? formatMoney(calculateTimeValue(entry), entry.currency) : 'Non-billable'}</strong></div> })}</div></section>
    </div>
  )
}

export default DashboardPage
