import { useEffect, useMemo, useState } from 'react'
import Modal from '../components/Modal'
import PageHeader from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { calculateTimeValue, formatDate, formatDuration, formatMoney, formatTimer } from '../utils/formatters'

function localDateValue(date = new Date()) {
  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return adjusted.toISOString().slice(0, 10)
}

const createEmptyManualForm = (projectId = '') => ({
  project_id: projectId, task_id: '', description: '', entry_date: localDateValue(), start_time: '09:00', end_time: '10:00',
  manual_mode: 'RANGE', duration_minutes: 60, billable: true, rate_snapshot: '', currency: 'THB',
})

function TimeTrackerPage() {
  const { data, loading, error, refresh, save, remove } = useWorkspace()
  const [selectedProject, setSelectedProject] = useState('')
  const [selectedTask, setSelectedTask] = useState('')
  const [description, setDescription] = useState('')
  const [billable, setBillable] = useState(true)
  const [tick, setTick] = useState(0)
  const [manualOpen, setManualOpen] = useState(false)
  const [manualForm, setManualForm] = useState(createEmptyManualForm())
  const [formError, setFormError] = useState('')
  const [filters, setFilters] = useState({ client: 'ALL', project: 'ALL', task: 'ALL', billable: 'ALL', invoice: 'ALL', from: '', to: '' })

  const activeProjects = useMemo(() => (data?.projects || []).filter((project) => project.status === 'ACTIVE'), [data?.projects])
  const runningEntry = data?.time_entries?.find((entry) => !entry.ended_at) || null
  const timerProjectId = selectedProject || activeProjects[0]?.id || ''

  useEffect(() => {
    if (!runningEntry) return undefined
    const interval = window.setInterval(() => setTick(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [runningEntry])

  if (loading) return <LoadingState label="กำลังโหลดรายการเวลา..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const entries = data.time_entries
    .filter((entry) => entry.ended_at)
    .filter((entry) => filters.client === 'ALL' || data.projects.find((project) => project.id === entry.project_id)?.client_id === filters.client)
    .filter((entry) => filters.project === 'ALL' || entry.project_id === filters.project)
    .filter((entry) => filters.task === 'ALL' || entry.task_id === filters.task)
    .filter((entry) => filters.billable === 'ALL' || String(entry.billable) === filters.billable)
    .filter((entry) => filters.invoice === 'ALL' || (filters.invoice === 'INVOICED' ? entry.invoice_id : !entry.invoice_id))
    .filter((entry) => !filters.from || entry.started_at.slice(0, 10) >= filters.from)
    .filter((entry) => !filters.to || entry.started_at.slice(0, 10) <= filters.to)
    .sort((a, b) => new Date(b.started_at) - new Date(a.started_at))

  const selectedProjectData = data.projects.find((project) => project.id === timerProjectId)
  const selectedTasks = data.tasks.filter((task) => task.project_id === timerProjectId && task.status !== 'DONE')
  const runningProject = runningEntry && data.projects.find((project) => project.id === runningEntry.project_id)
  const runningTask = runningEntry && data.tasks.find((task) => task.id === runningEntry.task_id)
  const elapsedSeconds = runningEntry && tick ? Math.max(0, Math.floor((tick - new Date(runningEntry.started_at).getTime()) / 1000)) : 0
  const totalMinutes = entries.reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
  const billableMinutes = entries.filter((entry) => entry.billable).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
  const totalValue = entries.reduce((sum, entry) => sum + calculateTimeValue(entry), 0)

  const startTimer = async () => {
    if (!selectedProjectData || runningEntry) return
    const profile = data.profiles[0]
    await save('time_entries', {
      project_id: timerProjectId,
      task_id: selectedTask || null,
      description: description.trim(),
      started_at: new Date().toISOString(),
      ended_at: null,
      duration_minutes: null,
      billable,
      rate_snapshot: selectedProjectData.billing_type === 'HOURLY' ? (selectedProjectData.hourly_rate || profile?.default_hourly_rate || 0) : 0,
      currency: selectedProjectData.currency || profile?.currency || 'THB',
      invoice_id: null,
    })
    setDescription('')
  }

  const stopTimer = async () => {
    if (!runningEntry) return
    const endedAt = new Date()
    const duration = Math.max(1, Math.round((endedAt - new Date(runningEntry.started_at)) / 60000))
    await save('time_entries', { ...runningEntry, ended_at: endedAt.toISOString(), duration_minutes: duration })
  }

  const openManual = (entry = null) => {
    if (entry) {
      const start = new Date(entry.started_at)
      const end = new Date(entry.ended_at)
      setManualForm({
        ...entry,
        entry_date: localDateValue(start),
        start_time: start.toTimeString().slice(0, 5),
        end_time: end.toTimeString().slice(0, 5),
        manual_mode: 'RANGE',
      })
    } else {
      const projectId = activeProjects[0]?.id || ''
      setManualForm(createEmptyManualForm(projectId))
    }
    setFormError('')
    setManualOpen(true)
  }

  const handleManualChange = (event) => {
    const { name, value, type, checked } = event.target
    setManualForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value, ...(name === 'project_id' ? { task_id: '' } : {}) }))
  }

  const saveManualEntry = async (event) => {
    event.preventDefault()
    const project = data.projects.find((item) => item.id === manualForm.project_id)
    if (!project) {
      setFormError('กรุณาเลือกโปรเจกต์')
      return
    }

    const startedAt = new Date(`${manualForm.entry_date}T${manualForm.start_time}:00`)
    let endedAt
    let duration
    if (manualForm.manual_mode === 'RANGE') {
      endedAt = new Date(`${manualForm.entry_date}T${manualForm.end_time}:00`)
      duration = Math.round((endedAt - startedAt) / 60000)
    } else {
      duration = Number(manualForm.duration_minutes)
      endedAt = new Date(startedAt.getTime() + duration * 60000)
    }
    if (!Number.isFinite(duration) || duration <= 0) {
      setFormError('เวลาเริ่มต้องน้อยกว่าเวลาสิ้นสุดและระยะเวลาต้องมากกว่า 0')
      return
    }

    const profile = data.profiles[0]
    const { manual_mode: _manualMode, entry_date: _entryDate, start_time: _startTime, end_time: _endTime, ...record } = manualForm
    await save('time_entries', {
      ...record,
      task_id: manualForm.task_id || null,
      started_at: startedAt.toISOString(),
      ended_at: endedAt.toISOString(),
      duration_minutes: duration,
      rate_snapshot: project.billing_type === 'HOURLY' ? (Number(manualForm.rate_snapshot) || project.hourly_rate || profile?.default_hourly_rate || 0) : 0,
      currency: project.currency || profile?.currency || 'THB',
      invoice_id: manualForm.invoice_id || null,
    })
    setManualOpen(false)
  }

  const duplicateEntry = (entry) => {
    const duplicate = { ...entry, id: undefined, invoice_id: null, started_at: new Date().toISOString(), ended_at: new Date(Date.now() + entry.duration_minutes * 60000).toISOString() }
    save('time_entries', duplicate)
  }

  const applyRange = (preset) => {
    if (preset === 'ALL') {
      setFilters((current) => ({ ...current, from: '', to: '' }))
      return
    }
    const now = new Date()
    const local = (date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
    if (preset === 'DAY') {
      const today = local(now)
      setFilters((current) => ({ ...current, from: today, to: today }))
      return
    }
    const weekday = now.getDay() || 7
    const start = new Date(now)
    start.setDate(start.getDate() - weekday + 1)
    setFilters((current) => ({ ...current, from: local(start), to: local(now) }))
  }

  return (
    <div className="page-view">
      <PageHeader eyebrow="Workspace / Time tracker" title="Time tracker" description="เปลี่ยนเวลาทำงานให้เป็นรายการที่แม่นยำและพร้อมเรียกเก็บเงิน" actions={<button className="button button-primary" type="button" onClick={() => openManual()}>＋ เพิ่มเวลาด้วยตนเอง</button>} />

      <div className="tracker-layout">
        <section className="panel big-timer-card">
          <span className={`timer-pill${runningEntry ? ' live' : ''}`}><i />{runningEntry ? 'Timer กำลังทำงาน' : 'พร้อมเริ่มงาน'}</span>
          <div className="big-timer">{formatTimer(elapsedSeconds)}</div>
          {runningEntry ? (
            <>
              <div className="running-project"><span className="color-dot" style={{ '--dot-color': runningProject?.color }} /><span><strong>{runningProject?.name}</strong><small>{runningTask?.name || runningEntry.description || 'ไม่ระบุ Task'}</small></span></div>
              <div className="timer-button-row"><button className="button button-danger wide" type="button" onClick={stopTimer}>■ หยุดและบันทึก</button><button className="button button-secondary" type="button" onClick={() => remove('time_entries', runningEntry.id)}>ยกเลิก</button></div>
            </>
          ) : (
            <>
              <div className="timer-selects">
                <div className="form-field"><label htmlFor="timer-project">Project</label><select id="timer-project" value={timerProjectId} onChange={(event) => { setSelectedProject(event.target.value); setSelectedTask('') }}>{activeProjects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
                <div className="form-field"><label htmlFor="timer-task">Task</label><select id="timer-task" value={selectedTask} onChange={(event) => setSelectedTask(event.target.value)}><option value="">ไม่ระบุ Task</option>{selectedTasks.map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select></div>
              </div>
              <div className="form-field"><label htmlFor="timer-description">คำอธิบาย</label><input id="timer-description" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="กำลังทำอะไรอยู่?" /></div>
              <label className="check-field timer-billable"><input type="checkbox" checked={billable} onChange={(event) => setBillable(event.target.checked)} /><span>Billable time</span></label>
              <button className="button button-primary wide" type="button" disabled={!timerProjectId} onClick={startTimer}>▶ เริ่มจับเวลา</button>
            </>
          )}
        </section>

        <section className="panel week-summary">
          <div className="panel-heading"><div><h2>Filtered summary</h2><p>สรุปจากตัวกรองรายการด้านล่าง</p></div><strong className="week-hours">{formatDuration(totalMinutes)}</strong></div>
          <div className="summary-list"><div><span>Billable time</span><strong>{formatDuration(billableMinutes)}</strong></div><div><span>Utilization</span><strong>{totalMinutes ? Math.round((billableMinutes / totalMinutes) * 100) : 0}%</strong></div><div><span>มูลค่าเกิดขึ้น</span><strong>{formatMoney(totalValue)}</strong></div><div><span>จำนวนรายการ</span><strong>{entries.length}</strong></div></div>
        </section>
      </div>

      <section className="panel entries-panel">
        <div className="panel-heading"><div><h2>Time entries</h2><p>ตรวจสอบ แก้ไข และกรองเวลาทำงาน</p></div><div className="range-buttons"><button type="button" onClick={() => applyRange('DAY')}>วันนี้</button><button type="button" onClick={() => applyRange('WEEK')}>สัปดาห์นี้</button><button type="button" onClick={() => applyRange('ALL')}>ทั้งหมด</button></div></div>
        <div className="entry-filters">
          <select value={filters.client} onChange={(event) => setFilters((current) => ({ ...current, client: event.target.value, project: 'ALL', task: 'ALL' }))}><option value="ALL">ทุกลูกค้า</option>{data.clients.map((client) => <option key={client.id} value={client.id}>{client.company_name || client.name}</option>)}</select>
          <select value={filters.project} onChange={(event) => setFilters((current) => ({ ...current, project: event.target.value, task: 'ALL' }))}><option value="ALL">ทุกโปรเจกต์</option>{data.projects.filter((project) => filters.client === 'ALL' || project.client_id === filters.client).map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select>
          <select value={filters.task} onChange={(event) => setFilters((current) => ({ ...current, task: event.target.value }))}><option value="ALL">ทุก Task</option>{data.tasks.filter((task) => filters.project === 'ALL' || task.project_id === filters.project).map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select>
          <select value={filters.billable} onChange={(event) => setFilters((current) => ({ ...current, billable: event.target.value }))}><option value="ALL">Billable ทั้งหมด</option><option value="true">Billable</option><option value="false">Non-billable</option></select>
          <select value={filters.invoice} onChange={(event) => setFilters((current) => ({ ...current, invoice: event.target.value }))}><option value="ALL">Invoice ทั้งหมด</option><option value="UNBILLED">ยังไม่ออก Invoice</option><option value="INVOICED">ออก Invoice แล้ว</option></select>
          <input type="date" value={filters.from} onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} aria-label="จากวันที่" />
          <input type="date" value={filters.to} onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} aria-label="ถึงวันที่" />
        </div>
        {entries.length === 0 ? <EmptyState icon="◷" title="ไม่มีรายการเวลา" description="ลองเปลี่ยนตัวกรองหรือเพิ่มรายการเวลาใหม่" /> : (
          <div className="table-wrap"><table className="data-table"><thead><tr><th>Project / Task</th><th>วันที่</th><th>ระยะเวลา</th><th>ประเภท</th><th>มูลค่า</th><th>Invoice</th><th /></tr></thead><tbody>
            {entries.map((entry) => {
              const project = data.projects.find((item) => item.id === entry.project_id)
              const task = data.tasks.find((item) => item.id === entry.task_id)
              return <tr key={entry.id}><td><div className="table-primary"><span className="color-dot" style={{ '--dot-color': project?.color }} /><span><strong>{project?.name}</strong><small>{task?.name || entry.description || 'ไม่ระบุ Task'}</small></span></div></td><td>{formatDate(entry.started_at)}</td><td><strong>{formatDuration(entry.duration_minutes)}</strong></td><td><span className={`type-badge ${entry.billable ? 'billable' : ''}`}>{entry.billable ? 'Billable' : 'Non-billable'}</span></td><td>{formatMoney(calculateTimeValue(entry), entry.currency)}</td><td>{entry.invoice_id ? 'Invoiced' : 'Unbilled'}</td><td><div className="table-actions">{!entry.invoice_id && <><button className="mini-button" type="button" onClick={() => openManual(entry)}>แก้ไข</button><button className="mini-button" type="button" onClick={() => remove('time_entries', entry.id)}>ลบ</button></>}<button className="mini-button" type="button" onClick={() => duplicateEntry(entry)}>คัดลอก</button></div></td></tr>
            })}
          </tbody></table></div>
        )}
      </section>

      <Modal open={manualOpen} onClose={() => setManualOpen(false)} title={manualForm.id ? 'แก้ไขรายการเวลา' : 'เพิ่มรายการเวลา'} size="large">
        <form onSubmit={saveManualEntry}>
          {formError && <p className="form-error">{formError}</p>}
          <div className="form-grid">
            <div className="form-field"><label htmlFor="manual-project">โปรเจกต์</label><select id="manual-project" name="project_id" value={manualForm.project_id} onChange={handleManualChange} required>{data.projects.filter((project) => project.status !== 'ARCHIVED').map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
            <div className="form-field"><label htmlFor="manual-task">Task</label><select id="manual-task" name="task_id" value={manualForm.task_id || ''} onChange={handleManualChange}><option value="">ไม่ระบุ</option>{data.tasks.filter((task) => task.project_id === manualForm.project_id).map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select></div>
            <div className="form-field full"><label htmlFor="manual-description">คำอธิบาย</label><input id="manual-description" name="description" value={manualForm.description} onChange={handleManualChange} /></div>
            <div className="form-field"><label htmlFor="manual-date">วันที่</label><input id="manual-date" name="entry_date" type="date" value={manualForm.entry_date} onChange={handleManualChange} required /></div>
            <div className="form-field"><label htmlFor="manual-mode">วิธีระบุเวลา</label><select id="manual-mode" name="manual_mode" value={manualForm.manual_mode} onChange={handleManualChange}><option value="RANGE">เวลาเริ่ม–สิ้นสุด</option><option value="DURATION">ระยะเวลา</option></select></div>
            <div className="form-field"><label htmlFor="manual-start">เวลาเริ่ม</label><input id="manual-start" name="start_time" type="time" value={manualForm.start_time} onChange={handleManualChange} required /></div>
            {manualForm.manual_mode === 'RANGE' ? <div className="form-field"><label htmlFor="manual-end">เวลาสิ้นสุด</label><input id="manual-end" name="end_time" type="time" value={manualForm.end_time} onChange={handleManualChange} required /></div> : <div className="form-field"><label htmlFor="manual-duration">ระยะเวลา (นาที)</label><input id="manual-duration" name="duration_minutes" type="number" min="1" value={manualForm.duration_minutes} onChange={handleManualChange} required /></div>}
            <div className="form-field"><label htmlFor="manual-rate">อัตราต่อชั่วโมง (เว้นว่างเพื่อใช้ Project rate)</label><input id="manual-rate" name="rate_snapshot" type="number" min="0" step="0.01" value={manualForm.rate_snapshot} onChange={handleManualChange} /></div>
            <label className="check-field"><input name="billable" type="checkbox" checked={manualForm.billable} onChange={handleManualChange} /><span>รายการนี้เรียกเก็บเงินได้</span></label>
          </div>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setManualOpen(false)}>ยกเลิก</button><button className="button button-primary" type="submit">บันทึกรายการ</button></div>
        </form>
      </Modal>
    </div>
  )
}

export default TimeTrackerPage
