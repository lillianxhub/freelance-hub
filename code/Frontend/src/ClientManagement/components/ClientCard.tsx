import { Card } from '../../components/ui/card'
import { Button } from '../../components/ui/button'
import { Link } from 'react-router-dom'
import type { ClientCardProps } from '../../types/clientsPage'
import { formatDuration, formatMoney, initials } from '../../utils/formatters'
import ClientStatus from './ClientStatus'

export default function ClientCard({ client, projectCount, minutes, revenue, onEdit, onArchive }: ClientCardProps) {
  const displayName = client.company_name || client.name
  return (
    <Card asChild><article className="client-card">
      <Link className="card-link" to={`/clients/${client.id}`} aria-label={`เปิด ${displayName}`} />
      <div className="card-top">
        <span className="client-avatar" style={{ background: client.color }}>{initials(displayName)}</span>
        <div className="card-menu">
          <Button variant="ghost" className="mini-button" type="button" onClick={() => onEdit(client)}>แก้ไข</Button>
          <Button variant="ghost" className="mini-button" type="button" onClick={() => onArchive(client)}>{client.status === 'ARCHIVED' ? 'นำกลับ' : 'เก็บถาวร'}</Button>
        </div>
      </div>
      <h2>{displayName}</h2>
      <p>{client.email || 'ยังไม่มีอีเมล'}</p>
      <div className="client-stats"><span><strong>{projectCount}</strong>โปรเจกต์</span><span><strong>{formatDuration(minutes)}</strong>เวลารวม</span></div>
      <div className="card-footer"><ClientStatus status={client.status} /><span>{formatMoney(revenue)}</span></div>
    </article></Card>
  )
}
