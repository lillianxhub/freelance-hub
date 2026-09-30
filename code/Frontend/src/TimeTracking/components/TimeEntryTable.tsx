import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../components/ui/table'
import { Button } from '../../components/ui/button'
import type { TimeEntryTableProps } from '../../types/timeTrackerPage'
import { formatDate, formatDurationSeconds } from '../../utils/formatters'

export default function TimeEntryTable({ entries, projects, tasks = [], pagination, onEdit, onDelete, onDuplicate }: TimeEntryTableProps) {
  return <div className="table-wrap"><Table className="data-table" pagination={pagination}><TableHeader><TableRow><TableHead>โปรเจกต์ / งาน</TableHead><TableHead>วันที่</TableHead><TableHead>ระยะเวลา</TableHead><TableHead /></TableRow></TableHeader><TableBody>
    {entries.map((entry) => {
      const project = projects.find((item) => item.id === entry.project_id)
      const task = tasks.find((item) => item.id === entry.task_id)
      return <TableRow key={entry.id}><TableCell><div className="table-primary"><span className="color-dot" style={{ '--dot-color': project?.color }} /><span><strong>{project?.name}</strong><small>{task?.name || entry.task_name || entry.description || 'ไม่ระบุงาน'}</small></span></div></TableCell><TableCell>{formatDate(entry.started_at)}</TableCell><TableCell><strong>{formatDurationSeconds(entry.duration_seconds ?? (entry.duration_minutes ?? 0) * 60)}</strong></TableCell>{/* <td><span className={`type-badge ${entry.billable ? 'billable' : ''}`}>{entry.billable ? 'billable' : 'ไม่billable'}</span></td><td>{formatMoney(calculateTimeValue(entry), entry.currency)}</td><td>{entry.invoice_id ? 'Invoicesd' : 'ยังไม่วางบิล'}</td> */}<TableCell><div className="table-actions"><Button variant="ghost" className="mini-button" type="button" onClick={() => onEdit(entry)}>แก้ไข</Button><Button variant="ghost" className="mini-button" type="button" onClick={() => onDelete(entry)}>ลบ</Button><Button variant="ghost" className="mini-button" type="button" onClick={() => onDuplicate(entry)}>คัดลอก</Button></div></TableCell></TableRow>
    })}
  </TableBody></Table></div>
}
