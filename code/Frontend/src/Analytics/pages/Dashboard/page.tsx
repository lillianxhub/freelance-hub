import { useMemo, useState } from 'react'
import { FiBriefcase, FiCalendar, FiCheckSquare, FiClock, FiTrendingDown, FiTrendingUp } from 'react-icons/fi'
import { toast } from 'sonner'
import { getErrorMessage } from '../../../api/apiError'
import PageHeader from '../../../components/PageHeader'
import SummaryCard from '../../../components/SummaryCard'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/card'
import { DatePicker } from '../../../components/ui/date-picker'
import { Label } from '../../../components/ui/label'
import { formatDuration } from '../../../lib/formatters'
import { stopTimer } from '../../../services/timeTracking'
import type { TimeEntry } from '../../../types/timeTracking'
import { emptyAnalyticsData, loadAnalyticsData } from '../../AnalyticsContext'
import { useAsyncData } from '../../../shared/useAsyncData'
import DashboardProjectsTable from '../../components/DashboardProjectsTable'
import DashboardTasksTable from '../../components/DashboardTasksTable'
import DashboardTimerCard from '../../components/DashboardTimerCard'
import ProductivityChart from '../../components/ProductivityChart'

function localDateKey(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}

function dateAtOffset(date: Date, offset: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + offset)
  return result
}

function totalMinutes(entries: readonly TimeEntry[], from: string, to: string): number {
  return entries.filter((entry) => entry.ended_at && entry.started_at.slice(0, 10) >= from && entry.started_at.slice(0, 10) <= to).reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
}

function percentageChange(current: number, previous: number): number {
  if (!previous) return current ? 100 : 0
  return Math.round(((current - previous) / previous) * 100)
}

function Trend({ value, label }: { value: number; label: string }) {
  const positive = value >= 0
  return <span className="inline-flex items-center gap-1"><span className={positive ? 'inline-flex items-center gap-1 font-semibold text-green' : 'inline-flex items-center gap-1 font-semibold text-destructive'}>{positive ? <FiTrendingUp aria-hidden="true" /> : <FiTrendingDown aria-hidden="true" />}{Math.abs(value)}%</span> {label}</span>
}

function DashboardPage() {
  const { data: source, loading, error, refresh } = useAsyncData(loadAnalyticsData, emptyAnalyticsData)
  const now = useMemo(() => new Date(), [])
  const [dateFrom, setDateFrom] = useState(() => localDateKey(dateAtOffset(now, -6)))
  const [dateTo, setDateTo] = useState(() => localDateKey(now))
  const completedEntries = source.time_entries.filter((entry) => entry.ended_at)
  const today = localDateKey(now)
  const yesterday = localDateKey(dateAtOffset(now, -1))
  const weekday = now.getDay() || 7
  const weekStart = localDateKey(dateAtOffset(now, 1 - weekday))
  const previousWeekStart = localDateKey(dateAtOffset(now, 1 - weekday - 7))
  const previousWeekEnd = localDateKey(dateAtOffset(now, -weekday))
  const todayMinutes = totalMinutes(completedEntries, today, today)
  const yesterdayMinutes = totalMinutes(completedEntries, yesterday, yesterday)
  const weekMinutes = totalMinutes(completedEntries, weekStart, today)
  const previousWeekMinutes = totalMinutes(completedEntries, previousWeekStart, previousWeekEnd)
  const activeProjects = source.projects.filter((project) => project.status === 'ACTIVE')
  const completedTasks = source.tasks.filter((task) => task.status === 'DONE').length

  if (loading) return <LoadingState label="กำลังโหลดข้อมูลภาพรวม..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  async function handleStopTimer() {
    try {
      await stopTimer()
      await refresh()
    } catch (reason: unknown) {
      toast.error(getErrorMessage(reason, 'ไม่สามารถหยุดตัวจับเวลาได้'))
    }
  }

  const chartData = (() => {
    const from = new Date(`${dateFrom}T12:00:00`)
    const to = new Date(`${dateTo}T12:00:00`)
    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || from > to) return []
    const result = []
    for (let date = new Date(from), count = 0; date <= to && count < 366; date = dateAtOffset(date, 1), count += 1) {
      const key = localDateKey(date)
      result.push({
        key,
        day: new Intl.DateTimeFormat('th-TH', { day: 'numeric', month: 'short' }).format(date),
        totalSeconds: Math.round(totalMinutes(completedEntries, key, key) * 60),
      })
    }
    return result
  })()

  return (
    <div className="mx-auto w-full max-w-screen-2xl space-y-6">
      <PageHeader eyebrow={new Intl.DateTimeFormat('th-TH', { dateStyle: 'full' }).format(now)} title="ภาพรวมการทำงาน" description="ติดตามเวลา โปรเจกต์ และงานสำคัญจากที่เดียว" />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="สรุปการทำงาน">
        <SummaryCard label="ชั่วโมงที่บันทึกวันนี้" value={formatDuration(todayMinutes)} icon={<FiClock aria-hidden="true" />} accent="blue" compact foot={<Trend value={percentageChange(todayMinutes, yesterdayMinutes)} label="จากเมื่อวาน" />} />
        <SummaryCard label="ชั่วโมงสัปดาห์นี้" value={formatDuration(weekMinutes)} icon={<FiCalendar aria-hidden="true" />} accent="violet" compact foot={<Trend value={percentageChange(weekMinutes, previousWeekMinutes)} label="จากสัปดาห์ก่อน" />} />
        <SummaryCard label="โปรเจกต์ที่กำลังทำ" value={activeProjects.length} unit="โปรเจกต์" icon={<FiBriefcase aria-hidden="true" />} accent="orange" foot={`จากทั้งหมด ${source.projects.length} โปรเจกต์`} />
        <SummaryCard label="งานที่เสร็จแล้ว" value={`${completedTasks} / ${source.tasks.length}`} icon={<FiCheckSquare aria-hidden="true" />} accent="green" compact foot="จำนวนงานที่ดำเนินการเสร็จแล้ว" />
      </section>
      <section className="grid items-start gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(20rem,1fr)]">
        <Card>
          <CardHeader className="gap-4 sm:grid-cols-[1fr_auto]">
            <div><CardTitle>ชั่วโมงทำงาน</CardTitle><CardDescription>เลือกช่วงวันที่เพื่อดูชั่วโมงที่บันทึก</CardDescription></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2"><Label htmlFor="dashboard-date-from">ตั้งแต่วันที่</Label><DatePicker id="dashboard-date-from" value={dateFrom} onChange={setDateFrom} className="min-w-36" /></div>
              <div className="grid gap-2"><Label htmlFor="dashboard-date-to">ถึงวันที่</Label><DatePicker id="dashboard-date-to" value={dateTo} onChange={setDateTo} className="min-w-36" /></div>
            </div>
          </CardHeader>
          <CardContent>{chartData.length ? <ProductivityChart data={chartData} /> : <p className="grid h-72 place-items-center text-sm text-muted-foreground">กรุณาเลือกช่วงวันที่ให้ถูกต้อง</p>}</CardContent>
        </Card>
        <DashboardTimerCard entries={source.time_entries} projects={source.projects} tasks={source.tasks} onStop={handleStopTimer} />
      </section>
      <section className="grid items-start gap-6 2xl:grid-cols-2">
        <DashboardProjectsTable projects={source.projects} clients={source.clients} tasks={source.tasks} entries={source.time_entries} />
        <DashboardTasksTable tasks={source.tasks} projects={source.projects} />
      </section>
    </div>
  )
}

export default DashboardPage
