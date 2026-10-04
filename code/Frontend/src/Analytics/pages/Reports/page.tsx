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
import { downloadCsv } from '../../analytics.utils'
import { formatDurationSeconds } from '../../../lib/formatters'
import { getReportSummary } from '../../../services/report'
import { useAsyncData } from '../../../shared/useAsyncData'
import type { ReportSummaryData } from '../../../types/analytics'

const now = new Date()
const reportTo = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const reportFrom = `${reportTo.slice(0, 8)}01`
const emptyReport: ReportSummaryData = {
  generatedAt: '', filters: { clients: [], projects: [] },
  summary: { totalTrackedSeconds: 0, trackedTimeTrendPercent: 0, timeEntryCount: 0, projectsWithTime: 0, totalProjects: 0, clientsWithTime: 0, totalClients: 0 },
  timeByClient: [], projectUsage: [],
}

function clampPercent(value: number): number { return Math.max(0, Math.min(100, value)) }

function ReportsPage() {
  const [range, setRange] = useState({ from: reportFrom, to: reportTo })
  const [clientId, setClientId] = useState('ALL')
  const [projectId, setProjectId] = useState('ALL')
  const queryKey = `${range.from}:${range.to}:${clientId}:${projectId}`
  const loadReport = useCallback(() => getReportSummary({
    from: range.from,
    to: range.to,
    clientId: clientId === 'ALL' ? undefined : clientId,
    projectId: projectId === 'ALL' ? undefined : projectId,
  }), [clientId, projectId, range.from, range.to])
  const { data, loading, error, refresh } = useAsyncData(loadReport, emptyReport, queryKey)
  const availableProjects = useMemo(
    () => data.filters.projects.filter((project) => clientId === 'ALL' || project.clientId === clientId),
    [clientId, data.filters.projects],
  )

  if (loading) return <LoadingState label="กำลังประมวลผลรายงาน..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const { summary } = data
  const projectRows = data.projectUsage.map((project) => ({
    project,
    targetHours: project.targetSeconds == null ? 0 : project.targetSeconds / 3600,
    usedHours: project.trackedSeconds / 3600,
  }))
  const clientChartData = data.timeByClient.map((client) => ({ name: client.clientName, hours: Number((client.trackedSeconds / 3600).toFixed(2)) }))
  const projectTimeChartData = data.projectUsage
    .filter((project) => project.trackedSeconds > 0)
    .map((project) => ({ name: project.projectName, hours: Number((project.trackedSeconds / 3600).toFixed(2)) }))
  const showProjectTimeChart = clientId !== 'ALL'
  const timeChartData = showProjectTimeChart ? projectTimeChartData : clientChartData
  const selectedClientName = data.filters.clients.find((client) => client.id === clientId)?.name || 'ลูกค้าที่เลือก'
  const timeChartTitle = showProjectTimeChart ? `เวลาตามโปรเจกต์: ${selectedClientName}` : 'เวลาตามลูกค้า'
  const timeChartDescription = showProjectTimeChart
    ? 'ชั่วโมงที่บันทึกแยกตามโปรเจกต์ของลูกค้าที่เลือก'
    : 'ชั่วโมงที่บันทึกแยกตามลูกค้าในช่วงเวลาที่เลือก'
  const projectChartData = data.projectUsage.filter((project) => project.trackedSeconds > 0 || project.taskProgressPercent > 0).map((project) => ({ name: project.projectName, usage: Number((project.usagePercent ?? 0).toFixed(1)), progress: Number(project.taskProgressPercent.toFixed(1)) }))
  const trendPercent = summary.trackedTimeTrendPercent
  const trend = <span className={`inline-flex items-center gap-1 font-semibold ${trendPercent >= 0 ? 'text-green' : 'text-destructive'}`}>{trendPercent >= 0 ? <FiTrendingUp /> : <FiTrendingDown />}{Math.abs(trendPercent).toFixed(0)}% จากช่วงก่อนหน้า</span>
  const exportReport = () => downloadCsv(`time-report-${range.from}-${range.to}.csv`, [
    ['โปรเจกต์', 'ลูกค้า', 'เวลาเป้าหมาย', 'เวลาที่ใช้', 'ใช้ไป (%)', 'ความคืบหน้า (%)', 'สถานะ'],
    ...projectRows.map(({ project, targetHours, usedHours }) => [project.projectName, project.clientName, targetHours || '', usedHours.toFixed(2), project.usagePercent?.toFixed(1) ?? '', project.taskProgressPercent.toFixed(1), project.status]),
  ])

  return <div className="mx-auto w-full min-w-0 max-w-screen-2xl space-y-6">
    <PageHeader eyebrow="จัดการ / รายงาน" title="รายงาน" description="วิเคราะห์การใช้เวลาและความคืบหน้าของโปรเจกต์" actions={<Button className="h-10" onClick={exportReport}><FiDownload /> ส่งออกรายงาน CSV</Button>} />

    <Card><CardContent className="grid grid-cols-1 gap-4 p-4 sm:grid-cols-2 xl:grid-cols-4">
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-from">จากวันที่</Label><DatePicker id="report-from" value={range.from} onChange={(value) => setRange((current) => ({ ...current, from: value }))} /></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-to">ถึงวันที่</Label><DatePicker id="report-to" value={range.to} onChange={(value) => setRange((current) => ({ ...current, to: value }))} /></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-client">ลูกค้า</Label><NativeSelect id="report-client" value={clientId} onChange={(event) => { setClientId(event.target.value); setProjectId('ALL') }}><option value="ALL">ลูกค้าทั้งหมด</option>{data.filters.clients.map((client) => <option key={client.id} value={client.id}>{client.name}</option>)}</NativeSelect></div>
      <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor="report-project">โปรเจกต์</Label><NativeSelect id="report-project" value={projectId} onChange={(event) => setProjectId(event.target.value)}><option value="ALL">โปรเจกต์ทั้งหมด</option>{availableProjects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</NativeSelect></div>
    </CardContent></Card>

    <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <SummaryCard label="เวลารวม" icon={<FiClock />} value={formatDurationSeconds(summary.totalTrackedSeconds)} foot={trend} accent={trendPercent >= 0 ? 'green' : 'red'} compact />
      <SummaryCard label="จำนวนรายการเวลา" icon={<FiList />} value={summary.timeEntryCount} unit="รายการ" foot="ในช่วงเวลาที่เลือก" accent="blue" />
      <SummaryCard label="โปรเจกต์ที่มีการบันทึกเวลา" icon={<FiBriefcase />} value={summary.projectsWithTime} unit="โปรเจกต์" foot={`จากทั้งหมด ${summary.totalProjects} โปรเจกต์`} accent="violet" />
      <SummaryCard label="ลูกค้าที่มีการทำงาน" icon={<FiUsers />} value={summary.clientsWithTime} unit="ลูกค้า" foot={`จากทั้งหมด ${summary.totalClients} ลูกค้า`} accent="orange" />
    </section>

    <Card><CardHeader><CardTitle>{timeChartTitle}</CardTitle><CardDescription>{timeChartDescription}</CardDescription></CardHeader><CardContent>
      {timeChartData.length ? <ChartContainer className="h-[300px] w-full min-w-0 aspect-auto" config={{ hours: { label: 'ชั่วโมง', color: '#4F6BFF' } }}><BarChart data={timeChartData} layout="vertical" margin={{ left: 12, right: 28 }}><CartesianGrid horizontal={false} strokeDasharray="3 3" /><XAxis type="number" axisLine={false} tickLine={false} unit=" ชม." /><YAxis type="category" dataKey="name" axisLine={false} tickLine={false} width={110} tick={{ fontSize: 12 }} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="hours" name="ชั่วโมง" fill="var(--color-hours)" radius={[0, 6, 6, 0]} /></BarChart></ChartContainer> : <p className="py-12 text-center text-sm text-muted-foreground">ไม่มีข้อมูลเวลาในช่วงที่เลือก</p>}
    </CardContent></Card>

    <Card><CardHeader><CardTitle>การใช้ชั่วโมงเทียบเป้าหมายและความคืบหน้างาน</CardTitle><CardDescription>เปรียบเทียบเปอร์เซ็นต์เวลาที่ใช้กับเปอร์เซ็นต์งานที่เสร็จของแต่ละโปรเจกต์</CardDescription></CardHeader><CardContent>
      {projectChartData.length ? <div className="w-full overflow-x-auto"><ChartContainer className="h-[320px] min-w-[44rem] aspect-auto" config={{ usage: { label: 'การใช้ชั่วโมง', color: '#4F6BFF' }, progress: { label: 'ความคืบหน้างาน', color: '#7C3AED' } }}><BarChart data={projectChartData}><CartesianGrid vertical={false} strokeDasharray="3 3" /><XAxis dataKey="name" axisLine={false} tickLine={false} interval={0} /><YAxis axisLine={false} tickLine={false} unit="%" domain={[0, (maximum: number) => Math.max(100, Math.ceil(maximum / 20) * 20)]} /><ChartTooltip content={<ChartTooltipContent />} /><Bar dataKey="usage" name="การใช้ชั่วโมง" fill="var(--color-usage)" radius={[5, 5, 0, 0]} /><Bar dataKey="progress" name="ความคืบหน้างาน" fill="var(--color-progress)" radius={[5, 5, 0, 0]} /></BarChart></ChartContainer></div> : <p className="py-12 text-center text-sm text-muted-foreground">ไม่มีข้อมูลโปรเจกต์ในช่วงที่เลือก</p>}
    </CardContent></Card>

    <Card><CardHeader><CardTitle>รายละเอียดการใช้เวลาแต่ละโปรเจกต์</CardTitle><CardDescription>เวลา เป้าหมาย และความคืบหน้าของโปรเจกต์</CardDescription></CardHeader><CardContent><Table><TableHeader><TableRow><TableHead>ชื่อโปรเจกต์</TableHead><TableHead>ชื่อลูกค้า</TableHead><TableHead className="w-32">เวลาเป้าหมาย</TableHead><TableHead className="w-32">เวลาที่ใช้</TableHead><TableHead className="w-36">ใช้ไป</TableHead><TableHead className="w-36">ความคืบหน้า</TableHead><TableHead className="w-36">สถานะ</TableHead></TableRow></TableHeader><TableBody>
      {projectRows.map(({ project, targetHours }) => <TableRow key={project.projectId}><TableCell><div className="flex min-w-0 items-center gap-2"><span className="size-2.5 shrink-0 rounded-full" style={{ backgroundColor: project.color }} /><strong className="truncate" title={project.projectName}>{project.projectName}</strong></div></TableCell><TableCell>{project.clientName}</TableCell><TableCell>{targetHours ? `${targetHours.toFixed(1)} ชม.` : 'ไม่กำหนด'}</TableCell><TableCell>{formatDurationSeconds(project.trackedSeconds)}</TableCell><TableCell><div className="flex min-w-28 items-center gap-2"><Progress value={clampPercent(project.usagePercent ?? 0)} /><span className="w-10 text-right">{project.usagePercent == null ? '—' : `${project.usagePercent.toFixed(0)}%`}</span></div></TableCell><TableCell><div className="flex min-w-28 items-center gap-2"><Progress value={clampPercent(project.taskProgressPercent)} indicatorColor="#7C3AED" /><span className="w-10 text-right">{project.taskProgressPercent.toFixed(0)}%</span></div></TableCell><TableCell><StatusBadge status={project.status} /></TableCell></TableRow>)}
      {!projectRows.length && <TableRow><TableCell colSpan={7} className="py-10 text-center text-muted-foreground">ไม่พบโปรเจกต์ตามตัวกรอง</TableCell></TableRow>}
    </TableBody></Table></CardContent></Card>
  </div>
}

export default ReportsPage
