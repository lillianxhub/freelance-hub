import { useCallback, useMemo, useState } from 'react'
import { FiBriefcase, FiClock, FiDownload, FiList, FiTrendingDown, FiTrendingUp, FiUsers } from 'react-icons/fi'
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import PageHeader from '../../../components/PageHeader'
import SummaryCard from '../../../components/SummaryCard'
import StatusBadge from '../../../components/StatusBadge'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { Button } from '../../../components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../../components/ui/chart'
import { DatePicker } from '../../../components/ui/date-picker'
import { Label } from '../../../components/ui/label'
import { NativeSelect } from '../../../components/ui/native-select'
import { Progress } from '../../../components/ui/progress'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../../components/ui/table'
import { downloadCsv } from '../../../utils/csv'
import { formatDurationSeconds } from '../../../utils/duration'
import { getReportSummary } from '../../../services/report'
import { useAsyncData } from '../../../hooks/useAsyncData'
import type { ReportSummaryData } from '../../../types/analytics'

const emptyReport: ReportSummaryData = {
  generatedAt: '', filters: { clients: [], projects: [] },
  summary: { totalTrackedSeconds: 0, trackedTimeTrendPercent: null, timeEntryCount: 0, projectsWithTime: 0, totalProjects: 0, clientsWithTime: 0, totalClients: 0 },
}
const emptyDistribution: ReportDistributionData = { groupBy: 'CLIENT', items: [] }
const emptyProjects: ReportProjectPage = { items: [], page: 1, totalPages: 1, total: 0 }

function clampPercent(value: number): number { return Math.max(0, Math.min(100, value)) }

function truncateChartLabel(value: string): string {
  const characters = Array.from(value)
  return characters.length > 12 ? `${characters.slice(0, 12).join('')}…` : value
}

