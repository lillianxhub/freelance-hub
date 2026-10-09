import { useState } from 'react'
import { FiBriefcase, FiCheckSquare, FiClock, FiTrendingDown, FiTrendingUp } from 'react-icons/fi'
import { toast } from 'sonner'
import { getErrorMessage } from '../../../api/apiError'
import PageHeader from '../../../components/PageHeader'
import SummaryCard from '../../../components/SummaryCard'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../../../components/ui/card'
import { Label } from '../../../components/ui/label'
import { NativeSelect } from '../../../components/ui/native-select'
import { activityChartPoints, formatChartDuration, weeklyChartPoints } from '../../../lib/dashboard'
import { stopTimer } from '../../../services/timeTracking'
import { useCurrentTimer } from '../../../TimeTracking/useCurrentTimer'
import type { DashboardChartPeriod } from '../../../types/dashboard'
import DashboardProjectsTable from '../../components/DashboardProjectsTable'
import DashboardTasksTable from '../../components/DashboardTasksTable'
import DashboardTimerCard from '../../components/DashboardTimerCard'
import ProductivityChart from '../../components/ProductivityChart'
import { useDashboard } from '../../useDashboard'
import { useDashboardActivity } from '../../useDashboardActivity'

function Trend({ value }: { value: number | null }) {
  if (value === null) return <span>ยังไม่มีข้อมูลสัปดาห์ก่อน</span>
  const positive = value >= 0
  return (
    <span
      className={
        positive
          ? 'inline-flex items-center gap-1 font-semibold text-green'
          : 'inline-flex items-center gap-1 font-semibold text-destructive'
      }
    >
      {positive ? <FiTrendingUp aria-hidden="true" /> : <FiTrendingDown aria-hidden="true" />}
      {Math.abs(value)}% จากสัปดาห์ก่อน
    </span>
  )
}

function DashboardPage() {
  const { data, loading, error, refresh, loadActivity } = useDashboard()
  const { currentTimer, refreshCurrentTimer } = useCurrentTimer()
  const [period, setPeriod] = useState<DashboardChartPeriod>('WEEK')
  const {
    activity,
    loading: activityLoading,
    error: activityError,
    retry: retryActivity,
  } = useDashboardActivity(period, loadActivity)

  const chartData =
    period === 'WEEK'
      ? weeklyChartPoints(data.dailyWork)
      : activity?.period === period
        ? activityChartPoints(period, activity.points)
        : []
  const summary = data.summary
  const generatedAt = data.generatedAt ? new Date(data.generatedAt) : new Date()

  if (loading) return <LoadingState label="กำลังโหลดข้อมูลภาพรวม..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  async function handleStopTimer() {
    try {
      await stopTimer()
      await refreshCurrentTimer()
      await refresh()
    } catch (reason: unknown) {
      toast.error(getErrorMessage(reason, 'ไม่สามารถหยุดตัวจับเวลาได้'))
    }
  }

  return (
    <div className="mx-auto w-full max-w-screen-2xl space-y-6">
      <PageHeader
        eyebrow={new Intl.DateTimeFormat('th-TH', {
          dateStyle: 'full',
          timeZone: 'Asia/Bangkok',
        }).format(generatedAt)}
        title="ภาพรวมการทำงาน"
        description="ติดตามเวลา โปรเจกต์ และงานสำคัญจากที่เดียว"
      />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="สรุปการทำงาน">
        <SummaryCard
          label="เวลาที่บันทึกสัปดาห์นี้"
          value={formatChartDuration(summary.weekTrackedSeconds)}
          icon={<FiClock aria-hidden="true" />}
          accent="blue"
          compact
          foot={<Trend value={summary.weekTrendPercent} />}
        />
        <SummaryCard
          label="การใช้ชั่วโมงเป้าหมาย"
          value={
            summary.targetUsagePercent === null ? '—' : `${Math.round(summary.targetUsagePercent)}%`
          }
          icon={<FiClock aria-hidden="true" />}
          accent="violet"
          compact
          foot={`${formatChartDuration(summary.activeProjectTrackedSeconds)} / ${formatChartDuration(summary.activeProjectTargetSeconds)}`}
        />
        <SummaryCard
          label="โปรเจกต์ที่กำลังทำ"
          value={summary.activeProjectCount}
          unit="โปรเจกต์"
          icon={<FiBriefcase aria-hidden="true" />}
          accent="orange"
          foot="สถานะกำลังดำเนินการ"
        />
        <SummaryCard
          label="งานที่เสร็จแล้ว"
          value={`${summary.completedTaskCount} / ${summary.totalTaskCount}`}
          icon={<FiCheckSquare aria-hidden="true" />}
          accent="green"
          compact
          foot={`เสร็จแล้ว ${Math.round(summary.completedTaskPercent)}%`}
        />
      </section>
      <section className="grid items-start gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(20rem,1fr)]">
        <Card>
          <CardHeader className="gap-4 sm:grid-cols-[1fr_auto]">
            <div>
              <CardTitle>ชั่วโมงทำงาน</CardTitle>
              <CardDescription>เวลาที่บันทึกตามช่วงที่เลือก</CardDescription>
            </div>
            <div className="grid min-w-36 gap-2">
              <Label htmlFor="dashboard-period">ช่วงเวลา</Label>
              <NativeSelect
                id="dashboard-period"
                value={period}
                onChange={(event) => {
                  const nextPeriod = event.target.value as DashboardChartPeriod
                  setPeriod(nextPeriod)
                }}
              >
                <option value="WEEK">สัปดาห์</option>
                <option value="MONTH">เดือน</option>
                <option value="YEAR">ปี</option>
              </NativeSelect>
            </div>
          </CardHeader>
          <CardContent>
            {period !== 'WEEK' && activityLoading ? (
              <LoadingState label="กำลังโหลดกราฟ..." />
            ) : period !== 'WEEK' && activityError ? (
              <ErrorState message={activityError} onRetry={retryActivity} />
            ) : (
              <ProductivityChart data={chartData} />
            )}
          </CardContent>
        </Card>
        <DashboardTimerCard
          currentTimer={currentTimer}
          recentEntries={data.recentTimeEntries ?? []}
          onStop={handleStopTimer}
        />
      </section>
      <section className="grid items-start gap-6 2xl:grid-cols-2">
        <DashboardProjectsTable projects={data.activeProjects} />
        <DashboardTasksTable tasks={data.openTasks} />
      </section>
    </div>
  )
}

export default DashboardPage
