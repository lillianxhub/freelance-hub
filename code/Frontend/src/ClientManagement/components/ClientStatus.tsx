import type { ClientStatusProps } from '../../types/clientsPage'
import StatusBadge from '../../components/StatusBadge'

export default function ClientStatus({ status }: ClientStatusProps) {
  return <StatusBadge status={status} />
}
