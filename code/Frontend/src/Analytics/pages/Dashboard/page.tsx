import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { FiActivity, FiBriefcase, FiCheckSquare, FiClock, FiPlus, FiTrendingDown, FiTrendingUp } from 'react-icons/fi'
import { toast } from 'sonner'
import { Button } from '../../../components/ui/button'
import { Card } from '../../../components/ui/card'
import { NativeSelect } from '../../../components/ui/native-select'
import { Progress } from '../../../components/ui/progress'
import PageHeader from '../../../components/PageHeader'
import StatusBadge from '../../../components/StatusBadge'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { useAuth } from '../../../Authentication/useAuthentication'
import { getErrorMessage } from '../../../api/apiError'
import { getDashboard, getDashboardActivity } from '../../../services/dashboard'
import { listTimerProjects } from '../../../services/timerOptions'
import type { DashboardChartPeriod, DashboardData } from '../../../types/dashboard'
import type { ProductivityPoint } from '../../../types/analytics'
import type { TimerWorkspace } from '../../../types/timerWorkspace'
import TimerPanel from '../../../TimeTracking/components/TimerPanel'
import { formatDurationSeconds } from '../../../lib/formatters'
import { activityChartPoints, weeklyChartPoints } from '../../../utils/dashboardChart'
import ProductivityChart from '../../components/ProductivityChart'
import SummaryCard from '../../../components/SummaryCard'

function greetingForHour(hour: number): string {
  if (hour < 12) return 'สวัสดีตอนเช้า'
  if (hour < 18) return 'สวัสดีตอนบ่าย'
  return 'สวัสดีตอนเย็น'
}

