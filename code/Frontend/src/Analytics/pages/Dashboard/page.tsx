import { Link } from 'react-router-dom'
import { FiActivity, FiBriefcase, FiCheckSquare, FiClock, FiPlus, FiTrendingDown, FiTrendingUp } from 'react-icons/fi'
import PageHeader from '../../../components/PageHeader'
import StatusBadge from '../../../components/StatusBadge'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { useAnalytics } from '../../useAnalytics'
import ProductivityChart from '../../components/ProductivityChart'
import SummaryCard from '../../components/SummaryCard'
import TimerPanel from '../../../TimeTracking/components/TimerPanel'
import { formatDate, formatDuration } from '../../../utils/formatters'
import type { TimeEntry } from '../../../types/timeTracking'

function localDateKey(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}

function dateAtOffset(date: Date, offset: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + offset)
  return result
}

function totalMinutes(entries: readonly TimeEntry[], from: string, to: string): number {
  return entries
    .filter((entry) => {
      const date = entry.started_at.slice(0, 10)
      return date >= from && date <= to
    })
    .reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
}

function percentageChange(current: number, previous: number): number {
  if (!previous) return current ? 100 : 0
  return Math.round(((current - previous) / previous) * 100)
}

function greetingForHour(hour: number): string {
  if (hour < 12) return 'สวัสดีตอนเช้า'
  if (hour < 18) return 'สวัสดีตอนบ่าย'
  return 'สวัสดีตอนเย็น'
}

