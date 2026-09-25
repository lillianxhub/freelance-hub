import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import PageHeader from '../components/PageHeader'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import type { BillingType, Project, ProjectStatus, ResourceInput } from '../types/domain'
import { getErrorMessage } from '../utils/errors'
import { formatDuration, formatMoney } from '../utils/formatters'

type ProjectFilter = 'ALL' | ProjectStatus
type BillingFilter = 'ALL' | BillingType
type ProjectSort = 'UPDATED_DESC' | 'NAME_ASC' | 'END_ASC'
type ProjectForm = Omit<ResourceInput<'projects'>, 'hourly_rate' | 'fixed_price' | 'budget_hours' | 'budget_amount'> & {
  hourly_rate: number | ''
  fixed_price: number | ''
  budget_hours: number | ''
  budget_amount: number | ''
}

const emptyForm: ProjectForm = {
  name: '', client_id: '', description: '', color: '#3867f4', status: 'PLANNED', billing_type: 'HOURLY',
  hourly_rate: 850, fixed_price: '', budget_hours: 40, budget_amount: '', currency: 'THB', start_date: '', end_date: '',
}

function ProjectsPage() {
  const { data, loading, error, refresh, save } = useWorkspace()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<ProjectFilter>('ALL')
  const [billingType, setBillingType] = useState<BillingFilter>('ALL')
  const [sortBy, setSortBy] = useState<ProjectSort>('UPDATED_DESC')
  const [page, setPage] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const projects = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return (data?.projects || [])
      .filter((project) => status === 'ALL' || project.status === status)
      .filter((project) => billingType === 'ALL' || project.billing_type === billingType)
      .filter((project) => {
        const client = data.clients.find((item) => item.id === project.client_id)
        return !normalized || [project.name, project.description, client?.company_name, client?.name]
          .some((value) => value?.toLowerCase().includes(normalized))
      })
      .sort((a, b) => {
        if (sortBy === 'NAME_ASC') return a.name.localeCompare(b.name)
        if (sortBy === 'END_ASC') return (a.end_date || '9999').localeCompare(b.end_date || '9999')
        return Date.parse(b.updated_at ?? '') - Date.parse(a.updated_at ?? '')
      })
  }, [billingType, data, query, sortBy, status])
  const pageSize = 6
  const totalPages = Math.max(1, Math.ceil(projects.length / pageSize))
  const safePage = Math.min(page, totalPages)
  const visibleProjects = projects.slice((safePage - 1) * pageSize, safePage * pageSize)

  const openCreate = () => {
    setForm({ ...emptyForm, client_id: data.clients.find((client) => client.status === 'ACTIVE')?.id || '' })
    setFormError('')
    setModalOpen(true)
  }

  const openEdit = (project: Project) => {
    setForm({ ...emptyForm, ...project, hourly_rate: project.hourly_rate ?? '', fixed_price: project.fixed_price ?? '', budget_hours: project.budget_hours ?? '', budget_amount: project.budget_amount ?? '' })
    setFormError('')
    setModalOpen(true)
  }

  const handleChange = (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.name.trim() || !form.client_id) {
      setFormError('กรุณาระบุชื่อโปรเจกต์และลูกค้า')
      return
    }
    if (form.billing_type === 'HOURLY' && Number(form.hourly_rate) <= 0) {
      setFormError('โปรเจกต์รายชั่วโมงต้องมี hourly rate มากกว่า 0')
      return
    }
    if (form.billing_type === 'FIXED_PRICE' && Number(form.fixed_price) <= 0) {
      setFormError('โปรเจกต์เหมาจ่ายต้องมีมูลค่างานมากกว่า 0')
      return
    }
    if (form.start_date && form.end_date && form.end_date < form.start_date) {
      setFormError('วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่ม')
      return
    }

    setSaving(true)
    setFormError('')
    try {
      await save('projects', {
        ...form,
        name: form.name.trim(),
        hourly_rate: form.billing_type === 'HOURLY' ? Number(form.hourly_rate) : null,
        fixed_price: form.billing_type === 'FIXED_PRICE' ? Number(form.fixed_price) : null,
        budget_hours: form.budget_hours === '' ? null : Number(form.budget_hours),
        budget_amount: form.budget_amount === '' ? null : Number(form.budget_amount),
      })
      setModalOpen(false)
    } catch (err) {
      setFormError(getErrorMessage(err, 'บันทึกโปรเจกต์ไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const archiveProject = async (project: Project) => {
    await save('projects', { ...project, status: project.status === 'ARCHIVED' ? 'PLANNED' : 'ARCHIVED' })
  }

  if (loading) return <LoadingState label="กำลังโหลดโปรเจกต์..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  return (
    <div className="page-view">
      <PageHeader eyebrow="Workspace / Projects" title="Your projects" description="ติดตามขอบเขตงาน งบประมาณ Task และการส่งมอบในมุมมองเดียว" actions={<button className="button button-primary" type="button" onClick={openCreate}>＋ เพิ่มโปรเจกต์</button>} />

      <div className="filter-row">
        <div className="search-box"><span>⌕</span><input value={query} onChange={(event) => { setQuery(event.target.value); setPage(1) }} placeholder="ค้นหาโปรเจกต์หรือลูกค้า" /></div>
        <select className="select-button" value={status} onChange={(event) => { setStatus(event.target.value as ProjectFilter); setPage(1) }}><option value="ALL">ทุกสถานะ</option><option value="PLANNED">วางแผน</option><option value="ACTIVE">กำลังทำ</option><option value="ON_HOLD">พักงาน</option><option value="COMPLETED">เสร็จสิ้น</option><option value="ARCHIVED">เก็บถาวร</option></select>
        <select className="select-button" value={billingType} onChange={(event) => { setBillingType(event.target.value as BillingFilter); setPage(1) }}><option value="ALL">ทุกรูปแบบราคา</option><option value="HOURLY">รายชั่วโมง</option><option value="FIXED_PRICE">เหมาจ่าย</option></select>
        <select className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value as ProjectSort); setPage(1) }}><option value="UPDATED_DESC">อัปเดตล่าสุด</option><option value="NAME_ASC">ชื่อ A–Z</option><option value="END_ASC">กำหนดส่งใกล้สุด</option></select>
      </div>

      {projects.length === 0 ? (
        <section className="panel"><EmptyState icon="▦" title="ยังไม่พบโปรเจกต์" description="สร้างโปรเจกต์แรกหรือปรับตัวกรอง" action={<button className="button button-primary" type="button" onClick={openCreate}>เพิ่มโปรเจกต์</button>} /></section>
      ) : (
        <div className="card-grid project-card-grid">
          {visibleProjects.map((project) => {
            const client = data.clients.find((item) => item.id === project.client_id)
            const tasks = data.tasks.filter((task) => task.project_id === project.id)
            const completedTasks = tasks.filter((task) => task.status === 'DONE').length
            const taskPercent = tasks.length ? Math.round((completedTasks / tasks.length) * 100) : 0
            const minutes = data.time_entries.filter((entry) => entry.project_id === project.id).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
            const budgetPercent = project.budget_hours ? Math.round((minutes / 60 / project.budget_hours) * 100) : 0
            return (
              <article className="project-card" key={project.id} style={{ '--project-color': project.color }}>
                <Link className="card-link" to={`/projects/${project.id}`} aria-label={`เปิด ${project.name}`} />
                <div className="card-top"><span className="color-dot project-dot" style={{ '--dot-color': project.color }} /><div className="card-menu"><button className="mini-button" type="button" onClick={() => openEdit(project)}>แก้ไข</button><button className="mini-button" type="button" onClick={() => archiveProject(project)}>{project.status === 'ARCHIVED' ? 'นำกลับ' : 'Archive'}</button></div></div>
                <h2>{project.name}</h2><p>{client?.company_name || client?.name || 'ไม่พบลูกค้า'} · {tasks.length} tasks</p>
                <div className="card-metrics"><span><strong>{completedTasks}<small> / {tasks.length} เสร็จแล้ว</small></strong>Task progress</span><span><strong>{formatDuration(minutes)}</strong>Time tracked</span></div>
                <div className="progress-label"><span>Task progress</span><strong>{taskPercent}%</strong></div><div className="progress-track"><span style={{ width: `${taskPercent}%`, background: project.color }} /></div>
                {project.budget_hours && <div className={`budget-note${budgetPercent >= 100 ? ' danger' : budgetPercent >= 80 ? ' warning' : ''}`}>ใช้เวลา {budgetPercent}% ของงบ {project.budget_hours} ชั่วโมง</div>}
                <div className="card-footer"><StatusBadge status={project.status} /><span>{project.billing_type === 'HOURLY' ? `${formatMoney(project.hourly_rate, project.currency)}/ชม.` : formatMoney(project.fixed_price, project.currency)}</span></div>
              </article>
            )
          })}
        </div>
      )}

      {totalPages > 1 && <div className="pagination"><button className="button button-secondary" type="button" disabled={safePage === 1} onClick={() => setPage((value) => value - 1)}>← ก่อนหน้า</button><span>หน้า {safePage} จาก {totalPages}</span><button className="button button-secondary" type="button" disabled={safePage === totalPages} onClick={() => setPage((value) => value + 1)}>ถัดไป →</button></div>}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'แก้ไขโปรเจกต์' : 'เพิ่มโปรเจกต์'} size="large">
        <form onSubmit={handleSubmit}>
          {formError && <p className="form-error">{formError}</p>}
          <div className="form-grid">
            <div className="form-field"><label htmlFor="project-name">ชื่อโปรเจกต์</label><input id="project-name" name="name" value={form.name} onChange={handleChange} required /></div>
            <div className="form-field"><label htmlFor="project-client">ลูกค้า</label><select id="project-client" name="client_id" value={form.client_id} onChange={handleChange} required>{data.clients.filter((client) => client.status === 'ACTIVE' || client.id === form.client_id).map((client) => <option key={client.id} value={client.id}>{client.company_name || client.name}</option>)}</select></div>
            <div className="form-field"><label htmlFor="project-status">สถานะ</label><select id="project-status" name="status" value={form.status} onChange={handleChange}><option value="PLANNED">วางแผน</option><option value="ACTIVE">กำลังดำเนินการ</option><option value="ON_HOLD">พักงาน</option><option value="COMPLETED">เสร็จสิ้น</option><option value="ARCHIVED">เก็บถาวร</option></select></div>
            <div className="form-field"><label htmlFor="project-billing">รูปแบบราคา</label><select id="project-billing" name="billing_type" value={form.billing_type} onChange={handleChange}><option value="HOURLY">รายชั่วโมง</option><option value="FIXED_PRICE">เหมาจ่าย</option></select></div>
            {form.billing_type === 'HOURLY' ? <div className="form-field"><label htmlFor="hourly-rate">Hourly rate</label><input id="hourly-rate" name="hourly_rate" type="number" min="0" step="0.01" value={form.hourly_rate} onChange={handleChange} required /></div> : <div className="form-field"><label htmlFor="fixed-price">มูลค่างาน</label><input id="fixed-price" name="fixed_price" type="number" min="0" step="0.01" value={form.fixed_price} onChange={handleChange} required /></div>}
            <div className="form-field"><label htmlFor="budget-hours">งบชั่วโมง</label><input id="budget-hours" name="budget_hours" type="number" min="0" step="0.5" value={form.budget_hours} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="start-date">วันที่เริ่ม</label><input id="start-date" name="start_date" type="date" value={form.start_date || ''} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="end-date">วันที่สิ้นสุด</label><input id="end-date" name="end_date" type="date" value={form.end_date || ''} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="project-color">สีโปรเจกต์</label><input id="project-color" name="color" type="color" value={form.color} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="project-currency">สกุลเงิน</label><select id="project-currency" name="currency" value={form.currency} onChange={handleChange}><option value="THB">THB</option><option value="USD">USD</option><option value="EUR">EUR</option></select></div>
            <div className="form-field full"><label htmlFor="project-description">รายละเอียด</label><textarea id="project-description" name="description" value={form.description} onChange={handleChange} /></div>
          </div>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setModalOpen(false)}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกโปรเจกต์'}</button></div>
        </form>
      </Modal>
    </div>
  )
}

export default ProjectsPage
