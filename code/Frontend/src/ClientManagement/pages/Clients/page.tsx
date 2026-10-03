import { Card } from '../../../components/ui/card'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '../../../components/ui/pagination'
import { Button } from '../../../components/ui/button'
import { NativeSelect } from '../../../components/ui/native-select'
import { useCallback, useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { FiPlus, FiUsers } from 'react-icons/fi'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../../components/ui/dialog'
import PageHeader from '../../../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../../../components/ViewState'
import { useClients } from '../../useClients'
import type { Client } from '../../../types/client'
import type { ClientInput } from '../../../types/client'
import type { ClientFilter, ClientSort } from '../../../types/clientsPage'
import ClientCard from '../../components/ClientCard'
import ClientForm from '../../components/ClientForm'
import FilterBar from '../../../components/FilterBar'
import { normalizeDigits, validateClient } from '../../client.validators'
import { getErrorMessage } from '../../../api/apiError'
import { listClientsPage, type ClientListFilters } from '../../../services/client'
import type { ApiMeta } from '../../../types/api'

const clientPageLimit = 10

const clientSortParams: Record<ClientSort, Pick<ClientListFilters, 'sortBy' | 'direction'>> = {
  UPDATED_DESC: { sortBy: 'updatedAt', direction: 'DESC' },
  NAME_ASC: { sortBy: 'companyName', direction: 'ASC' },
  CREATED_ASC: { sortBy: 'createdAt', direction: 'ASC' },
}

function getPaginationItems(currentPage: number, totalPages: number): Array<number | 'ellipsis'> {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1)

  const pages = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1])
  return [...pages]
    .filter((item) => item >= 1 && item <= totalPages)
    .sort((a, b) => a - b)
    .flatMap((item, index, items) =>
      index > 0 && item - items[index - 1] > 1 ? ['ellipsis' as const, item] : [item],
    )
}

const emptyForm: ClientInput = {
  name: '', company_name: '', email: '', phone: '', address: '', province: '', district: '', sub_district: '', postal_code: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#4F6BFF',
}

function toClientForm(value: Partial<ClientInput> = {}): ClientInput {
  return {
    ...emptyForm,
    ...value,
    name: value.name ?? '',
    company_name: value.company_name ?? '',
    email: value.email ?? '',
    phone: value.phone ?? '',
    address: value.address ?? '',
    province: value.province ?? '',
    district: value.district ?? '',
    sub_district: value.sub_district ?? '',
    postal_code: value.postal_code ?? '',
    tax_id: value.tax_id ?? '',
    notes: value.notes ?? '',
    status: value.status ?? 'ACTIVE',
    color: value.color ?? '#4F6BFF',
  }
}

