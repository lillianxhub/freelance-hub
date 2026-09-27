import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import Modal from '../../../components/Modal'
import PageHeader from '../../../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useWorkspace } from '../../../Workspace/useWorkspace'
import type { TimeEntry } from '../../../types/timeTracking'
import type { RangePreset, TimeFilters } from '../../../types/timeTrackerPage'
import TimerPanel from '../../components/TimerPanel'
import TimeSummary from '../../components/TimeSummary'
import TimeEntryTable from '../../components/TimeEntryTable'
import TimeEntryForm from '../../components/TimeEntryForm'
import { createEmptyManualForm, localDateValue } from '../../../utils/timeTracking'
import { calculateTimeValue } from '../../../utils/formatters'

function TimeTrackerPage() {
  const { data, loading, error, refresh, save, remove } = useWorkspace()
  const [manualOpen, setManualOpen] = useState(false)
  const [manualForm, setManualForm] = useState(createEmptyManualForm())
  const [formError, setFormError] = useState('')
  const [filters, setFilters] = useState<TimeFilters>({ client: 'ALL', project: 'ALL', task: 'ALL', billable: 'ALL', invoice: 'ALL', from: '', to: '' })

  const activeProjects = useMemo(() => (data?.projects || []).filter((project) => project.status === 'ACTIVE'), [data?.projects])
  if (loading) return <LoadingState label="LoadingTime entries..." />
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
    .sort((a, b) => Date.parse(b.started_at) - Date.parse(a.started_at))

  const totalMinutes = entries.reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
  const billableMinutes = entries.filter((entry) => entry.billable).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
  const totalValue = entries.reduce((sum, entry) => sum + calculateTimeValue(entry), 0)

  const openManual = (entry: TimeEntry | null = null) => {
    if (entry) {
      const start = new Date(entry.started_at)
      const end = new Date(entry.ended_at ?? entry.started_at)
      setManualForm({
        ...entry,
        task_id: entry.task_id || '',
        duration_minutes: entry.duration_minutes ?? 0,
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

  const handleManualChange = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    const fieldValue = event.target instanceof HTMLInputElement && event.target.type === 'checkbox' ? event.target.checked : value
    setManualForm((current) => ({ ...current, [name]: fieldValue, ...(name === 'project_id' ? { task_id: '' } : {}) }))
  }

  const saveManualEntry = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const project = data.projects.find((item) => item.id === manualForm.project_id)
    if (!project) {
      setFormError('กรุณาเลือกโปรเจกต์')
      return
    }

    const startedAt = new Date(`${manualForm.entry_date}T${manualForm.start_time}:00`)
    let endedAt: Date
    let duration: number
    if (manualForm.manual_mode === 'RANGE') {
      endedAt = new Date(`${manualForm.entry_date}T${manualForm.end_time}:00`)
      duration = Math.round((endedAt.getTime() - startedAt.getTime()) / 60000)
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

  const duplicateEntry = (entry: TimeEntry) => {
    const duplicate = { ...entry, id: undefined, invoice_id: null, started_at: new Date().toISOString(), ended_at: new Date(Date.now() + (entry.duration_minutes ?? 0) * 60000).toISOString() }
    save('time_entries', duplicate)
  }

  const applyRange = (preset: RangePreset) => {
    if (preset === 'ALL') {
      setFilters((current) => ({ ...current, from: '', to: '' }))
      return
    }
    const now = new Date()
    const local = (date: Date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
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
      <PageHeader eyebrow="พื้นที่ทำงาน / บันทึกเวลา" title="บันทึกเวลา" description="เปลี่ยนเวลาทำTaskให้เป็นรายการที่แม่นยำและพร้อมเรียกเก็บเงิน" actions={<button className="button button-primary" type="button" onClick={() => openManual()}>＋ เพิ่มเวลาด้วยตนเอง</button>} />

      <div className="tracker-layout">
        <TimerPanel />

        <TimeSummary totalMinutes={totalMinutes} billableMinutes={billableMinutes} totalValue={totalValue} count={entries.length} />
      </div>

      <section className="panel entries-panel">
        <div className="panel-heading"><div><h2>รายการเวลา</h2><p>ตรวจสอบ แก้ไข และกรองเวลาทำงาน</p></div><div className="range-buttons"><button type="button" onClick={() => applyRange('DAY')}>วันนี้</button><button type="button" onClick={() => applyRange('WEEK')}>สัปดาห์นี้</button><button type="button" onClick={() => applyRange('ALL')}>ทั้งหมด</button></div></div>
        <div className="entry-filters">
          <select value={filters.client} onChange={(event) => setFilters((current) => ({ ...current, client: event.target.value, project: 'ALL', task: 'ALL' }))}><option value="ALL">ทุกลูกค้า</option>{data.clients.map((client) => <option key={client.id} value={client.id}>{client.company_name || client.name}</option>)}</select>
          <select value={filters.project} onChange={(event) => setFilters((current) => ({ ...current, project: event.target.value, task: 'ALL' }))}><option value="ALL">ทุกโปรเจกต์</option>{data.projects.filter((project) => filters.client === 'ALL' || project.client_id === filters.client).map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select>
          <select value={filters.task} onChange={(event) => setFilters((current) => ({ ...current, task: event.target.value }))}><option value="ALL">ทุกงาน</option>{data.tasks.filter((task) => filters.project === 'ALL' || task.project_id === filters.project).map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}</select>
          <select value={filters.billable} onChange={(event) => setFilters((current) => ({ ...current, billable: event.target.value as TimeFilters['billable'] }))}><option value="ALL">คิดค่าบริการ ทั้งหมด</option><option value="true">คิดค่าบริการ</option><option value="false">ไม่คิดค่าบริการ</option></select>
          <select value={filters.invoice} onChange={(event) => setFilters((current) => ({ ...current, invoice: event.target.value as TimeFilters['invoice'] }))}><option value="ALL">ใบแจ้งหนี้ ทั้งหมด</option><option value="UNBILLED">ยังไม่ออก ใบแจ้งหนี้</option><option value="INVOICED">ออก ใบแจ้งหนี้ แล้ว</option></select>
          <input type="date" value={filters.from} onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} aria-label="จากวันที่" />
          <input type="date" value={filters.to} onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} aria-label="ถึงวันที่" />
        </div>
        {entries.length === 0 ? <EmptyState icon="◷" title="ไม่มีTime entries" description="ลองเปลี่ยนตัวกรองหรือเพิ่มTime entriesใหม่" /> : (
          <TimeEntryTable entries={entries} projects={data.projects} tasks={data.tasks} onEdit={openManual} onDelete={(entry) => remove('time_entries', entry.id)} onDuplicate={duplicateEntry} />
        )}
      </section>

      <Modal open={manualOpen} onClose={() => setManualOpen(false)} title={manualForm.id ? 'แก้ไขTime entries' : 'เพิ่มTime entries'} size="large">
        <TimeEntryForm value={manualForm} projects={data.projects} tasks={data.tasks} error={formError} onChange={handleManualChange} onSubmit={saveManualEntry} onCancel={() => setManualOpen(false)} />
      </Modal>
    </div>
  )
}

export default TimeTrackerPage
