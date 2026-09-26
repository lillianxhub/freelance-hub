import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import Modal from '../../../components/Modal'
import PageHeader from '../../../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useClients } from '../../useClients'
import type { Client } from '../../../types/client'
import type { ResourceInput } from '../../../types/workspace'
import type { ClientFilter, ClientSort } from '../../../types/clientsPage'
import ClientCard from '../../components/ClientCard'
import ClientForm from '../../components/ClientForm'
import { validateClient } from '../../client.validators'
import { getErrorMessage } from '../../../api/apiError'

const emptyForm: ResourceInput<'clients'> = {
  name: '', company_name: '', email: '', phone: '', address: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#3867f4',
}

function ClientsPage() {
  const { data, loading, error, refresh, save } = useClients()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<ClientFilter>('ACTIVE')
  const [sortBy, setSortBy] = useState<ClientSort>('UPDATED_DESC')
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
        if (sortBy === 'CREATED_ASC') return Date.parse(a.created_at ?? '') - Date.parse(b.created_at ?? '')
        return Date.parse(b.updated_at ?? '') - Date.parse(a.updated_at ?? '')
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

  const openEdit = (client: Client) => {
    setForm({ ...emptyForm, ...client })
    setFormError('')
    setModalOpen(true)
  }

  const handleChange = (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationError = validateClient(form)
    if (validationError) {
      setFormError(validationError)
      return
    }

    setSaving(true)
    setFormError('')
    try {
      await save('clients', { ...form, name: form.name.trim(), company_name: form.company_name.trim() })
      setModalOpen(false)
    } catch (err) {
      setFormError(getErrorMessage(err, 'บันทึกลูกค้าไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const archiveClient = async (client: Client) => {
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
        <select className="select-button" value={status} onChange={(event) => { setStatus(event.target.value as ClientFilter); setPage(1) }} aria-label="กรองสถานะลูกค้า">
          <option value="ALL">ทุกสถานะ</option>
          <option value="ACTIVE">ใช้งานอยู่</option>
          <option value="ARCHIVED">เก็บถาวร</option>
        </select>
        <select className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value as ClientSort); setPage(1) }} aria-label="เรียงลำดับลูกค้า"><option value="UPDATED_DESC">อัปเดตล่าสุด</option><option value="NAME_ASC">ชื่อ A–Z</option><option value="CREATED_ASC">เพิ่มก่อนสุด</option></select>
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
            return <ClientCard key={client.id} client={client} projectCount={clientProjects.length} minutes={minutes} revenue={revenue} onEdit={openEdit} onArchive={archiveClient} />
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
        <ClientForm value={form} error={formError} saving={saving} onChange={handleChange} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
    </div>
  )
}

export default ClientsPage
