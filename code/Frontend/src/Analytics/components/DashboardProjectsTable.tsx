import { Link } from 'react-router-dom'
import { FiArrowRight } from 'react-icons/fi'
import StatusBadge from '../../components/StatusBadge'
import { Button } from '../../components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card'
import { Progress } from '../../components/ui/progress'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table'
import { formatDate, formatDuration } from '../../lib/formatters'
import type { Client } from '../../types/client'
import type { Project } from '../../types/project'
import type { Task } from '../../types/task'
import type { TimeEntry } from '../../types/timeTracking'
import { DASHBOARD_LIMITS } from '../dashboard.constants'

interface Props {
  projects: readonly Project[]
  clients: readonly Client[]
  tasks: readonly Task[]
  entries: readonly TimeEntry[]
}

export default function DashboardProjectsTable({ projects, clients, tasks, entries }: Props) {
  const rows = projects.filter((project) => project.status === 'ACTIVE').slice(0, DASHBOARD_LIMITS.activeProjects)

  return (
    <Card>
      <CardHeader>
        <CardTitle>โปรเจกต์ที่กำลังดำเนินการ</CardTitle>
        <CardDescription>ติดตามความคืบหน้า เวลา และกำหนดส่ง</CardDescription>
        <CardAction>
          <Button asChild variant="ghost" size="sm">
            <Link to="/projects">ดูทั้งหมด <FiArrowRight aria-hidden="true" /></Link>
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        {rows.length ? (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="!bg-transparent">โปรเจกต์</TableHead>
                <TableHead className="!bg-transparent">ความคืบหน้า</TableHead>
                <TableHead className="!bg-transparent">เวลาที่ใช้</TableHead>
                <TableHead className="!bg-transparent">กำหนดส่ง</TableHead>
                <TableHead className="!bg-transparent">สถานะ</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((project) => {
                const projectTasks = tasks.filter((task) => task.project_id === project.id)
                const completed = projectTasks.filter((task) => task.status === 'DONE').length
                const progress = projectTasks.length ? Math.round((completed / projectTasks.length) * 100) : 0
                const minutes = entries.filter((entry) => entry.project_id === project.id && entry.ended_at).reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
                const client = clients.find((item) => item.id === project.client_id)
                return (
                  <TableRow key={project.id}>
                    <TableCell>
                      <Link className="flex items-center gap-3 font-semibold text-text-primary hover:text-primary" to={`/projects/${project.id}`}>
                        <span className="size-2.5 rounded-full" style={{ backgroundColor: project.color }} />
                        <span><span className="block">{project.name}</span><span className="block text-xs font-normal text-muted-foreground">{client?.company_name || client?.name || 'ไม่ระบุลูกค้า'}</span></span>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <div className="flex min-w-32 items-center gap-3"><Progress value={progress} indicatorColor={project.color} /><span className="text-xs tabular-nums text-muted-foreground">{progress}%</span></div>
                    </TableCell>
                    <TableCell className="font-medium tabular-nums">{formatDuration(minutes)}</TableCell>
                    <TableCell>{project.end_date ? formatDate(project.end_date) : 'ยังไม่กำหนด'}</TableCell>
                    <TableCell><StatusBadge status={project.status} /></TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        ) : <p className="py-10 text-center text-sm text-muted-foreground">ยังไม่มีโปรเจกต์ที่กำลังดำเนินการ</p>}
      </CardContent>
    </Card>
  )
}
