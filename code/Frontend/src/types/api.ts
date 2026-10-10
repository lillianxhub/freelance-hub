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
  details?: Record<string, unknown> | null
  status?: number
  timestamp?: string
  fieldErrors?: Record<string, string> | null
  traceId?: string | null
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
  role?: 'USER' | 'ADMIN'
  isActive?: boolean
  createdAt?: string
  displayName?: string | null
  firstName?: string | null
  lastName?: string | null
  phone?: string | null
  address?: string | null
  subdistrict?: string | null
  district?: string | null
  province?: string | null
  postalCode?: string | null
  taxId?: string | null
  bio?: string | null
}

export interface ApiClient {
  id: string
  name: string
  companyName?: string
  email?: string
  phone?: string
  address?: string
  subdistrict?: string
  district?: string
  province?: string
  postalCode?: string
  taxId?: string
  notes?: string
  status: 'ACTIVE' | 'ARCHIVED'
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ApiProject {
  id: string
  clientId?: string
  name: string
  description?: string
  startDate?: string
  endDate?: string
  color?: string
  targetMinutes?: number
  targetHours?: number | null
  client?: {
    id: string
    name: string
  }
  taskProgress?: {
    totalTasks: number
    completedTasks: number
    percent: number
  }
  timeTracking?: {
    trackedSeconds: number
    trackedHours: number
    usagePercent: number
  } | null
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
  projectId?: string
  projectName?: string
  taskId?: string
  taskName?: string
  project?: {
    id: string
    name: string
  }
  task?: {
    id: string
    title: string
  } | null
  description?: string
  entryType?: 'TIMER' | 'MANUAL'
  startedAt: string
  endedAt?: string
  durationMinutes?: number
  durationSeconds?: number
  lockedAt?: string
  running: boolean
  locked: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ApiCurrentTimer {
  running: boolean
  timeEntry: {
    id: string
    project: {
      id: string
      name: string
    }
    task: {
      id: string
      title: string
    } | null
    startedAt: string
    description: string | null
  } | null
}
