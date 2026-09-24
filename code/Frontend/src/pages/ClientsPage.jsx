import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Modal from '../components/Modal'
import PageHeader from '../components/PageHeader'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { formatDuration, formatMoney, initials } from '../utils/formatters'

const emptyForm = {
  name: '', company_name: '', email: '', phone: '', address: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#3867f4',
}

function ClientsPage() {
  const { data, loading, error, refresh, save } = useWorkspace()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('ACTIVE')
  const [sortBy, setSortBy] = useState('UPDATED_DESC')
  const [page, setPage] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const pageSize = 6

  const clients = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return (data?.clients || [])
      .filter((client) => status === 'ALL' || client.status === status)
      .filter((client) => !normalizedQuery || [client.name, client.company_name, client.email, client.phone]
        .some((value) => value?.toLowerCase().includes(normalizedQuery)))
      .sort((a, b) => {
        if (sortBy === 'NAME_ASC') return (a.company_name || a.name).localeCompare(b.company_name || b.name)
        if (sortBy === 'CREATED_ASC') return new Date(a.created_at) - new Date(b.created_at)
        return new Date(b.updated_at) - new Date(a.updated_at)
      })
  }, [data?.clients, query, sortBy, status])

  const totalPages = Math.max(1, Math.ceil(clients.length / pageSize))
  const safePage = Math.min(page, totalPages)
  const visibleClients = clients.slice((safePage - 1) * pageSize, safePage * pageSize)

  const openCreate = () => {
    setForm(emptyForm)
    setFormError('')
    setModalOpen(true)
  }

  const openEdit = (client) => {
    setForm({ ...emptyForm, ...client })
    setFormError('')
    setModalOpen(true)
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!form.name.trim() && !form.company_name.trim()) {
      setFormError('กรุณากรอกชื่อผู้ติดต่อหรือชื่อบริษัทอย่างน้อยหนึ่งรายการ')
      return
    }
    if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) {
      setFormError('รูปแบบอีเมลไม่ถูกต้อง')
      return
    }

    setSaving(true)
    setFormError('')
    try {
      await save('clients', { ...form, name: form.name.trim(), company_name: form.company_name.trim() })
      setModalOpen(false)
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const archiveClient = async (client) => {
    await save('clients', { ...client, status: client.status === 'ARCHIVED' ? 'ACTIVE' : 'ARCHIVED' })
  }

  if (loading) return <LoadingState label="กำลังโหลดรายชื่อลูกค้า..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  return (
    <div className="page-view">
      <PageHeader
        eyebrow="Workspace / Clients"
        title="Clients"
        description="เก็บข้อมูลลูกค้า โปรเจกต์ และกิจกรรมทั้งหมดไว้ในที่เดียว"
        actions={<button className="button button-primary" type="button" onClick={openCreate}>＋ เพิ่มลูกค้า</button>}
      />

      <div className="filter-row">
        <div className="search-box"><span>⌕</span><input value={query} onChange={(event) => { setQuery(event.target.value); setPage(1) }} placeholder="ค้นหาชื่อ บริษัท อีเมล หรือเบอร์โทร" aria-label="ค้นหาลูกค้า" /></div>
        <select className="select-button" value={status} onChange={(event) => { setStatus(event.target.value); setPage(1) }} aria-label="กรองสถานะลูกค้า">
          <option value="ALL">ทุกสถานะ</option>
          <option value="ACTIVE">ใช้งานอยู่</option>
          <option value="ARCHIVED">เก็บถาวร</option>
        </select>
        <select className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value); setPage(1) }} aria-label="เรียงลำดับลูกค้า"><option value="UPDATED_DESC">อัปเดตล่าสุด</option><option value="NAME_ASC">ชื่อ A–Z</option><option value="CREATED_ASC">เพิ่มก่อนสุด</option></select>
      </div>

      {visibleClients.length === 0 ? (
        <section className="panel"><EmptyState icon="♧" title="ยังไม่พบลูกค้า" description="เพิ่มลูกค้ารายแรกหรือเปลี่ยนคำค้นหาและตัวกรอง" action={<button className="button button-primary" type="button" onClick={openCreate}>เพิ่มลูกค้า</button>} /></section>
      ) : (
        <div className="card-grid">
          {visibleClients.map((client) => {
            const clientProjects = data.projects.filter((project) => project.client_id === client.id)
            const projectIds = new Set(clientProjects.map((project) => project.id))
            const minutes = data.time_entries.filter((entry) => projectIds.has(entry.project_id)).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
            const revenue = data.time_entries.filter((entry) => projectIds.has(entry.project_id) && entry.billable).reduce((sum, entry) => sum + ((entry.duration_minutes || 0) / 60) * (entry.rate_snapshot || 0), 0)
            const displayName = client.company_name || client.name
            return (
              <article className="client-card" key={client.id}>
                <Link className="card-link" to={`/clients/${client.id}`} aria-label={`เปิด ${displayName}`} />
                <div className="card-top">
                  <span className="client-avatar" style={{ background: client.color }}>{initials(displayName)}</span>
                  <div className="card-menu">
                    <button className="mini-button" type="button" onClick={() => openEdit(client)}>แก้ไข</button>
                    <button className="mini-button" type="button" onClick={() => archiveClient(client)}>{client.status === 'ARCHIVED' ? 'นำกลับ' : 'Archive'}</button>
                  </div>
                </div>
                <h2>{displayName}</h2>
                <p>{client.email || 'ยังไม่มีอีเมล'}</p>
                <div className="client-stats">
                  <span><strong>{clientProjects.length}</strong>โปรเจกต์</span>
                  <span><strong>{formatDuration(minutes)}</strong>เวลารวม</span>
                </div>
                <div className="card-footer"><StatusBadge status={client.status} /><span>{formatMoney(revenue)}</span></div>
              </article>
            )
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="button button-secondary" type="button" disabled={safePage === 1} onClick={() => setPage((value) => value - 1)}>← ก่อนหน้า</button>
          <span>หน้า {safePage} จาก {totalPages}</span>
          <button className="button button-secondary" type="button" disabled={safePage === totalPages} onClick={() => setPage((value) => value + 1)}>ถัดไป →</button>
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'แก้ไขข้อมูลลูกค้า' : 'เพิ่มลูกค้า'} size="large">
        <form onSubmit={handleSubmit}>
          {formError && <p className="form-error">{formError}</p>}
          <div className="form-grid">
            <div className="form-field"><label htmlFor="client-name">ชื่อผู้ติดต่อ</label><input id="client-name" name="name" value={form.name} onChange={handleChange} placeholder="ชื่อ-นามสกุล" /></div>
            <div className="form-field"><label htmlFor="company-name">ชื่อบริษัท</label><input id="company-name" name="company_name" value={form.company_name} onChange={handleChange} placeholder="บริษัท จำกัด" /></div>
            <div className="form-field"><label htmlFor="client-email">อีเมล</label><input id="client-email" name="email" type="email" value={form.email} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="client-phone">โทรศัพท์</label><input id="client-phone" name="phone" value={form.phone} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="client-tax">เลขประจำตัวผู้เสียภาษี</label><input id="client-tax" name="tax_id" value={form.tax_id} onChange={handleChange} /></div>
            <div className="form-field"><label htmlFor="client-status">สถานะ</label><select id="client-status" name="status" value={form.status} onChange={handleChange}><option value="ACTIVE">ใช้งานอยู่</option><option value="ARCHIVED">เก็บถาวร</option></select></div>
            <div className="form-field full"><label htmlFor="client-address">ที่อยู่</label><textarea id="client-address" name="address" value={form.address} onChange={handleChange} /></div>
            <div className="form-field full"><label htmlFor="client-notes">หมายเหตุ</label><textarea id="client-notes" name="notes" value={form.notes} onChange={handleChange} /></div>
          </div>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setModalOpen(false)}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกลูกค้า'}</button></div>
        </form>
      </Modal>
    </div>
  )
}

export default ClientsPage
