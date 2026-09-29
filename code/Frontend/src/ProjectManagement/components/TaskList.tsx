import type { TaskListProps } from '../../types/projectDetailPage'
import { formatDate } from '../../utils/formatters'
import StatusBadge from '../../components/StatusBadge'
import { FiArrowDown, FiArrowUp, FiCheck, FiEdit2, FiTrash2 } from 'react-icons/fi'

export default function TaskList({ tasks, onToggle, onMove, onEdit, onDelete }: TaskListProps) {
  return <div className="task-list">
    {tasks.map((task, index) => <article className="task-row" key={task.id}>
      <button className={`task-check${task.status === 'DONE' ? ' checked' : ''}`} type="button" aria-label="สลับStatusTask" onClick={() => onToggle(task)}>{task.status === 'DONE' && <FiCheck aria-hidden="true" />}</button>
      <div className="task-copy"><strong>{task.name}</strong><small>{task.due_date ? `ครบกำหนด ${formatDate(task.due_date)}` : 'ไม่มีกำหนดส่ง'}</small></div>
      <StatusBadge status={task.status} />
      <div className="task-actions"><button type="button" aria-label="เลื่อนงานขึ้น" disabled={index === 0} onClick={() => onMove(task, -1)}><FiArrowUp aria-hidden="true" /></button><button type="button" aria-label="เลื่อนงานลง" disabled={index === tasks.length - 1} onClick={() => onMove(task, 1)}><FiArrowDown aria-hidden="true" /></button><button type="button" aria-label="แก้ไขงาน" onClick={() => onEdit(task)}><FiEdit2 aria-hidden="true" /></button><button type="button" aria-label="ลบงาน" onClick={() => onDelete(task)}><FiTrash2 aria-hidden="true" /></button></div>
    </article>)}
    {tasks.length === 0 && <p className="inline-empty">ยังไม่มี งาน ในโปรเจกต์นี้</p>}
  </div>
}
