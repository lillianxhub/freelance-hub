import { formatTimer } from '../../utils/formatters'
import { useTimer } from '../useTimer'

export default function TimerPanel() {
  const timer = useTimer()
  return <section className="panel big-timer-card">
    <span className={`timer-pill${timer.runningEntry ? ' live' : ''}`}><i />{timer.runningEntry ? 'Timer กำลังทำTask' : 'พร้อมเริ่มTask'}</span>
    <div className="big-timer">{formatTimer(timer.elapsedSeconds)}</div>
    {timer.runningEntry ? <>
      <div className="running-project"><span className="color-dot" style={{ '--dot-color': timer.runningProject?.color }} /><span><strong>{timer.runningProject?.name}</strong><small>{timer.runningTask?.name || timer.runningEntry.description || 'ไม่ระบุงาน'}</small></span></div>
      <div className="timer-button-row"><button className="button button-danger wide" type="button" onClick={timer.stopTimer}>■ หยุดและบันทึก</button><button className="button button-secondary" type="button" onClick={timer.cancelTimer}>ยกเลิก</button></div>
    </> : <>
      <div className="timer-selects">
        <div className="form-field"><label htmlFor="timer-project">โปรเจกต์</label><select id="timer-project" value={timer.timerProjectId} onChange={(event) => { timer.setSelectedProject(event.target.value); timer.setSelectedTask('') }}>{timer.activeProjects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
        <div className="form-field"><label htmlFor="timer-task">งาน</label><select id="timer-task" value={timer.selectedTask} onChange={(event) => timer.setSelectedTask(event.target.value)}><option value="">ไม่ระบุงาน</option>{timer.selectedTasks.map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select></div>
      </div>
      <div className="form-field"><label htmlFor="timer-description">คำอธิบาย</label><input id="timer-description" value={timer.description} onChange={(event) => timer.setDescription(event.target.value)} placeholder="กำลังทำอะไรอยู่?" /></div>
      <label className="check-field timer-billable"><input type="checkbox" checked={timer.billable} onChange={(event) => timer.setBillable(event.target.checked)} /><span>เวลาที่คิดค่าบริการ</span></label>
      <button className="button button-primary wide" type="button" disabled={!timer.timerProjectId} onClick={timer.startTimer}>▶ เริ่มจับเวลา</button>
    </>}
  </section>
}
