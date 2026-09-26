import { Link } from 'react-router-dom'
import type { ProjectTableProps } from '../../types/projectsPage'
import ProjectStatus from './ProjectStatus'

export default function ProjectTable({ projects, clients }: ProjectTableProps) {
  return <div className="table-wrap"><table className="data-table"><thead><tr><th>โปรเจกต์</th><th>ลูกค้า</th><th>สถานะ</th><th>รูปแบบราคา</th></tr></thead><tbody>
    {projects.map((project) => {
      const client = clients.find((item) => item.id === project.client_id)
      return <tr key={project.id}><td><Link to={`/projects/${project.id}`}>{project.name}</Link></td><td>{client?.company_name || client?.name || '—'}</td><td><ProjectStatus status={project.status} /></td><td>{project.billing_type === 'HOURLY' ? 'รายชั่วโมง' : 'เหมาจ่าย'}</td></tr>
    })}
  </tbody></table></div>
}
