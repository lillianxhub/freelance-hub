import { Card } from '../../components/ui/card'
import { Button } from '../../components/ui/button'
import { NativeSelect } from '../../components/ui/native-select'
import { Progress } from '../../components/ui/progress'
import { Link } from 'react-router-dom'
import { FiEdit2 } from 'react-icons/fi'
import type { Project, ProjectStatus } from '../../types/project'
import { formatDate } from '../../utils/date'
import { formatTimer } from '../../utils/duration'

const projectStatusLabels: Record<ProjectStatus, string> = {
  PLANNED: 'วางแผน',
  ACTIVE: 'กำลังดำเนินการ',
  ON_HOLD: 'พักงาน',
  COMPLETED: 'เสร็จสิ้น',
  ARCHIVED: 'เก็บถาวร',
}

const allowedStatusTransitions: Record<ProjectStatus, readonly ProjectStatus[]> = {
  PLANNED: ['PLANNED', 'ACTIVE', 'ARCHIVED'],
  ACTIVE: ['ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'],
  ON_HOLD: ['ON_HOLD', 'ACTIVE', 'ARCHIVED'],
  COMPLETED: ['COMPLETED', 'ARCHIVED'],
  ARCHIVED: ['ARCHIVED', 'PLANNED', 'ACTIVE'],
}

const statusSelectClasses: Record<ProjectStatus, string> = {
  PLANNED: '!bg-violet-soft !text-violet',
  ACTIVE: '!bg-green-soft !text-green',
  ON_HOLD: '!bg-orange-soft !text-orange',
  COMPLETED: '!bg-green-soft !text-green',
  ARCHIVED: '!bg-red-soft !text-destructive',
}

interface ProjectCardProps {
  project: Project
  clientName: string
  changing: boolean
  onEdit: (project: Project) => void
  onStatusChange: (project: Project, status: ProjectStatus) => void
}

function getBudgetColor(percent: number) {
  if (percent >= 100) return 'var(--red)'
  if (percent >= 80) return 'var(--orange)'
  return 'var(--green)'
}

function formatHours(hours: number | null) {
  if (hours === null || hours === undefined) return '—'
  return `${Number(hours).toLocaleString('th-TH', { maximumFractionDigits: 2 })} ชม.`
}

function isPastEndDate(endDate: string) {
  if (!endDate) return false
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return new Date(`${endDate}T00:00:00`) < today
}

export default function ProjectCard({ project, clientName, changing, onEdit, onStatusChange }: ProjectCardProps) {
  const totalTasks = project.task_progress?.total_tasks ?? 0
  const completedTasks = project.task_progress?.completed_tasks ?? 0
  const taskPercent = Math.max(0, Math.min(100, Math.round(project.task_progress?.percent ?? 0)))
  const trackedSeconds = project.time_tracking?.tracked_seconds ?? null
  const budgetPercent = project.time_tracking?.usage_percent ?? null
  const budgetColor = budgetPercent === null ? undefined : getBudgetColor(budgetPercent)
  const hasIncompleteTasks = totalTasks > completedTasks
  const isOverdue = hasIncompleteTasks && isPastEndDate(project.end_date)

  return (
    <Card asChild>
      <article
        className="relative min-w-0 border-l-4 p-5 transition-[transform,box-shadow] duration-150 hover:-translate-y-0.5 hover:shadow-lg"
        style={{ borderLeftColor: project.color }}
      >
        <Link className="absolute inset-0 z-0 rounded-xl" to={`/projects/${project.id}`} aria-label={`เปิด ${project.name}`} />

        <div className="pointer-events-none relative z-10 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="truncate text-lg font-semibold text-text-primary">{project.name}</h2>
            <p className="mt-0.5 truncate text-sm text-text-secondary">{clientName} · {totalTasks} งาน</p>
            <p className={`mt-0.5 text-sm ${isOverdue ? 'font-semibold text-destructive' : 'text-text-secondary'}`}>
              {project.end_date ? `สิ้นสุด ${formatDate(project.end_date)}` : 'ไม่กำหนดวันสิ้นสุด'}
            </p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="pointer-events-auto shrink-0 text-muted-foreground hover:bg-muted hover:text-text-primary"
            type="button"
            onClick={() => onEdit(project)}
          >
            <FiEdit2 aria-hidden="true" />
            แก้ไข
          </Button>
        </div>

        <div className="pointer-events-none relative z-10 grid grid-cols-2 gap-4 pt-2">
          <div className="grid gap-0.5">
            <strong className="text-xl font-semibold leading-tight text-text-primary tabular-nums">{trackedSeconds === null ? '—' : formatTimer(trackedSeconds)}</strong>
            <span className="text-sm text-text-secondary">เวลาที่บันทึก</span>
          </div>
          <div className="grid gap-0.5">
            <strong className="text-xl font-semibold leading-tight text-text-primary tabular-nums">{formatHours(project.budget_hours)}</strong>
            <span className="text-sm text-text-secondary">จำนวนชั่วโมง</span>
          </div>
        </div>

        <div className="pointer-events-none relative z-10 grid gap-2 pt-2">
          <div className={`flex items-center justify-between gap-3 text-sm ${isOverdue ? 'font-semibold text-destructive' : 'text-text-secondary'}`}>
            <span>ความคืบหน้างาน ({completedTasks}/{totalTasks})</span>
            <strong className={isOverdue ? 'text-destructive' : 'text-text-primary'}>{taskPercent}%</strong>
          </div>
          <Progress className="h-2 bg-border" value={taskPercent} indicatorColor={isOverdue ? 'var(--red)' : project.color} />
        </div>

        {budgetPercent !== null && (
          <div className="pointer-events-none relative z-10 grid gap-2 pt-2">
            <div className="flex items-center justify-between gap-3 text-sm text-text-secondary">
              <span>ใช้เวลาไปแล้ว</span>
              <strong className="text-text-primary">{budgetPercent}%</strong>
            </div>
            <Progress className="h-2 bg-border" value={budgetPercent} indicatorColor={budgetColor} />
          </div>
        )}

        <div className="relative z-10 mt-5 pointer-events-auto">
          <NativeSelect
            size="sm"
            className={`h-9 w-fit min-w-36 rounded-full border-0 px-3 py-1.5 text-sm font-semibold ${statusSelectClasses[project.status]}`}
            wrapperClassName="w-fit"
            value={project.status}
            aria-label={`สถานะของโปรเจกต์ ${project.name}`}
            disabled={changing}
            onChange={(event) => onStatusChange(project, event.target.value as ProjectStatus)}
          >
            {allowedStatusTransitions[project.status].map((status) => (
              <option key={status} value={status}>{projectStatusLabels[status]}</option>
            ))}
          </NativeSelect>
        </div>
      </article>
    </Card>
  )
}
