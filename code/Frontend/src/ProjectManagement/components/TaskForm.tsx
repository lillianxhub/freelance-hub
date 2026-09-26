import type { TaskStatus } from '../../types/task'
import type { TaskFormProps } from '../../types/projectDetailPage'

export default function TaskForm({ value, error, onChange, onSubmit, onCancel }: TaskFormProps) {
  return <form onSubmit={onSubmit}>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field full"><label htmlFor="task-name">ชื่องาน</label><input id="task-name" value={value.name} onChange={(event) => onChange({ ...value, name: event.target.value })} required /></div>
      <div className="form-field"><label htmlFor="task-status">สถานะ</label><select id="task-status" value={value.status} onChange={(event) => onChange({ ...value, status: event.target.value as TaskStatus })}><option value="TODO">รอดำเนินการ</option><option value="IN_PROGRESS">กำลังทำ</option><option value="IN_REVIEW">ตรวจสอบ</option><option value="DONE">เสร็จแล้ว</option></select></div>
      <div className="form-field"><label htmlFor="task-due">กำหนดส่ง</label><input id="task-due" type="date" value={value.due_date || ''} onChange={(event) => onChange({ ...value, due_date: event.target.value })} /></div>
      <div className="form-field full"><label htmlFor="task-description">รายละเอียด</label><textarea id="task-description" value={value.description} onChange={(event) => onChange({ ...value, description: event.target.value })} /></div>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit">บันทึก Task</button></div>
  </form>
}
