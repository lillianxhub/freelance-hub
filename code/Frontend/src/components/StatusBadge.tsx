import type { StatusBadgeProps, SupportedStatus } from '../types/ui'
import { Badge } from './ui/badge'
import { cn } from 'cn'

const statusLabels = {
  ACTIVE: 'กำลังดำเนินการ',
  ARCHIVED: 'เก็บถาวร',
  PLANNED: 'วางแผน',
  ON_HOLD: 'พักงาน',
  COMPLETED: 'เสร็จแล้ว',
  OPEN: 'รอดำเนินการ',
  IN_PROGRESS: 'กำลังดำเนินการ',
} satisfies Record<SupportedStatus, string>

const statusClasses = {
  ACTIVE: 'bg-primary-soft text-primary-dark',
  IN_PROGRESS: 'bg-primary-soft text-primary-dark',
  COMPLETED: 'bg-green-soft text-green',
  PLANNED: 'bg-violet-soft text-violet',
  OPEN: 'bg-violet-soft text-violet',
  ON_HOLD: 'bg-orange-soft text-orange',
  ARCHIVED: 'bg-surface-soft text-text-secondary',
} satisfies Record<SupportedStatus, string>

function StatusBadge({ status, label, className }: StatusBadgeProps) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        'h-auto min-h-5 rounded-full border-0 px-2 py-1 text-xs font-semibold',
        statusClasses[status],
        className,
      )}
    >
      {label || statusLabels[status]}
    </Badge>
  )
}

export default StatusBadge