function ClientsPage() {
  const { data, loading, error, refresh, saveClient, archiveClient } = useClients()
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
  const paginationItems = getPaginationItems(safePage, totalPages)
  const visibleClients = pageClients

  const openCreate = () => {
    setForm(toClientForm())
    setFormError('')
    setModalOpen(true)
  }

  const openEdit = (client: Client) => {
    setForm(toClientForm(client))
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

  const handleFieldsChange = (values: Partial<ClientInput>) => {
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
      await saveClient({ ...form, name: (form.name ?? '').trim(), company_name: (form.company_name ?? '').trim() })
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
    if (client.status === 'ARCHIVED') await saveClient({ ...client, status: 'ACTIVE' })
    else await archiveClient(client.id)
    await refresh()
    await loadClientPage(safePage)
  }

  if (loading || pageLoading) return <LoadingState label="กำลังโหลดรายชื่อลูกค้า..." />
  if (error || pageError) return <ErrorState message={error || pageError} onRetry={() => { void refresh(); void loadClientPage(page) }} />

  return (
    <div className="mx-auto w-full max-w-auto">
      <PageHeader
        title="ลูกค้า"
        actions={
          <Button variant="default" className="h-10" type="button" onClick={openCreate}>
            <FiPlus aria-hidden="true" /> เพิ่มลูกค้า
          </Button>}
      />

      <FilterBar
        value={query}
        onChange={(event) => { setQuery(event.target.value); setPage(1) }}
        placeholder="ค้นหาชื่อ บริษัท อีเมล หรือเบอร์โทร"
        searchAriaLabel="ค้นหาClients"
      >
        <NativeSelect value={status} onChange={(event) => { setStatus(event.target.value as ClientFilter); setPage(1) }} aria-label="กรองStatusClients">
          <option value="ALL">ทุกสถานะ</option>
          <option value="ACTIVE">ใช้งานอยู่</option>
          <option value="ARCHIVED">เก็บถาวร</option>
        </NativeSelect>
        <NativeSelect value={sortBy} onChange={(event) => { setSortBy(event.target.value as ClientSort); setPage(1) }} aria-label="เรียงลำดับClients"><option value="UPDATED_DESC">อัปเดตล่าสุด</option><option value="NAME_ASC">ชื่อ A–Z</option><option value="CREATED_ASC">เพิ่มก่อนสุด</option></NativeSelect>
      </FilterBar>

      {visibleClients.length === 0 ? (
        <Card asChild><section className="p-4 sm:p-6"><EmptyState icon={<FiUsers aria-hidden="true" />} title="ยังไม่พบClients" description="เพิ่มClientsรายแรกหรือเปลี่ยนคำค้นหาและตัวกรอง" action={<Button variant="default" className="h-10" type="button" onClick={openCreate}>เพิ่มลูกค้า</Button>} /></section></Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {visibleClients.map((client) => {
            const clientProjects = data.projects.filter((project) => project.client_id === client.id)
            const projectIds = new Set(clientProjects.map((project) => project.id))
            const minutes = data.time_entries.filter((entry) => projectIds.has(entry.project_id)).reduce((sum, entry) => sum + (entry.duration_minutes || 0), 0)
            const revenue = data.time_entries.filter((entry) => projectIds.has(entry.project_id) && entry.billable).reduce((sum, entry) => sum + ((entry.duration_minutes || 0) / 60) * (entry.rate_snapshot || 0), 0)
            return <ClientCard key={client.id} client={client} projectCount={clientProjects.length} minutes={minutes} revenue={revenue} onEdit={openEdit} onArchive={archiveClients} />
          })}
        </div>
      )}

      <Pagination className="mt-6">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href={`?page=${safePage - 1}`}
              text="ก่อนหน้า"
              aria-disabled={safePage === 1}
              className={safePage === 1 ? 'pointer-events-none opacity-50' : undefined}
              onClick={(event) => {
                event.preventDefault()
                if (safePage > 1) setPage(safePage - 1)
              }}
            />
          </PaginationItem>
          {paginationItems.map((item, index) => (
            <PaginationItem key={`${item}-${index}`}>
              {item === 'ellipsis' ? (
                <PaginationEllipsis />
              ) : (
                <PaginationLink
                  href={`?page=${item}`}
                  isActive={item === safePage}
                  onClick={(event) => {
                    event.preventDefault()
                    setPage(item)
                  }}
                >
                  {item}
                </PaginationLink>
              )}
            </PaginationItem>
          ))}
          <PaginationItem>
            <PaginationNext
              href={`?page=${safePage + 1}`}
              text="ถัดไป"
              aria-disabled={safePage === totalPages}
              className={safePage === totalPages ? 'pointer-events-none opacity-50' : undefined}
              onClick={(event) => {
                event.preventDefault()
                if (safePage < totalPages) setPage(safePage + 1)
              }}
            />
          </PaginationItem>
        </PaginationContent>
      </Pagination>

      <Dialog open={modalOpen} onOpenChange={(open) => { if (!open) setModalOpen(false) }}>
        <DialogContent className="max-h-[calc(100dvh-2rem)] !max-w-4xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{form.id ? 'แก้ไขข้อมูลลูกค้า' : 'เพิ่มClients'}</DialogTitle>
          </DialogHeader>
          <ClientForm
            value={form}
            error={formError}
            saving={saving}
            onChange={handleChange}
            onFieldsChange={handleFieldsChange}
            onSubmit={handleSubmit}
            onCancel={() => setModalOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default ClientsPage