function DashboardPage() {
  const analytics = useAnalytics()
  const { data, loading, error, refresh } = analytics

  if (loading) return <LoadingState label="กำลังสรุปภาพรวม..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const now = new Date()
  const today = localDateKey(now)
  const weekday = now.getDay() || 7
  const weekStart = localDateKey(dateAtOffset(now, 1 - weekday))
  const previousWeekStart = localDateKey(dateAtOffset(now, 1 - weekday - 7))
  const previousWeekEnd = localDateKey(dateAtOffset(now, -weekday))
  const completedEntries = data.time_entries.filter((entry) => entry.ended_at)
  const weekMinutes = totalMinutes(completedEntries, weekStart, today)
  const previousWeekMinutes = totalMinutes(completedEntries, previousWeekStart, previousWeekEnd)
  const weekTrend = percentageChange(weekMinutes, previousWeekMinutes)
  const activeProjects = data.projects.filter((project) => project.status === 'ACTIVE')
  const totalTasks = data.tasks.length
  const completedTasks = data.tasks.filter((task) => task.status === 'DONE').length
  const completedPercent = totalTasks ? Math.round((completedTasks / totalTasks) * 100) : 0
  const targetHours = activeProjects.reduce((sum, project) => sum + Number(project.budget_hours || 0), 0)
  const activeProjectIds = new Set(activeProjects.map((project) => project.id))
  const activeMinutes = completedEntries
    .filter((entry) => activeProjectIds.has(entry.project_id))
    .reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
  const utilization = targetHours ? Math.round((activeMinutes / 60 / targetHours) * 100) : 0

  const chartData = Array.from({ length: 7 }, (_, index) => {
    const date = dateAtOffset(now, index - 6)
    const key = localDateKey(date)
    const minutes = completedEntries
      .filter((entry) => entry.started_at.slice(0, 10) === key)
      .reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)

    return {
      key,
      day: new Intl.DateTimeFormat('th-TH', { weekday: 'short' }).format(date),
      total: Number((minutes / 60).toFixed(2)),
    }
  })

  const visibleProjects = activeProjects
    .map((project) => {
      const tasks = data.tasks.filter((task) => task.project_id === project.id)
      const done = tasks.filter((task) => task.status === 'DONE').length
      return {
        project,
        client: data.clients.find((client) => client.id === project.client_id),
        totalTasks: tasks.length,
        completedTasks: done,
        progress: tasks.length ? Math.round((done / tasks.length) * 100) : 0,
      }
    })
    .sort((first, second) => second.progress - first.progress)
    .slice(0, 5)

  const upcomingTasks = data.tasks
    .filter((task) => task.status !== 'DONE')
    .sort((first, second) => {
      if (!first.due_date) return 1
      if (!second.due_date) return -1
      return first.due_date.localeCompare(second.due_date)
    })
    .slice(0, 5)

  const profile = data.profiles[0]
  const name = profile?.display_name || profile?.first_name || profile?.full_name || 'ฟรีแลนซ์'

  return (
    <div className="page-view dashboard-page">
      <PageHeader
        eyebrow={new Intl.DateTimeFormat('th-TH', { dateStyle: 'full' }).format(now)}
        title={`${greetingForHour(now.getHours())}, ${name}`}
        description="ติดตามเวลาทำงาน โปรเจกต์ และงานที่ต้องทำต่อได้จากที่เดียว"
        actions={
          <Link className="button button-primary" to="/projects">
            <FiPlus aria-hidden="true" /> เพิ่มโปรเจกต์
          </Link>
        }
      />

      <section className="summary-grid dashboard-summary-grid" aria-label="สรุปการทำงาน">
        <SummaryCard
          label="ชั่วโมงที่บันทึกสัปดาห์นี้"
          icon={<FiClock aria-hidden="true" />}
          value={formatDuration(weekMinutes)}
          accent="blue"
          foot={<><span className={weekTrend >= 0 ? 'trend-positive' : 'negative-money'}>{weekTrend >= 0 ? <FiTrendingUp aria-hidden="true" /> : <FiTrendingDown aria-hidden="true" />} {Math.abs(weekTrend)}%</span> เทียบสัปดาห์ก่อน</>}
          compact
        />
        <SummaryCard
          label="การใช้ชั่วโมงเป้าหมาย"
          icon={<FiActivity aria-hidden="true" />}
          value={utilization}
          unit="%"
          accent="green"
          foot={targetHours ? `${formatDuration(activeMinutes)} จากเป้าหมาย ${targetHours} ชม.` : 'ยังไม่ได้กำหนดชั่วโมงเป้าหมาย'}
        />
        <SummaryCard
          label="โปรเจกต์ที่กำลังทำ"
          icon={<FiBriefcase aria-hidden="true" />}
          value={activeProjects.length}
          unit="โปรเจกต์"
          accent="violet"
          foot="สถานะกำลังดำเนินการ"
        />
        <SummaryCard
          label="งานที่เสร็จแล้ว"
          icon={<FiCheckSquare aria-hidden="true" />}
          value={`${completedTasks}/${totalTasks}`}
          accent="orange"
          foot={`${completedPercent}% ของงานทั้งหมด`}
          compact
        />
      </section>

      <section className="dashboard-grid dashboard-main-grid">
        <section className="panel chart-panel dashboard-activity-panel">
          <div className="panel-heading">
            <div>
              <h2>ภาพรวมกิจกรรม</h2>
              <p>ชั่วโมงทำงานรวมใน 7 วันล่าสุด</p>
            </div>
          </div>
          <ProductivityChart data={chartData} />
        </section>
        <TimerPanel workspace={analytics} />
      </section>

      <section className="dashboard-grid dashboard-work-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>โปรเจกต์ที่กำลังทำ</h2>
              <p>ติดตามความคืบหน้าจากงานที่เสร็จแล้ว</p>
            </div>
            <Link className="mini-button text-link" to="/projects">ดูทั้งหมด</Link>
          </div>
          <div className="dashboard-projects">
            {visibleProjects.length ? visibleProjects.map(({ project, client, totalTasks: projectTasks, completedTasks: projectDone, progress }) => (
              <Link to={`/projects/${project.id}`} key={project.id}>
                <span className="project-dot" style={{ background: project.color }} />
                <span>
                  <strong>{project.name}</strong>
                  <small>{client?.company_name || client?.name || 'ไม่ระบุลูกค้า'}</small>
                  <i className="progress-track"><b style={{ width: `${progress}%`, background: project.color }} /></i>
                </span>
                <span className="dashboard-project-meta">
                  <StatusBadge status={project.status} />
                  <small>{progress}% · {projectDone}/{projectTasks} งาน</small>
                </span>
              </Link>
            )) : <p className="inline-empty">ยังไม่มีโปรเจกต์ที่กำลังทำ</p>}
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>งานที่ใกล้ถึงกำหนด</h2>
              <p>งานที่ยังไม่เสร็จ เรียงตามกำหนดส่ง</p>
            </div>
            <Link className="mini-button text-link" to="/projects">ดูโปรเจกต์ทั้งหมด</Link>
          </div>
          <div className="dashboard-tasks">
            {upcomingTasks.length ? upcomingTasks.map((task) => {
              const project = data.projects.find((item) => item.id === task.project_id)
              return (
                <Link to={`/projects/${task.project_id}`} key={task.id}>
                  <span className="task-state-dot" />
                  <span>
                    <strong>{task.name}</strong>
                    <small>{project?.name || 'ไม่ระบุโปรเจกต์'}</small>
                  </span>
                  <span>
                    <StatusBadge status={task.status} />
                    <small>{task.due_date ? formatDate(task.due_date) : 'ยังไม่กำหนดส่ง'}</small>
                  </span>
                </Link>
              )
            }) : <p className="inline-empty">ไม่มีงานค้างอยู่</p>}
          </div>
        </section>
      </section>
    </div>
  )
}

export default DashboardPage
