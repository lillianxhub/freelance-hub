export type ApiOptions = Omit<RequestInit, 'method' | 'body'>

export type JsonMethod = 'POST' | 'PUT' | 'PATCH'

export interface ApiMeta {
  page: number
  limit: number
  total: number
  totalPages: number
}

export interface ApiResponseError {
  code?: string
  message?: string
  details?: unknown
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  meta: ApiMeta | null
  error: ApiResponseError | null
}

export interface ApiUser {
  id: string
  email: string
  displayName?: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
  city?: string
  country?: string
  postalCode?: string
  timezone?: string
  dateFormat?: string
}

export interface ApiClient {
  id: string
  name: string
  companyName?: string
  email?: string
  phone?: string
  address?: string
  taxId?: string
  notes?: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt?: string
  updatedAt?: string
}

export interface ApiProject {
  id: string
  clientId: string
  name: string
  description?: string
  startDate?: string
  endDate?: string
  color?: string
  targetMinutes?: number
  status: 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
  createdAt?: string
  updatedAt?: string
}

export interface ApiTask {
  id: string
  projectId: string
  name: string
  description?: string
  status: 'OPEN' | 'IN_PROGRESS' | 'COMPLETED'
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface ApiTimeEntry {
  id: string
  clientId?: string
  clientName?: string
  projectId: string
  projectName?: string
  taskId?: string
  taskName?: string
  description?: string
  entryType: 'TIMER' | 'MANUAL'
  startedAt: string
  endedAt?: string
  durationMinutes?: number
  lockedAt?: string
  running: boolean
  locked: boolean
  createdAt?: string
  updatedAt?: string
}
