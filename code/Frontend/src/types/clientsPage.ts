import type { ChangeEvent, FormEvent } from 'react'
import type { Client, ClientStatus } from './client'
import type { ClientInput } from './client'

export type ClientFilter = 'ALL' | ClientStatus
export type ClientSort = 'UPDATED_DESC' | 'NAME_ASC' | 'CREATED_ASC'
export type ClientFieldName =
  | 'name'
  | 'company_name'
  | 'email'
  | 'phone'
  | 'address'
  | 'province'
  | 'district'
  | 'sub_district'
  | 'postal_code'
  | 'tax_id'
  | 'status'
export type ClientFieldErrors = Partial<Record<ClientFieldName, string>>

export interface ClientFormProps {
  value: ClientInput
  error: string
  saving: boolean
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void
  onFieldsChange: (values: Partial<ClientInput>) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

export interface ClientCardProps {
  client: Client
  projectCount: number
  minutes: number
  revenue: number
  onEdit: (client: Client) => void
  onArchive: (client: Client) => void
}

export interface ClientTableProps {
  clients: readonly Client[]
}

export interface ClientStatusProps {
  status: ClientStatus
}