function DashboardPage() {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [chartPeriod, setChartPeriod] = useState<DashboardChartPeriod>('WEEK')
  const [rangePoints, setRangePoints] = useState<ProductivityPoint[]>([])
  const [chartLoading, setChartLoading] = useState(false)
  const [chartError, setChartError] = useState('')
  const [chartRequestKey, setChartRequestKey] = useState(0)
  const [timerProjects, setTimerProjects] = useState<TimerWorkspace['data']['projects'] | null>(null)
  const timerProjectsLoading = useRef(false)

  const refresh = useCallback(async () => {
    try {
      setDashboard(await getDashboard())
      setError('')
    } catch (reason: unknown) {
      setError(getErrorMessage(reason, 'ไม่สามารถโหลดข้อมูลภาพรวมได้'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void refresh() }, [refresh])

  const loadTimerProjects = useCallback(() => {
    if (timerProjects || timerProjectsLoading.current) return
    timerProjectsLoading.current = true
    void listTimerProjects()
      .then((projects) => setTimerProjects(projects))
      .catch((reason: unknown) => toast.error(getErrorMessage(reason, 'ไม่สามารถโหลดโปรเจกต์สำหรับจับเวลาได้')))
      .finally(() => { timerProjectsLoading.current = false })
  }, [timerProjects])

  useEffect(() => {
    if (dashboard && dashboard.activeProjects.length === 0) loadTimerProjects()
  }, [dashboard, loadTimerProjects])

  useEffect(() => {
    if (!dashboard || chartPeriod === 'WEEK') return
    let active = true
    const period = chartPeriod
    setChartLoading(true)
    setChartError('')
    setRangePoints([])
    void getDashboardActivity(period)
      .then((activity) => {
        if (active) setRangePoints(activityChartPoints(period, activity.points))
      })
      .catch((reason: unknown) => {
        if (active) setChartError(getErrorMessage(reason, 'ไม่สามารถโหลดข้อมูลกราฟได้'))
      })
      .finally(() => {
        if (active) setChartLoading(false)
      })
    return () => { active = false }
  }, [chartPeriod, chartRequestKey, dashboard])

  const timerWorkspace = useMemo<TimerWorkspace>(() => ({
    data: {
      projects: timerProjects ?? (dashboard?.activeProjects ?? []).map((project) => ({
        id: project.id,
        name: project.name,
        color: project.color || '#4F6BFF',
        status: project.status,
      })),
      tasks: [],
      time_entries: [],
    },
    refresh,
  }), [dashboard?.activeProjects, refresh, timerProjects])

  if (loading) return <LoadingState label="กำลังสรุปภาพรวม..." />
  if (error || !dashboard) return <ErrorState message={error || 'ไม่สามารถโหลดข้อมูลภาพรวมได้'} onRetry={refresh} />

  const now = new Date()
  const { summary } = dashboard
  const weekTrend = summary.weekTrendPercent
  const chartData = chartPeriod === 'WEEK' ? weeklyChartPoints(dashboard.dailyWork) : rangePoints

  return (
    <div className="page-view dashboard-page">
      <PageHeader
        eyebrow={new Intl.DateTimeFormat('th-TH', { dateStyle: 'full' }).format(now)}
        title={`${greetingForHour(now.getHours())}, ${user?.user_metadata.full_name || 'ฟรีแลนซ์'}`}
        description="ติดตามเวลาทำงาน โปรเจกต์ และงานที่ต้องทำต่อได้จากที่เดียว"
        actions={<Button asChild variant="default"><Link className="button button-primary" to="/projects"><FiPlus aria-hidden="true" /> เพิ่มโปรเจกต์</Link></Button>}
      />

      <section className="summary-grid dashboard-summary-grid" aria-label="สรุปการทำงาน">
        <SummaryCard label="ชั่วโมงที่บันทึกสัปดาห์นี้" icon={<FiClock aria-hidden="true" />} value={formatDurationSeconds(summary.weekTrackedSeconds)} accent="blue" compact
          foot={weekTrend === null ? 'ยังไม่มีข้อมูลสัปดาห์ก่อน' : <><span className={weekTrend >= 0 ? 'trend-positive' : 'negative-money'}>{weekTrend >= 0 ? <FiTrendingUp aria-hidden="true" /> : <FiTrendingDown aria-hidden="true" />} {Math.abs(weekTrend)}%</span> เทียบสัปดาห์ก่อน</>} />
        <SummaryCard label="การใช้ชั่วโมงเป้าหมาย" icon={<FiActivity aria-hidden="true" />} value={Math.round(summary.targetUsagePercent ?? 0)} unit="%" accent="green"
          foot={summary.activeProjectTargetSeconds ? `${formatDurationSeconds(summary.activeProjectTrackedSeconds)} จากเป้าหมาย ${formatDurationSeconds(summary.activeProjectTargetSeconds)}` : 'ยังไม่ได้กำหนดชั่วโมงเป้าหมาย'} />
        <SummaryCard label="โปรเจกต์ที่กำลังทำ" icon={<FiBriefcase aria-hidden="true" />} value={summary.activeProjectCount} unit="โปรเจกต์" accent="violet" foot="สถานะกำลังดำเนินการ" />
        <SummaryCard label="งานที่เสร็จแล้ว" icon={<FiCheckSquare aria-hidden="true" />} value={`${summary.completedTaskCount}/${summary.totalTaskCount}`} accent="orange" compact
          foot={`${Math.round(summary.completedTaskPercent)}% ของงานทั้งหมด`} />
      </section>

      <section className="dashboard-grid dashboard-main-grid">
        <Card asChild><section className="panel chart-panel dashboard-activity-panel">
          <div className="panel-heading">
            <div><h2>ภาพรวมกิจกรรม</h2><p>ชั่วโมงทำงานรวม{chartPeriod === 'WEEK' ? 'ใน 7 วันล่าสุด' : chartPeriod === 'MONTH' ? 'ในเดือนนี้' : 'ในปีนี้'}</p></div>
            <NativeSelect
              aria-label="เลือกช่วงเวลาของกราฟ"
              className="w-36"
              value={chartPeriod}
              onChange={(event) => {
                const value = event.target.value
                if (value === 'WEEK' || value === 'MONTH' || value === 'YEAR') setChartPeriod(value)
              }}
            >
              <option value="WEEK">สัปดาห์</option>
              <option value="MONTH">เดือน</option>
              <option value="YEAR">ปี</option>
            </NativeSelect>
          </div>
          {chartLoading && chartPeriod !== 'WEEK' ? <p className="inline-empty">กำลังโหลดข้อมูลกราฟ...</p>
            : chartError && chartPeriod !== 'WEEK' ? <div className="inline-empty"><p>{chartError}</p><Button variant="outline" onClick={() => setChartRequestKey((key) => key + 1)}>ลองอีกครั้ง</Button></div>
              : <ProductivityChart data={chartData} />}
        </section></Card>
        <TimerPanel workspace={timerWorkspace} onProjectOptionsOpen={loadTimerProjects} />
      </section>

      <section className="dashboard-grid dashboard-work-grid">
        <Card asChild><section className="panel">
          <div className="panel-heading"><div><h2>โปรเจกต์ที่กำลังทำ</h2><p>ติดตามความคืบหน้าจากงานที่เสร็จแล้ว</p></div>
            <Button asChild variant="ghost"><Link className="mini-button text-link" to="/projects">ดูทั้งหมด</Link></Button></div>
          <div className="dashboard-projects">
            {dashboard.activeProjects.length ? dashboard.activeProjects.map((project) => (
              <Link to={`/projects/${project.id}`} key={project.id}>
                <span className="project-dot" style={{ background: project.color || '#4F6BFF' }} />
                <span><strong>{project.name}</strong><small>{project.clientName || 'ไม่ระบุลูกค้า'}</small>
                  <Progress className="progress-track" value={project.taskProgressPercent} indicatorColor={project.color || '#4F6BFF'} /></span>
                <span className="dashboard-project-meta"><StatusBadge status={project.status} />
                  <small>{Math.round(project.taskProgressPercent)}% · {project.completedTaskCount}/{project.totalTaskCount} งาน</small></span>
              </Link>
            )) : <p className="inline-empty">ยังไม่มีโปรเจกต์ที่กำลังทำ</p>}
          </div>
        </section></Card>

        <Card asChild><section className="panel">
          <div className="panel-heading"><div><h2>งานที่ยังไม่เสร็จ</h2><p>งานล่าสุดที่รอดำเนินการ</p></div>
            <Button asChild variant="ghost"><Link className="mini-button text-link" to="/projects">ดูโปรเจกต์ทั้งหมด</Link></Button></div>
          <div className="dashboard-tasks">
            {dashboard.openTasks.length ? dashboard.openTasks.map((task) => (
              <Link to={`/projects/${task.projectId}`} key={task.id}>
                <span className="task-state-dot" />
                <span><strong>{task.name}</strong><small>{task.projectName || 'ไม่ระบุโปรเจกต์'}</small></span>
                <span><StatusBadge status={task.status === 'OPEN' ? 'TODO' : task.status === 'COMPLETED' ? 'DONE' : 'IN_PROGRESS'} /></span>
              </Link>
            )) : <p className="inline-empty">ไม่มีงานค้างอยู่</p>}
          </div>
        </section></Card>
      </section>
    </div>
  )
}

export default DashboardPage