function previousPeriodLabel(from: string, to: string): string {
  const start = new Date(`${from}T00:00:00Z`)
  const end = new Date(`${to}T00:00:00Z`)
  const days = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1
  const previousStart = new Date(start.getTime() - days * 86_400_000)
  const previousEnd = new Date(start.getTime() - 86_400_000)
  const fullDate = new Intl.DateTimeFormat('th-TH', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
  const dayMonth = new Intl.DateTimeFormat('th-TH', { day: 'numeric', month: 'short', timeZone: 'UTC' })

  if (previousStart.getUTCFullYear() === previousEnd.getUTCFullYear()) {
    if (previousStart.getUTCMonth() === previousEnd.getUTCMonth()) {
      return `${previousStart.getUTCDate()}–${fullDate.format(previousEnd)}`
    }
    return `${dayMonth.format(previousStart)}–${fullDate.format(previousEnd)}`
  }
  return `${fullDate.format(previousStart)}–${fullDate.format(previousEnd)}`
}

function ReportsPage() {
  const [range, setRange] = useState({ from: '', to: '' })
  const [clientId, setClientId] = useState('ALL')
  const [projectId, setProjectId] = useState('ALL')
  const [groupBy, setGroupBy] = useState<'CLIENT' | 'PROJECT'>('CLIENT')
  const [page, setPage] = useState(1)
  const queryKey = `${range.from}:${range.to}:${clientId}:${projectId}`
  const query: ReportSummaryQuery = useMemo(() => ({
    from: range.from,
    to: range.to,
    clientId: clientId === 'ALL' ? undefined : clientId,
    projectId: projectId === 'ALL' ? undefined : projectId,
  }), [range.from, range.to, clientId, projectId])
  const hasDateRange = Boolean(range.from && range.to)
  const validRange = hasDateRange && range.from <= range.to
  const hasPartialRange = Boolean(range.from) !== Boolean(range.to)
  const canLoad = !hasPartialRange && (!hasDateRange || validRange)
  const loadSummary = useCallback(() => canLoad ? getReportSummary(query) : Promise.resolve(emptyReport), [query, canLoad])
  const loadDistribution = useCallback(() => canLoad ? getReportDistribution(query, groupBy) : Promise.resolve(emptyDistribution), [query, groupBy, canLoad])
  const loadProjects = useCallback(() => canLoad ? getReportProjects(query, page) : Promise.resolve(emptyProjects), [query, page, canLoad])
  const { data, loading: summaryLoading, error: summaryError, refresh: refreshSummary } = useAsyncData(loadSummary, emptyReport, queryKey)
  const { data: distribution, loading: distributionLoading, error: distributionError, refresh: refreshDistribution } = useAsyncData(loadDistribution, emptyDistribution, `${queryKey}:${groupBy}`)
  const { data: projects, loading: projectsLoading, error: projectsError, refresh: refreshProjects } = useAsyncData(loadProjects, emptyProjects, `${queryKey}:${page}`)
  const loading = summaryLoading || distributionLoading || projectsLoading
  const error = summaryError || distributionError || projectsError
  const refresh = () => { void Promise.all([refreshSummary(), refreshDistribution(), refreshProjects()]) }
  const availableProjects = useMemo(
    () => data.filters.projects.filter((project) => clientId === 'ALL' || project.clientId === clientId),
    [clientId, data.filters.projects],
  )

  const { summary } = data
  const projectRows = projects.items.map((project) => ({
    project,
    targetHours: project.targetSeconds == null ? 0 : project.targetSeconds / 3600,
    usedHours: project.trackedSeconds / 3600,
  }))
  const clientChartData = distribution.items.map((item) => ({ name: item.name, hours: Number((item.trackedSeconds / 3600).toFixed(2)) }))
  const showProjectTimeChart = groupBy === 'PROJECT'
  const timeChartData = clientChartData
  const selectedClientName = data.filters.clients.find((client) => client.id === clientId)?.name || 'ลูกค้าที่เลือก'
  const timeChartTitle = showProjectTimeChart ? 'เวลาตามโปรเจกต์' : 'เวลาตามลูกค้า'
  const timeChartDescription = clientId !== 'ALL'
    ? `ชั่วโมงที่บันทึก${showProjectTimeChart ? 'แยกตามโปรเจกต์' : ''}ของลูกค้า ${selectedClientName}`
    : `ชั่วโมงที่บันทึกแยกตาม${showProjectTimeChart ? 'โปรเจกต์' : 'ลูกค้า'}${hasDateRange ? 'ในช่วงเวลาที่เลือก' : 'ทุกช่วงเวลา'}`
  const projectChartData = projects.items.filter((project) => project.trackedSeconds > 0 || project.taskProgressPercent > 0).map((project) => ({ name: project.projectName, usage: Number((project.usagePercent ?? 0).toFixed(1)), progress: Number(project.taskProgressPercent.toFixed(1)) }))
  const trendPercent = summary.trackedTimeTrendPercent
  const trend = trendPercent == null ? 'ทุกช่วงเวลา' : <span className={`inline-flex flex-wrap items-center gap-1 font-semibold ${trendPercent >= 0 ? 'text-green' : 'text-destructive'}`}>{trendPercent >= 0 ? <FiTrendingUp /> : <FiTrendingDown />}{trendPercent >= 0 ? 'เพิ่มขึ้น' : 'ลดลง'} {Math.abs(trendPercent).toFixed(0)}% เทียบกับ {previousPeriodLabel(range.from, range.to)}</span>
  const exportReport = () => downloadCsv(hasDateRange ? `time-report-${range.from}-${range.to}.csv` : 'time-report-all.csv', [
    ['โปรเจกต์', 'ลูกค้า', 'เวลาเป้าหมาย', 'เวลาที่ใช้', 'ใช้ไป (%)', 'ความคืบหน้า (%)', 'สถานะ'],
    ...projectRows.map(({ project, targetHours, usedHours }) => [project.projectName, project.clientName, targetHours || '', usedHours.toFixed(2), project.usagePercent?.toFixed(1) ?? '', project.taskProgressPercent.toFixed(1), project.status]),
  ])

  return <div className="mx-auto w-full min-w-0 max-w-screen-2xl space-y-6">
    <PageHeader eyebrow="จัดการ / รายงาน" title="รายงาน" description="วิเคราะห์การใช้เวลาและความคืบหน้าของโปรเจกต์" actions={<Button className="h-10" onClick={exportReport} disabled={!canLoad || projects.items.length === 0}><FiDownload /> ส่งออกหน้านี้ CSV</Button>} />

    <Card><CardContent className="grid grid-cols-1 gap-4 p-4 sm:grid-cols-2 xl:grid-cols-4">
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-from">จากวันที่</Label><DatePicker id="report-from" value={range.from} placeholder="เลือกวันที่เริ่ม" onChange={(value) => { setRange((current) => ({ ...current, from: value })); setPage(1) }} /></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-to">ถึงวันที่</Label><DatePicker id="report-to" value={range.to} placeholder="เลือกวันที่สิ้นสุด" onChange={(value) => { setRange((current) => ({ ...current, to: value })); setPage(1) }} /></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-client">ลูกค้า</Label><NativeSelect id="report-client" value={clientId} disabled={!canLoad} onChange={(event) => { setClientId(event.target.value); setProjectId('ALL'); setPage(1) }}><option value="ALL">ลูกค้าทั้งหมด</option>{data.filters.clients.map((client) => <option key={client.id} value={client.id}>{client.name}</option>)}</NativeSelect></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-project">โปรเจกต์</Label><NativeSelect id="report-project" value={projectId} disabled={!canLoad} onChange={(event) => { setProjectId(event.target.value); setPage(1) }}><option value="ALL">โปรเจกต์ทั้งหมด</option>{availableProjects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</NativeSelect></div>
    </CardContent></Card>

    {hasPartialRange ? <p className="text-sm text-destructive">เลือกวันที่เริ่มและวันที่สิ้นสุดให้ครบ หรือเว้นว่างทั้งคู่เพื่อดูทุกช่วงเวลา</p>
      : hasDateRange && !validRange ? <p className="text-sm text-destructive">วันที่เริ่มต้องไม่เกินวันที่สิ้นสุด</p>
        : loading ? <LoadingState label="กำลังประมวลผลรายงาน..." /> : error ? <ErrorState message={error} onRetry={refresh} /> : <>

    <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <SummaryCard label="เวลารวม" icon={<FiClock />} value={formatDurationSeconds(summary.totalTrackedSeconds)} foot={trend} accent={trendPercent == null ? 'blue' : trendPercent >= 0 ? 'green' : 'red'} compact />
      <SummaryCard label="จำนวนรายการเวลา" icon={<FiList />} value={summary.timeEntryCount} unit="รายการ" foot={hasDateRange ? 'ในช่วงเวลาที่เลือก' : 'ทุกช่วงเวลา'} accent="blue" />
      <SummaryCard label="โปรเจกต์ที่มีการบันทึกเวลา" icon={<FiBriefcase />} value={summary.projectsWithTime} unit="โปรเจกต์" foot={`จากทั้งหมด ${summary.totalProjects} โปรเจกต์`} accent="violet" />
      <SummaryCard label="ลูกค้าที่มีการทำงาน" icon={<FiUsers />} value={summary.clientsWithTime} unit="ลูกค้า" foot={`จากทั้งหมด ${summary.totalClients} ลูกค้า`} accent="orange" />
    </section>

    <Card><CardHeader><div className="flex min-w-0 flex-wrap items-start justify-between gap-3"><div className="min-w-0"><CardTitle>{timeChartTitle}</CardTitle><CardDescription className="break-words">{timeChartDescription}</CardDescription></div><ToggleGroup type="single" value={groupBy} onValueChange={(value) => { if (value === 'CLIENT' || value === 'PROJECT') setGroupBy(value) }} aria-label="จัดกลุ่มเวลาตาม"><ToggleGroupItem value="CLIENT" aria-label="เวลาตามลูกค้า">ตามลูกค้า</ToggleGroupItem><ToggleGroupItem value="PROJECT" aria-label="เวลาตามโปรเจกต์">ตามโปรเจกต์</ToggleGroupItem></ToggleGroup></div></CardHeader><CardContent>
      {timeChartData.length ? <ChartContainer className="h-[300px] w-full min-w-0 aspect-auto" config={{ hours: { label: 'ชั่วโมง', color: '#4F6BFF' } }}><BarChart data={timeChartData} layout="vertical" margin={{ left: 12, right: 28 }}><CartesianGrid horizontal={false} strokeDasharray="3 3" /><XAxis type="number" axisLine={false} tickLine={false} unit=" ชม." /><YAxis type="category" dataKey="name" axisLine={false} tickLine={false} width={110} tick={{ fontSize: 12 }} tickFormatter={showProjectTimeChart ? truncateChartLabel : undefined} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="hours" name="ชั่วโมง" fill="var(--color-hours)" radius={[0, 6, 6, 0]} /></BarChart></ChartContainer> : <p className="py-12 text-center text-sm text-muted-foreground">ไม่มีข้อมูลเวลาในช่วงที่เลือก</p>}
    </CardContent></Card>

    <Card><CardHeader><CardTitle>การใช้ชั่วโมงเทียบเป้าหมายและความคืบหน้างาน</CardTitle><CardDescription>เปรียบเทียบเปอร์เซ็นต์เวลาที่ใช้กับเปอร์เซ็นต์งานที่เสร็จของโปรเจกต์ในหน้านี้</CardDescription></CardHeader><CardContent>
      {projectChartData.length ? <div className="w-full overflow-x-auto"><ChartContainer className="h-[320px] min-w-[44rem] aspect-auto" config={{ usage: { label: 'การใช้ชั่วโมง', color: '#4F6BFF' }, progress: { label: 'ความคืบหน้างาน', color: '#7C3AED' } }}><BarChart data={projectChartData}><CartesianGrid vertical={false} strokeDasharray="3 3" /><XAxis dataKey="name" axisLine={false} tickLine={false} interval={0} tickFormatter={truncateChartLabel} /><YAxis axisLine={false} tickLine={false} unit="%" domain={[0, (maximum: number) => Math.max(100, Math.ceil(maximum / 20) * 20)]} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="usage" name="การใช้ชั่วโมง" fill="var(--color-usage)" radius={[5, 5, 0, 0]} /><Bar dataKey="progress" name="ความคืบหน้างาน" fill="var(--color-progress)" radius={[5, 5, 0, 0]} /></BarChart></ChartContainer></div> : <p className="py-12 text-center text-sm text-muted-foreground">ไม่มีข้อมูลโปรเจกต์ในช่วงที่เลือก</p>}
    </CardContent></Card>

    <Card><CardHeader><CardTitle>รายละเอียดการใช้เวลาแต่ละโปรเจกต์</CardTitle><CardDescription>เวลา เป้าหมาย และความคืบหน้าของโปรเจกต์ ({projects.total} รายการ)</CardDescription></CardHeader><CardContent><Table pagination={{ page: projects.page, totalPages: projects.totalPages, total: projects.total, onPageChange: setPage }}><TableHeader><TableRow><TableHead>ชื่อโปรเจกต์</TableHead><TableHead>ชื่อลูกค้า</TableHead><TableHead className="w-32">เวลาเป้าหมาย</TableHead><TableHead className="w-32">เวลาที่ใช้</TableHead><TableHead className="w-36">ใช้ไป</TableHead><TableHead className="w-36">ความคืบหน้า</TableHead><TableHead className="w-36">สถานะ</TableHead></TableRow></TableHeader><TableBody>
      {projectRows.map(({ project, targetHours }) => <TableRow key={project.projectId}><TableCell><div className="flex min-w-0 items-center gap-2"><span className="size-2.5 shrink-0 rounded-full" style={{ backgroundColor: project.color }} /><strong className="truncate" title={project.projectName}>{project.projectName}</strong></div></TableCell><TableCell>{project.clientName}</TableCell><TableCell>{targetHours ? `${targetHours.toFixed(1)} ชม.` : 'ไม่กำหนด'}</TableCell><TableCell>{formatDurationSeconds(project.trackedSeconds)}</TableCell><TableCell><div className="flex min-w-28 items-center gap-2"><Progress value={clampPercent(project.usagePercent ?? 0)} /><span className="w-10 text-right">{project.usagePercent == null ? '—' : `${project.usagePercent.toFixed(0)}%`}</span></div></TableCell><TableCell><div className="flex min-w-28 items-center gap-2"><Progress value={clampPercent(project.taskProgressPercent)} indicatorColor="#7C3AED" /><span className="w-10 text-right">{project.taskProgressPercent.toFixed(0)}%</span></div></TableCell><TableCell><StatusBadge status={project.status} /></TableCell></TableRow>)}
      {!projectRows.length && <TableRow><TableCell colSpan={7} className="py-10 text-center text-muted-foreground">ไม่พบโปรเจกต์ตามตัวกรอง</TableCell></TableRow>}
    </TableBody></Table></CardContent></Card>
    </>}
  </div>
}

export default ReportsPage
