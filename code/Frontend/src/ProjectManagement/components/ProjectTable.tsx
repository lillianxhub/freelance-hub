import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../components/ui/table'
import { Link } from 'react-router-dom'
import type { ProjectTableProps } from '../../types/projectsPage'
import ProjectStatus from './ProjectStatus'

export default function ProjectsTable({ projects, clients }: ProjectTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>โปรเจกต์</TableHead>
          <TableHead>ลูกค้า</TableHead>
          <TableHead>สถานะ</TableHead>
          <TableHead>รูปแบบราคา</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {projects.map((project) => {
          const client = clients.find((item) => item.id === project.client_id)

          return (
            <TableRow key={project.id}>
              <TableCell>
                <Link className="font-medium text-primary hover:underline" to={`/projects/${project.id}`}>
                  {project.name}
                </Link>
              </TableCell>
              <TableCell>{client?.company_name || client?.name || '—'}</TableCell>
              <TableCell><ProjectStatus status={project.status} /></TableCell>
              <TableCell>{project.billing_type === 'HOURLY' ? 'รายชั่วโมง' : 'เหมาจ่าย'}</TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
