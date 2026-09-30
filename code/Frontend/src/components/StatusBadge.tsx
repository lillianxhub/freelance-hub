import type { StatusBadgeProps, SupportedStatus } from '../types/ui'
import { Badge } from './ui/badge'

const statusLabels = {
  ACTIVE: 'กำลังดำเนินการ', ARCHIVED: 'เก็บถาวร', PLANNED: 'วางแผน', ON_HOLD: 'พักงาน', COMPLETED: 'เสร็จสิ้น',
  TODO: 'รอดำเนินการ', IN_PROGRESS: 'กำลังทำ', IN_REVIEW: 'ตรวจสอบ', DONE: 'เสร็จแล้ว',
  DRAFT: 'ฉบับร่าง', ISSUED: 'ออกแล้ว', PAID: 'ชำระแล้ว', OVERDUE: 'เกินกำหนด', VOID: 'ยกเลิก',
} satisfies Record<SupportedStatus, string>

function StatusBadge({ status }: StatusBadgeProps) {
  return <Badge variant="secondary" className={`status-badge h-auto min-h-5 status-${status.toLowerCase()}`}>{statusLabels[status]}</Badge>
}

export default StatusBadge
