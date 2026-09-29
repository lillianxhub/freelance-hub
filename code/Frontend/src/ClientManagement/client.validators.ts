import type { ResourceInput } from '../types/workspace'
import { isValidEmail, isValidPhone } from '../Authentication/authentication.validators'
import type { ClientFieldErrors, ClientFieldName } from '../types/clientsPage'

export function normalizeDigits(value: string, maximumLength: number): string {
  return value.replace(/\D/g, '').slice(0, maximumLength)
}

function isRequired(value: string): boolean {
  return value.trim().length > 0
}

export function validateClientField(
  field: ClientFieldName,
  client: ResourceInput<'clients'>,
): string | undefined {
  const value = client[field]

  if (!isRequired(value)) return 'กรุณากรอกข้อมูลในช่องนี้'
  if (field === 'email' && !isValidEmail(client.email)) return 'กรุณากรอกอีเมลให้ถูกต้อง'
  if (field === 'phone' && !isValidPhone(client.phone)) return 'กรุณากรอกเบอร์โทรศัพท์ 10 หลัก'
  if (field === 'postal_code' && !/^\d{5}$/.test(client.postal_code)) return 'กรุณาเลือกรหัสไปรษณีย์ 5 หลัก'
  if (field === 'tax_id' && !/^\d{13}$/.test(client.tax_id)) return 'กรุณากรอกเลขประจำตัวผู้เสียภาษี 13 หลัก'

  return undefined
}

export function validateClientFields(client: ResourceInput<'clients'>): ClientFieldErrors {
  const fields: ClientFieldName[] = [
    'name',
    'company_name',
    'email',
    'phone',
    'address',
    'province',
    'district',
    'sub_district',
    'postal_code',
    'tax_id',
    'status',
  ]

  return fields.reduce<ClientFieldErrors>((errors, field) => {
    const error = validateClientField(field, client)
    if (error) errors[field] = error
    return errors
  }, {})
}

export function validateClient(client: ResourceInput<'clients'>): string | null {
  if (!client.name.trim() && !client.company_name.trim()) return 'กรุณากรอกชื่อผู้ติดต่อหรือชื่อบริษัทอย่างน้อยหนึ่งรายการ'
  if (client.email && !isValidEmail(client.email)) return 'รูปแบบอีเมลไม่ถูกต้อง'
  return null
}
