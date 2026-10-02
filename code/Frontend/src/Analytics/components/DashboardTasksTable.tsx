import { Link } from 'react-router-dom'
import { FiArrowRight } from 'react-icons/fi'
import StatusBadge from '../../components/StatusBadge'
import { Button } from '../../components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table'
import type { DashboardOpenTask } from '../../types/dashboard'
import { DASHBOARD_LIMITS } from '../dashboard.constants'

interface Props { tasks: readonly DashboardOpenTask[] }

export default function DashboardTasksTable({ tasks }: Props) {
  const rows = tasks.filter((task) => task.status !== 'COMPLETED').slice(0, DASHBOARD_LIMITS.pendingTasks)
  return (
    <Card>
      <CardHeader>
        <CardTitle>งานที่ต้องทำต่อ</CardTitle>
        <CardDescription>เรียงตามกำหนดส่งที่ใกล้ที่สุด</CardDescription>
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
                <TableHead className="!bg-transparent">งาน</TableHead>
                <TableHead className="!bg-transparent">โปรเจกต์</TableHead>
                {/* <TableHead className="!bg-transparent">กำหนดส่ง</TableHead> */}
                <TableHead className="!bg-transparent">สถานะ</TableHead>
                </TableRow>
                </TableHeader>
            <TableBody>{rows.map((task, index) => {
              return <TableRow key={task.id}>
                <TableCell>
                  <Link className="flex items-center gap-3 font-semibold text-text-primary hover:text-primary" to={`/projects/${task.projectId}`}>
                  <span className="grid size-7 place-items-center rounded-full bg-secondary text-xs text-primary">{index + 1}</span>{task.name}</Link>
                  </TableCell>
                <TableCell>{task.projectName || 'ไม่ระบุโปรเจกต์'}</TableCell>
                {/* <TableCell>{task.due_date ? formatDate(task.due_date) : 'ยังไม่กำหนด'}</TableCell> */}
                <TableCell><StatusBadge status={task.status === 'OPEN' ? 'TODO' : task.status === 'COMPLETED' ? 'DONE' : 'IN_PROGRESS'} /></TableCell>
              </TableRow>
            })}</TableBody>
          </Table>
        ) : <p className="py-10 text-center text-sm text-muted-foreground">ไม่มีงานค้างอยู่</p>}
      </CardContent>
    </Card>
  )
}
