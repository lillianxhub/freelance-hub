import { Card } from '../../../components/ui/card'
import { Pagination, PaginationContent, PaginationItem } from '../../../components/ui/pagination'
import { Button } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import { NativeSelect } from '../../../components/ui/native-select'
import { useCallback, useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { FiArrowLeft, FiArrowRight, FiPlus, FiSearch, FiUsers } from 'react-icons/fi'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../../components/ui/dialog'
import PageHeader from '../../../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useClients } from '../../useClients'
import type { Client } from '../../../types/client'
import type { ResourceInput } from '../../../types/workspace'
import type { ClientFilter, ClientSort } from '../../../types/clientsPage'
import ClientCard from '../../components/ClientCard'
import ClientForm from '../../components/ClientForm'
import { normalizeDigits, validateClient } from '../../client.validators'
import { getErrorMessage } from '../../../api/apiError'
import { listClientsPage, updateClientStatus, type ClientListFilters } from '../../../services/workspace'
import type { ApiMeta } from '../../../types/api'

const clientPageLimit = 10

const clientSortParams: Record<ClientSort, Pick<ClientListFilters, 'sortBy' | 'direction'>> = {
  UPDATED_DESC: { sortBy: 'updatedAt', direction: 'DESC' },
  NAME_ASC: { sortBy: 'companyName', direction: 'ASC' },
  CREATED_ASC: { sortBy: 'createdAt', direction: 'ASC' },
}

const emptyForm: ResourceInput<'clients'> = {
  name: '', company_name: '', email: '', phone: '', address: '', province: '', district: '', sub_district: '', postal_code: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#4F6BFF',
}

function ClientsPage() {
  const { data, loading, error, refresh, save } = useClients()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<ClientFilter>('ACTIVE')
  const [sortBy, setSortBy] = useState<ClientSort>('UPDATED_DESC')
  const [page, setPage] = useState(1)
  const [pageClients, setPageClients] = useState<Client[]>([])
  const [pageMeta, setPageMeta] = useState<ApiMeta>({ page: 1, limit: clientPageLimit, total: 0, totalPages: 1 })
  const [pageLoading, setPageLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const latestRequest = useRef(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const loadClientPage = useCallback(async (requestedPage: number) => {
    const requestId = ++latestRequest.current
    setPageLoading(true)
    setPageError('')
    try {
      const result = await listClientsPage(requestedPage, clientPageLimit, {
        search: query,
        status: status === 'ALL' ? undefined : status,
        ...clientSortParams[sortBy],
      })
      if (requestId !== latestRequest.current) return
      if (requestedPage > Math.max(1, result.meta.totalPages)) {
        setPage(Math.max(1, result.meta.totalPages))
        return
      }
      setPageClients(result.clients)
      setPageMeta(result.meta)
    } catch (loadError) {
      if (requestId === latestRequest.current) setPageError(getErrorMessage(loadError, 'ไม่สามารถโหลดรายชื่อลูกค้าได้'))
    } finally {
      if (requestId === latestRequest.current) setPageLoading(false)
    }
  }, [query, sortBy, status])

  useEffect(() => {
    void loadClientPage(page)
  }, [loadClientPage, page])

  const totalPages = Math.max(1, pageMeta.totalPages)
  const safePage = Math.min(pageMeta.page, totalPages)
  const visibleClients = pageClients

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
    const normalizedValue = name === 'phone'
      ? normalizeDigits(value, 10)
      : name === 'tax_id'
        ? normalizeDigits(value, 13)
        : value
    setForm((current) => ({ ...current, [name]: normalizedValue }))
  }

  const handleFieldsChange = (values: Partial<ResourceInput<'clients'>>) => {
    setForm((current) => ({ ...current, ...values }))
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
      if (page === 1) await loadClientPage(1)
      else setPage(1)
    } catch (err) {
      setFormError(getErrorMessage(err, 'บันทึกลูกค้าไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const archiveClients = async (client: Client) => {
    await updateClientStatus(client.id, client.status === 'ARCHIVED')
    await refresh()
    await loadClientPage(safePage)
  }

  if (loading || pageLoading) return <LoadingState label="กำลังโหลดรายชื่อลูกค้า..." />
  if (error || pageError) return <ErrorState message={error || pageError} onRetry={() => { void refresh(); void loadClientPage(page) }} />

  return (
    <div className="page-view">
      <PageHeader
        eyebrow="พื้นที่ทำงาน / ลูกค้า"
        title="ลูกค้า"
        description="เก็บข้อมูลลูกค้า โปรเจกต์ และกิจกรรมทั้งหมดไว้ในที่เดียว"
        actions={<Button variant="default" className="button button-primary" type="button" onClick={openCreate}><FiPlus aria-hidden="true" /> เพิ่มลูกค้า</Button>}
      />

      <div className="filter-row">
        <div className="search-box"><span><FiSearch aria-hidden="true" /></span><Input value={query} onChange={(event) => { setQuery(event.target.value); setPage(1) }} placeholder="ค้นหาชื่อ บริษัท อีเมล หรือเบอร์โทร" aria-label="ค้นหาClients" /></div>
        <NativeSelect className="select-button" value={status} onChange={(event) => { setStatus(event.target.value as ClientFilter); setPage(1) }} aria-label="กรองStatusClients">
          <option value="ALL">ทุกสถานะ</option>
          <option value="ACTIVE">ใช้งานอยู่</option>
          <option value="ARCHIVED">เก็บถาวร</option>
        </NativeSelect>
        <NativeSelect className="select-button" value={sortBy} onChange={(event) => { setSortBy(event.target.value as ClientSort); setPage(1) }} aria-label="เรียงลำดับClients"><option value="UPDATED_DESC">อัปเดตล่าสุด</option><option value="NAME_ASC">ชื่อ A–Z</option><option value="CREATED_ASC">เพิ่มก่อนสุด</option></NativeSelect>
      </div>

      {visibleClients.length === 0 ? (
        <Card asChild><section className="panel"><EmptyState icon={<FiUsers aria-hidden="true" />} title="ยังไม่พบClients" description="เพิ่มClientsรายแรกหรือเปลี่ยนคำค้นหาและตัวกรอง" action={<Button variant="default" className="button button-primary" type="button" onClick={openCreate}>เพิ่มลูกค้า</Button>} /></section></Card>
      ) : (
        <div className="card-grid">
          {visibleClients.map((client) => {
            const clientProjects = data.projects.filter((project) => project.client_id === client.id)
            const projectIds = new Set(clientProjects.map((project) => project.id))
            const minutes = data.time_entries.filter((entry) => projectIds.has(entry.project_id)).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
            const revenue = data.time_entries.filter((entry) => projectIds.has(entry.project_id) && entry.billable).reduce((sum, entry) => sum + ((entry.duration_minutes || 0) / 60) * (entry.rate_snapshot || 0), 0)
            return <ClientCard key={client.id} client={client} projectCount={clientProjects.length} minutes={minutes} revenue={revenue} onEdit={openEdit} onArchive={archiveClients} />
          })}
        </div>
      )}

      {totalPages > 1 && (
        <Pagination className="mt-5"><PaginationContent>
          <PaginationItem><Button variant="outline" type="button" disabled={safePage === 1} onClick={() => setPage((value) => value - 1)}><FiArrowLeft aria-hidden="true" /> ก่อนหน้า</Button></PaginationItem>
          <PaginationItem className="px-2 text-sm text-muted-foreground">หน้า {safePage} จาก {totalPages}</PaginationItem>
          <PaginationItem><Button variant="outline" type="button" disabled={safePage === totalPages} onClick={() => setPage((value) => value + 1)}>ถัดไป <FiArrowRight aria-hidden="true" /></Button></PaginationItem>
        </PaginationContent></Pagination>
      )}

      <Dialog open={modalOpen} onOpenChange={(open) => { if (!open) setModalOpen(false) }}>
        <DialogContent className="workspace-dialog workspace-dialog-large">
          <DialogHeader><DialogTitle>{form.id ? 'แก้ไขข้อมูลลูกค้า' : 'เพิ่มClients'}</DialogTitle></DialogHeader>
          <ClientForm value={form} error={formError} saving={saving} onChange={handleChange} onFieldsChange={handleFieldsChange} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default ClientsPage
