import type { ChangeEvent, FormEvent } from 'react'
import type { Client, ClientStatus } from './client'
import type { ResourceInput } from './workspace'

export type ClientFilter = 'ALL' | ClientStatus
export type ClientSort = 'UPDATED_DESC' | 'NAME_ASC' | 'CREATED_ASC'

export interface ClientFormProps {
  value: ResourceInput<'clients'>
  error: string
  saving: boolean
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void
  onFieldsChange: (values: Partial<ResourceInput<'clients'>>) => void
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
