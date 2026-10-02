import { Link } from 'react-router-dom'
import { FiArrowRight } from 'react-icons/fi'
import StatusBadge from '../../components/StatusBadge'
import { Button } from '../../components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card'
import { Progress } from '../../components/ui/progress'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table'
import type { DashboardActiveProject } from '../../types/dashboard'
import { DASHBOARD_LIMITS } from '../dashboard.constants'

interface Props {
  projects: readonly DashboardActiveProject[]
}

export default function DashboardProjectsTable({ projects }: Props) {
  const rows = projects.slice(0, DASHBOARD_LIMITS.activeProjects)

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
                <TableHead className="!bg-transparent">งาน</TableHead>
                <TableHead className="!bg-transparent">สถานะ</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((project) => {
                const progress = Math.round(project.taskProgressPercent)
                return (
                  <TableRow key={project.id}>
                    <TableCell>
                      <Link className="flex items-center gap-3 font-semibold text-text-primary hover:text-primary" to={`/projects/${project.id}`}>
                        <span className="size-2.5 rounded-full" style={{ backgroundColor: project.color }} />
                        <span><span className="block">{project.name}</span><span className="block text-xs font-normal text-muted-foreground">{project.clientName || 'ไม่ระบุลูกค้า'}</span></span>
                      </Link>
                    </TableCell>
                    <TableCell>
                      <div className="flex min-w-32 items-center gap-3"><Progress value={progress} indicatorColor={project.color || undefined} /><span className="text-xs tabular-nums text-muted-foreground">{progress}%</span></div>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">{project.completedTaskCount}/{project.totalTaskCount} งาน</TableCell>
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
