import type { ClientStatusProps } from '../../types/clientsPage'
import StatusBadge from '../../components/StatusBadge'

export default function ClientStatus({ status }: ClientStatusProps) {
  return (
    <StatusBadge
      status={status}
      label={status === 'ACTIVE' ? 'กำลังใช้งานอยู่' : 'เก็บถาวร'}
      className={status === 'ACTIVE' ? '!bg-green-soft !text-green' : '!bg-red-soft !text-destructive'}
    />
  )
}
