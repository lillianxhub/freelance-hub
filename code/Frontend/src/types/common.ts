export type CurrencyCode = 'THB' | 'USD' | 'EUR' | 'SGD'

export interface OwnedRecord {
  id: string
  owner_id: string
  created_at?: string
  updated_at?: string
}
