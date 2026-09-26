import type { TimeEntryFormProps } from '../../types/timeTrackerPage'

export default function TimeEntryForm({ value, projects, tasks, error, onChange, onSubmit, onCancel }: TimeEntryFormProps) {
  return <form onSubmit={onSubmit}>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field"><label htmlFor="manual-project">โปรเจกต์</label><select id="manual-project" name="project_id" value={value.project_id} onChange={onChange} required>{projects.filter((project) => project.status !== 'ARCHIVED').map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
      <div className="form-field"><label htmlFor="manual-task">งาน</label><select id="manual-task" name="task_id" value={value.task_id || ''} onChange={onChange}><option value="">ไม่ระบุ</option>{tasks.filter((task) => task.project_id === value.project_id).map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select></div>
      <div className="form-field full"><label htmlFor="manual-description">คำอธิบาย</label><input id="manual-description" name="description" value={value.description} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="manual-date">วันที่</label><input id="manual-date" name="entry_date" type="date" value={value.entry_date} onChange={onChange} required /></div>
      <div className="form-field"><label htmlFor="manual-mode">วิธีระบุเวลา</label><select id="manual-mode" name="manual_mode" value={value.manual_mode} onChange={onChange}><option value="RANGE">เวลาเริ่ม–สิ้นสุด</option><option value="DURATION">ระยะเวลา</option></select></div>
      <div className="form-field"><label htmlFor="manual-start">เวลาเริ่ม</label><input id="manual-start" name="start_time" type="time" value={value.start_time} onChange={onChange} required /></div>
      {value.manual_mode === 'RANGE' ? <div className="form-field"><label htmlFor="manual-end">เวลาสิ้นสุด</label><input id="manual-end" name="end_time" type="time" value={value.end_time} onChange={onChange} required /></div> : <div className="form-field"><label htmlFor="manual-duration">ระยะเวลา (นาที)</label><input id="manual-duration" name="duration_minutes" type="number" min="1" value={value.duration_minutes} onChange={onChange} required /></div>}
      <div className="form-field"><label htmlFor="manual-rate">อัตราต่อชั่วโมง (เว้นว่างเพื่อใช้ อัตราของโปรเจกต์)</label><input id="manual-rate" name="rate_snapshot" type="number" min="0" step="0.01" value={value.rate_snapshot} onChange={onChange} /></div>
      <label className="check-field"><input name="billable" type="checkbox" checked={value.billable} onChange={onChange} /><span>รายการนี้เรียกเก็บเงินได้</span></label>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit">บันทึกรายการ</button></div>
  </form>
}
