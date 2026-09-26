import type { CurrencyCode, OwnedRecord } from './common'

export interface Profile extends OwnedRecord {
  full_name: string
  email: string
  phone: string
  address: string
  tax_id: string
  logo_url: string
  bank_name: string
  bank_account_name: string
  bank_account_number: string
  timezone: string
  currency: CurrencyCode
  date_format: string
  default_tax_rate: number
  default_hourly_rate: number
}
