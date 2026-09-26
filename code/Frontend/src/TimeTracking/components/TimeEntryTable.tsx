import type { TimeEntryTableProps } from '../../types/timeTrackerPage'
import { calculateTimeValue, formatDate, formatDuration, formatMoney } from '../../utils/formatters'

export default function TimeEntryTable({ entries, projects, tasks, onEdit, onDelete, onDuplicate }: TimeEntryTableProps) {
  return <div className="table-wrap"><table className="data-table"><thead><tr><th>โปรเจกต์ / งาน</th><th>วันที่</th><th>ระยะเวลา</th><th>ประเภท</th><th>มูลค่า</th><th>ใบแจ้งหนี้</th><th /></tr></thead><tbody>
    {entries.map((entry) => {
      const project = projects.find((item) => item.id === entry.project_id)
      const task = tasks.find((item) => item.id === entry.task_id)
      return <tr key={entry.id}><td><div className="table-primary"><span className="color-dot" style={{ '--dot-color': project?.color }} /><span><strong>{project?.name}</strong><small>{task?.name || entry.description || 'ไม่ระบุงาน'}</small></span></div></td><td>{formatDate(entry.started_at)}</td><td><strong>{formatDuration(entry.duration_minutes)}</strong></td><td><span className={`type-badge ${entry.billable ? 'billable' : ''}`}>{entry.billable ? 'billable' : 'ไม่billable'}</span></td><td>{formatMoney(calculateTimeValue(entry), entry.currency)}</td><td>{entry.invoice_id ? 'Invoicesd' : 'ยังไม่วางบิล'}</td><td><div className="table-actions">{!entry.invoice_id && <><button className="mini-button" type="button" onClick={() => onEdit(entry)}>แก้ไข</button><button className="mini-button" type="button" onClick={() => onDelete(entry)}>ลบ</button></>}<button className="mini-button" type="button" onClick={() => onDuplicate(entry)}>คัดลอก</button></div></td></tr>
    })}
  </tbody></table></div>
}
