import type { ClientStatus, InvoiceStatus, ProjectStatus, TaskStatus } from '../types/domain'

type SupportedStatus = ClientStatus | ProjectStatus | TaskStatus | InvoiceStatus

const statusLabels = {
  ACTIVE: 'กำลังดำเนินการ', ARCHIVED: 'เก็บถาวร', PLANNED: 'วางแผน', ON_HOLD: 'พักงาน', COMPLETED: 'เสร็จสิ้น',
  TODO: 'รอดำเนินการ', IN_PROGRESS: 'กำลังทำ', IN_REVIEW: 'ตรวจสอบ', DONE: 'เสร็จแล้ว',
  DRAFT: 'ฉบับร่าง', ISSUED: 'ออกแล้ว', PAID: 'ชำระแล้ว', OVERDUE: 'เกินกำหนด', VOID: 'ยกเลิก',
} satisfies Record<SupportedStatus, string>

function StatusBadge({ status }: { status: SupportedStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{statusLabels[status]}</span>
}

export default StatusBadge
