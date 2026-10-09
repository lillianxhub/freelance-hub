import { api } from '../api/apiClient'
import type { ApiClient, ApiMeta } from '../types/api'
import type { Client, ClientInput } from '../types/client'

function emptyString(value: string | null | undefined): string {
  return value ?? ''
}

function toClient(source: ApiClient): Client {
  return {
    id: source.id,
    owner_id: '',
    name: source.name,
    company_name: emptyString(source.companyName),
    email: emptyString(source.email),
    phone: emptyString(source.phone),
    address: emptyString(source.address),
    province: emptyString(source.province),
    district: emptyString(source.district),
    sub_district: emptyString(source.subdistrict),
    postal_code: emptyString(source.postalCode),
    tax_id: emptyString(source.taxId),
    notes: emptyString(source.notes),
    status: source.isActive === false ? 'ARCHIVED' : source.status,
    color: '#4F6BFF',
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  }
}

function clientPayload(input: ClientInput) {
  return {
    name: input.name,
    companyName: input.company_name || undefined,
    email: input.email || undefined,
    phone: input.phone || undefined,
    address: input.address || undefined,
    subdistrict: input.sub_district || undefined,
    district: input.district || undefined,
    province: input.province || undefined,
    postalCode: input.postal_code || undefined,
    taxId: input.tax_id || undefined,
    notes: input.notes || undefined,
  }
}

export async function getClientById(id: string): Promise<Client> {
  return toClient((await api.get<ApiClient>(`/clients/${encodeURIComponent(id)}`)).data)
}

export async function listClients(): Promise<Client[]> {
  return (
    await api.get<ApiClient[]>('/clients?page=1&limit=10&sortBy=name&direction=ASC')
  ).data.map(toClient)
}

export interface ClientsPageResult {
  clients: Client[]
  meta: ApiMeta
}
export interface ClientListFilters {
  search?: string
  status?: Client['status']
  sortBy?: 'name' | 'companyName' | 'email' | 'createdAt' | 'updatedAt'
  direction?: 'ASC' | 'DESC'
}

export async function listClientsPage(
  page = 1,
  limit = 10,
  filters: ClientListFilters = {},
): Promise<ClientsPageResult> {
  const query = new URLSearchParams({
    page: String(page),
    limit: String(limit),
    sortBy: filters.sortBy ?? 'name',
    direction: filters.direction ?? 'ASC',
  })
  if (filters.search?.trim()) query.set('search', filters.search.trim())
  if (filters.status) query.set('status', filters.status)
  const response = await api.get<ApiClient[]>(`/clients?${query.toString()}`)
  return {
    clients: response.data.map(toClient),
    meta: response.meta ?? { page, limit, total: response.data.length, totalPages: 1 },
  }
}

export async function listClientOptions(): Promise<Client[]> {
  const filters: ClientListFilters = { status: 'ACTIVE', sortBy: 'name', direction: 'ASC' }
  const firstPage = await listClientsPage(1, 100, filters)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.meta.totalPages - 1) }, (_, index) =>
      listClientsPage(index + 2, 100, filters),
    ),
  )
  return [...firstPage.clients, ...remainingPages.flatMap((page) => page.clients)]
}

export async function saveClient(input: ClientInput): Promise<Client> {
  if (!input.id) return toClient((await api.post<ApiClient>('/clients', clientPayload(input))).data)
  if (input.status === 'ARCHIVED') return updateClientStatus(input.id, false)
  const updated = toClient(
    (await api.patch<ApiClient>(`/clients/${input.id}`, clientPayload(input))).data,
  )
  return updated.status === 'ARCHIVED' ? updateClientStatus(input.id, true) : updated
}

export async function updateClientStatus(id: string, isActive: boolean): Promise<Client> {
  return toClient(
    (await api.patch<ApiClient>(`/clients/${encodeURIComponent(id)}/status`, { isActive })).data,
  )
}

export async function deleteClient(id: string): Promise<void> {
  await updateClientStatus(id, false)
}
