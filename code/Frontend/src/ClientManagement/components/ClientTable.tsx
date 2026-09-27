import { Link } from 'react-router-dom'
import type { ClientTableProps } from '../../types/clientsPage'
import ClientStatus from './ClientStatus'

export default function ClientsTable({ clients }: ClientTableProps) {
  return <div className="table-wrap"><table className="data-table"><thead><tr><th>ลูกค้า</th><th>อีเมล</th><th>โทรศัพท์</th><th>สถานะ</th></tr></thead><tbody>
    {clients.map((client) => <tr key={client.id}><td><Link to={`/clients/${client.id}`}>{client.company_name || client.name}</Link></td><td>{client.email || '—'}</td><td>{client.phone || '—'}</td><td><ClientStatus status={client.status} /></td></tr>)}
  </tbody></table></div>
}
