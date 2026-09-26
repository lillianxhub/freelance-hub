export type ApiOptions = Omit<RequestInit, 'method' | 'body'>

export type JsonMethod = 'POST' | 'PUT' | 'PATCH'

export interface ApiPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface ApiUser {
  id: string
  email: string
  displayName?: string
  phone?: string
  address?: string
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
