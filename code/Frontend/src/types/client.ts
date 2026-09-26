import type { OwnedRecord } from './common'

export type ClientStatus = 'ACTIVE' | 'ARCHIVED'

export interface Client extends OwnedRecord {
  name: string
  company_name: string
  email: string
  phone: string
  address: string
  province: string
  district: string
  sub_district: string
  postal_code: string
  tax_id: string
  notes: string
  status: ClientStatus
  color: string
}
