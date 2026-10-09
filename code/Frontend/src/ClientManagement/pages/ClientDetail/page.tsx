import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/card'
import { Progress } from '../../../components/ui/progress'
import PaginationControls from '../../../components/PaginationControls'
import { Link, useParams } from 'react-router-dom'
import { FiArrowLeft, FiArrowRight, FiBriefcase, FiClock, FiList } from 'react-icons/fi'
import { useCallback, useState } from 'react'
import PageHeader from '../../../components/PageHeader'
import SummaryCard from '../../../components/SummaryCard'
import StatusBadge from '../../../components/StatusBadge'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { formatDurationSeconds } from '../../../utils/duration'
import { useAsyncData } from '../../../hooks/useAsyncData'
import { getClientById } from '../../../services/client'
import { listAllProjects } from '../../../services/project'
import { summarizeTimeEntries } from '../../../services/timeTracking'
import type { Client } from '../../../types/client'
import type { Project } from '../../../types/project'

interface ClientDetailData {
  client: Client | null
  projects: Project[]
  totalSeconds: number
  entryCount: number
}

const emptyData: ClientDetailData = { client: null, projects: [], totalSeconds: 0, entryCount: 0 }


function ClientDetailPage() {
  const { clientId } = useParams()
  const load = useCallback(async (): Promise<ClientDetailData> => {
    if (!clientId) throw new Error('ไม่พบรหัสลูกค้า')
    const [client, projects, summary] = await Promise.all([
      getClientById(clientId),
      listAllProjects({ clientId, status: 'ALL' }),
      summarizeTimeEntries({ clientId }),
    ])
    return { client, projects, totalSeconds: summary.totalSeconds, entryCount: summary.entryCount }
  }, [clientId])
  const { data, loading, error, refresh } = useAsyncData(load, emptyData, clientId)
  const [projectPage, setProjectPage] = useState(1)

  if (loading) return <LoadingState label="กำลังโหลดข้อมูลลูกค้า..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const client = data.client
  if (!client) return <ErrorState message="ไม่พบClientsที่ต้องการ" />

  const projects = data.projects
  const projectPageSize = 4
  const projectTotalPages = Math.max(1, Math.ceil(projects.length / projectPageSize))
  const safeProjectPage = Math.min(projectPage, projectTotalPages)
  const visibleProjects = projects.slice(
    (safeProjectPage - 1) * projectPageSize,
    safeProjectPage * projectPageSize,
  )
  const displayName = client.company_name || client.name

  return (
    <div className="mx-auto w-full max-w-screen-2xl">
      <Link className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary no-underline hover:text-primary-dark" to="/clients"><FiArrowLeft aria-hidden="true" /> กลับไปหน้าลูกค้า</Link>
      <PageHeader title={displayName} description={`${client.name || 'ไม่ระบุผู้ติดต่อ'} · ${client.email || 'ไม่มีอีเมล'}`} actions={<StatusBadge status={client.status} />} />

      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard
          label="โปรเจกต์"
          icon={<FiBriefcase aria-hidden="true" />}
          value={projects.length}
          foot="ทั้งหมดของลูกค้ารายนี้"
          accent="blue"
        />
        <SummaryCard
          label="เวลาที่บันทึก"
          icon={<FiClock aria-hidden="true" />}
          value={formatDurationSeconds(data.totalSeconds)}
          foot="รวมทุกโปรเจกต์"
          accent="green"
          compact
        />
        <SummaryCard
          label="รายการเวลา"
          icon={<FiList aria-hidden="true" />}
          value={data.entryCount}
          foot="รายการที่บันทึกทั้งหมด"
          accent="violet"
        />
      </div>

      <div className="grid items-start gap-4 lg:grid-cols-3">
        <div className="grid gap-4 lg:col-span-2">
          <Card asChild><section className="!p-0">
            <CardHeader className="p-6 pb-2">
              <CardTitle>โปรเจกต์</CardTitle>
              <CardDescription>โปรเจกต์ทั้งหมดของลูกค้ารายนี้</CardDescription>
            </CardHeader>
            <CardContent className="p-6 pt-0">
              <div className="flex flex-col">
                {visibleProjects.map((project) => {
                  const trackedSeconds = project.time_tracking?.tracked_seconds ?? 0
                  const totalTasks = project.task_progress?.total_tasks ?? 0
                  const completedTasks = project.task_progress?.completed_tasks ?? 0
                  const taskProgress = Math.max(0, Math.min(100, Math.round(project.task_progress?.percent ?? 0)))

                  return (
                    <Link
                      className="group grid grid-cols-[auto_minmax(0,1fr)] items-start gap-x-3 gap-y-3 rounded-xl border-b border-border p-3 transition-colors last:border-b-0 hover:bg-secondary sm:grid-cols-[auto_minmax(0,1fr)_minmax(16rem,auto)] sm:items-center sm:gap-3"
                      key={project.id}
                      to={`/projects/${project.id}`}
                    >
                      <span className="mt-1.5 size-2.5 shrink-0 rounded-full sm:mt-0" style={{ backgroundColor: project.color }} aria-hidden="true" />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-text-primary">{project.name}</p>
                        <p className="truncate text-sm text-text-secondary">งานเสร็จ {completedTasks}/{totalTasks}</p>
                        <Progress className="mt-2 h-1.5 w-full bg-border sm:w-3/5" value={taskProgress} indicatorColor={project.color} />
                      </div>
                      <div className="col-start-2 flex min-w-0 items-center justify-end gap-2 sm:col-start-auto sm:min-w-64 sm:gap-3">
                        <strong className="whitespace-nowrap text-sm font-bold tabular-nums text-text-primary">{formatDurationSeconds(trackedSeconds)}</strong>
                        <StatusBadge status={project.status} />
                        <FiArrowRight className="shrink-0 text-muted-foreground transition-colors group-hover:text-primary" aria-hidden="true" />
                      </div>
                    </Link>
                  )
                })}
                {projects.length === 0 && <p className="m-0 py-8 text-center text-sm text-text-secondary">ยังไม่มีโปรเจกต์</p>}
              </div>
              <PaginationControls page={safeProjectPage} totalPages={projectTotalPages} onPageChange={setProjectPage} queryParam="projectPage" className="mt-4" />
            </CardContent>
          </section></Card>

        </div>

        <Card asChild><aside className="!p-0">
          <CardHeader className="p-6 pb-2">
            <CardTitle>ข้อมูลลูกค้า</CardTitle>
            <CardDescription>ข้อมูลสำหรับติดต่อ</CardDescription>
          </CardHeader>
          <CardContent className="p-6 pt-0">
            <div className="grid gap-4">
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">ผู้ติดต่อ</span>
                <strong className="text-sm leading-relaxed text-text-primary">{client.name || '—'}</strong>
              </div>
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">อีเมล</span>
                <strong className="text-sm leading-relaxed text-text-primary">{client.email || '—'}</strong>
              </div>
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">โทรศัพท์</span>
                <strong className="text-sm leading-relaxed text-text-primary">{client.phone || '—'}</strong>
              </div>
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">เลขผู้เสียภาษี</span>
                <strong className="text-sm leading-relaxed text-text-primary">{client.tax_id || '—'}</strong>
              </div>
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">ที่อยู่</span>
                <p className="m-0 text-sm leading-relaxed text-text-primary">{client.address || '—'}</p>
              </div>
              <div className="grid gap-1.5">
                <span className="text-xs text-text-secondary">หมายเหตุ</span>
                <p className="m-0 text-sm leading-relaxed text-text-primary">{client.notes || '—'}</p>
              </div>
            </div>
          </CardContent>
        </aside></Card>
      </div>
    </div>
  )
}

export default ClientDetailPage
