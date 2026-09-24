const statusLabels = {
  ACTIVE: 'กำลังดำเนินการ', ARCHIVED: 'เก็บถาวร', PLANNED: 'วางแผน', ON_HOLD: 'พักงาน', COMPLETED: 'เสร็จสิ้น',
  TODO: 'รอดำเนินการ', IN_PROGRESS: 'กำลังทำ', IN_REVIEW: 'ตรวจสอบ', DONE: 'เสร็จแล้ว',
  DRAFT: 'ฉบับร่าง', ISSUED: 'ออกแล้ว', PAID: 'ชำระแล้ว', OVERDUE: 'เกินกำหนด', VOID: 'ยกเลิก',
}

function StatusBadge({ status }) {
  return <span className={`status-badge status-${String(status).toLowerCase()}`}>{statusLabels[status] || status}</span>
}

export default StatusBadge
