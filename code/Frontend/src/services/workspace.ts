import { api } from '../api/apiClient'
import type { ApiClient, ApiProject, ApiResponse, ApiTask, ApiTimeEntry, ApiUser } from '../types/api'
import type { Client } from '../types/client'
import type { Profile } from '../types/profile'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import type { TimeEntry } from '../types/timeTracking'
import type { ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from '../types/workspace'
import { getStoredProfileImage } from './profile'

const unsupportedResources = new Set<ResourceName>([
  'time_entries', 'finance_entries', 'invoices', 'invoice_items', 'payments',
])

function emptyString(value: string | null | undefined): string {
  return value ?? ''
}

function normalizePhone(value: string | null | undefined): string {
  const digits = emptyString(value).replace(/\D/g, '')
  if (digits.startsWith('66') && digits.length === 11) return `0${digits.slice(2)}`
  return digits.slice(0, 10)
}

export function toProfile(user: ApiUser): Profile {
  const fullName = user.displayName || [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email
  return {
    id: user.id,
    owner_id: user.id,
    full_name: fullName,
    display_name: emptyString(user.displayName) || fullName,
    first_name: emptyString(user.firstName),
    last_name: emptyString(user.lastName),
    email: user.email,
    phone: normalizePhone(user.phone),
    address: emptyString(user.address),
    city: '',
    country: '',
    postal_code: emptyString(user.postalCode),
    province: emptyString(user.province),
    district: emptyString(user.district),
    sub_district: emptyString(user.subdistrict),
    tax_id: emptyString(user.taxId),
    logo_url: getStoredProfileImage(user.id),
    bank_name: '',
    bank_account_name: '',
    bank_account_number: '',
    timezone: 'Asia/Bangkok',
    currency: 'THB',
    default_tax_rate: 0,
    default_hourly_rate: 0,
    bio: emptyString(user.bio),
    created_at: user.createdAt,
  }
}

function profilePayload(input: ResourceInput<'profiles'>) {
  return {
    displayName: input.display_name || undefined,
    firstName: input.first_name || undefined,
    lastName: input.last_name || undefined,
    phone: input.phone || undefined,
    address: input.address || undefined,
    subdistrict: input.sub_district || undefined,
    district: input.district || undefined,
    province: input.province || undefined,
    postalCode: input.postal_code || undefined,
    taxId: input.tax_id || undefined,
    bio: input.bio || undefined,
  }
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
    province: '',
    district: '',
    sub_district: '',
    postal_code: '',
    tax_id: emptyString(source.taxId),
    notes: emptyString(source.notes),
    status: source.status,
    color: '#4F6BFF',
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  }
}

export function toProject(source: ApiProject): Project {
  return {
    id: source.id,
    owner_id: '',
    client_id: source.clientId,
    name: source.name,
    description: emptyString(source.description),
    color: source.color || '#4F6BFF',
    status: source.status,
    billing_type: 'HOURLY',
    hourly_rate: null,
    fixed_price: null,
    budget_hours: source.targetMinutes ? source.targetMinutes / 60 : null,
    budget_amount: null,
    currency: 'THB',
    start_date: emptyString(source.startDate),
    end_date: emptyString(source.endDate),
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  }
}

export function toTask(source: ApiTask): Task {
  return {
    id: source.id,
    owner_id: '',
    project_id: source.projectId,
    name: source.name,
    description: emptyString(source.description),
    status: source.status === 'OPEN' ? 'TODO' : source.status === 'COMPLETED' ? 'DONE' : 'IN_PROGRESS',
    sort_order: source.sortOrder,
    due_date: '',
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  }
}

export function toTimeEntry(source: ApiTimeEntry): TimeEntry {
  return {
    id: source.id,
    owner_id: '',
    project_id: source.projectId,
    task_id: source.taskId || null,
    description: emptyString(source.description),
    started_at: source.startedAt,
    ended_at: source.endedAt || null,
    duration_minutes: source.durationMinutes ?? null,
    billable: true,
    rate_snapshot: 0,
    currency: 'THB',
    invoice_id: null,
    created_at: source.createdAt,
    updated_at: source.updatedAt,
  }
}

function clientPayload(input: ResourceInput<'clients'>) {
  const location = [
    input.sub_district ? `ตำบล/แขวง ${input.sub_district}` : '',
    input.district ? `อำเภอ/เขต ${input.district}` : '',
    input.province ? `จังหวัด ${input.province}` : '',
    input.postal_code ? `รหัสไปรษณีย์ ${input.postal_code}` : '',
  ].filter(Boolean).join(' ')
  return {
    name: input.name,
    companyName: input.company_name || undefined,
    email: input.email || undefined,
    phone: input.phone || undefined,
    address: [input.address, location].filter(Boolean).join(', ') || undefined,
    taxId: input.tax_id || undefined,
    notes: input.notes || undefined,
  }
}

function projectPayload(input: ResourceInput<'projects'>) {
  return {
    clientId: input.client_id,
    name: input.name,
    description: input.description || undefined,
    startDate: input.start_date || undefined,
    endDate: input.end_date || undefined,
    color: input.color || undefined,
    targetMinutes: input.budget_hours ? Math.round(Number(input.budget_hours) * 60) : undefined,
  }
}

export async function listClients(): Promise<Client[]> {
  const response = await api.get<ApiClient[]>('/clients?size=100&sortBy=name&direction=ASC')
  return response.data.map(toClient)
}

export async function listProjects(): Promise<Project[]> {
  const response = await api.get<ApiProject[]>('/projects?size=100&sort=name,asc')
  return response.data.map(toProject)
}

export async function listTasks(projects: readonly Project[]): Promise<Task[]> {
  const pages = await Promise.all(projects.map((project) => api.get<ApiTask[]>(`/projects/${project.id}/tasks?size=100&sort=sortOrder,asc`)))
  return pages.flatMap((page) => page.data.map(toTask))
}

export async function listTimeEntries(): Promise<TimeEntry[]> {
  const response = await api.get<ApiTimeEntry[]>('/time-entries?size=100&sortBy=startedAt&direction=DESC')
  return response.data.map(toTimeEntry)
}

export function createEmptyWorkspace(): WorkspaceData {
  return { profiles: [], clients: [], projects: [], tasks: [], time_entries: [], finance_entries: [], invoices: [], invoice_items: [], payments: [] }
}

export async function loadAllResources(): Promise<WorkspaceData> {
  const [user, clients, projects, timeEntries] = await Promise.all([
    api.get<ApiUser>('/users/me').then((response) => response.data),
    listClients(),
    listProjects(),
    listTimeEntries(),
  ])
  const tasks = await listTasks(projects)
  return { ...createEmptyWorkspace(), profiles: [toProfile(user)], clients, projects, tasks, time_entries: timeEntries }
}

async function saveClient(input: ResourceInput<'clients'>): Promise<Client> {
  if (!input.id) return toClient((await api.post<ApiClient>('/clients', clientPayload(input))).data)
  if (input.status === 'ARCHIVED') {
    await api.delete(`/clients/${input.id}`)
    return { ...input, status: 'ARCHIVED' } as Client
  }
  return toClient((await api.patch<ApiClient>(`/clients/${input.id}`, clientPayload(input))).data)
}

export async function updateProfile(input: ResourceInput<'profiles'>): Promise<ApiResponse<Profile>> {
  const response = await api.patch<ApiUser>('/users/me', profilePayload(input))
  return { ...response, data: toProfile(response.data) }
}

async function saveProject(input: ResourceInput<'projects'>): Promise<Project> {
  if (!input.id) return toProject((await api.post<ApiProject>('/projects', projectPayload(input))).data)
  if (input.status === 'ARCHIVED') {
    await api.delete(`/projects/${input.id}`)
    return { ...input, status: 'ARCHIVED' } as Project
  }
  return toProject((await api.patch<ApiProject>(`/projects/${input.id}`, projectPayload(input))).data)
}

async function saveTask(input: ResourceInput<'tasks'>): Promise<Task> {
  if (!input.project_id) throw new Error('Task ต้องระบุ project_id')
  if (!input.id) {
    return toTask((await api.post<ApiTask>(`/projects/${input.project_id}/tasks`, {
      name: input.name,
      description: input.description || undefined,
      sortOrder: input.sort_order ?? 0,
    })).data)
  }
  const task = (await api.patch<ApiTask>(`/projects/${input.project_id}/tasks/${input.id}`, {
    name: input.name,
    description: input.description || undefined,
  })).data
  if (input.status === 'DONE') return toTask((await api.patch<ApiTask>(`/projects/${input.project_id}/tasks/${input.id}/complete`)).data)
  return toTask((await api.patch<ApiTask>(`/projects/${input.project_id}/tasks/${input.id}/reorder`, { sortOrder: input.sort_order ?? task.sortOrder })).data)
}

export async function saveResource<K extends ResourceName>(resource: K, input: ResourceInput<K>): Promise<ResourceRecord<K>> {
  if (unsupportedResources.has(resource)) throw new Error(`${resource} ยังไม่มี endpoint ใน Swagger ของ backend`)
  if (resource === 'profiles') return (await updateProfile(input as ResourceInput<'profiles'>)).data as ResourceRecord<K>
  if (resource === 'clients') return await saveClient(input as ResourceInput<'clients'>) as ResourceRecord<K>
  if (resource === 'projects') return await saveProject(input as ResourceInput<'projects'>) as ResourceRecord<K>
  return await saveTask(input as ResourceInput<'tasks'>) as ResourceRecord<K>
}

export async function deleteResource(resource: ResourceName, id: string, projectId?: string): Promise<void> {
  if (unsupportedResources.has(resource)) throw new Error(`${resource} ยังไม่มี endpoint ใน Swagger ของ backend`)
  if (resource === 'clients') {
    await api.delete(`/clients/${id}`)
    return
  }
  if (resource === 'projects') {
    await api.delete(`/projects/${id}`)
    return
  }
  if (!projectId) throw new Error('การลบ Task ต้องระบุ projectId')
  await api.delete(`/projects/${projectId}/tasks/${id}`)
}
