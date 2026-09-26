import type { TaskListProps } from '../../types/projectDetailPage'
import { formatDate } from '../../utils/formatters'
import StatusBadge from '../../components/StatusBadge'

export default function TaskList({ tasks, onToggle, onMove, onEdit, onDelete }: TaskListProps) {
  return <div className="task-list">
    {tasks.map((task, index) => <article className="task-row" key={task.id}>
      <button className={`task-check${task.status === 'DONE' ? ' checked' : ''}`} type="button" aria-label="สลับStatusTask" onClick={() => onToggle(task)}>{task.status === 'DONE' ? '✓' : ''}</button>
      <div className="task-copy"><strong>{task.name}</strong><small>{task.due_date ? `ครบกำหนด ${formatDate(task.due_date)}` : 'ไม่มีกำหนดส่ง'}</small></div>
      <StatusBadge status={task.status} />
      <div className="task-actions"><button type="button" disabled={index === 0} onClick={() => onMove(task, -1)}>↑</button><button type="button" disabled={index === tasks.length - 1} onClick={() => onMove(task, 1)}>↓</button><button type="button" onClick={() => onEdit(task)}>แก้ไข</button><button type="button" onClick={() => onDelete(task)}>ลบ</button></div>
    </article>)}
    {tasks.length === 0 && <p className="inline-empty">ยังไม่มี งาน ในโปรเจกต์นี้</p>}
  </div>
}
