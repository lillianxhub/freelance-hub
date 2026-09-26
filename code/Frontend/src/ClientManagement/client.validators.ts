import type { ResourceInput } from '../types/workspace'
import { isValidEmail } from '../Authentication/authentication.validators'

export function validateClient(client: ResourceInput<'clients'>): string | null {
  if (!client.name.trim() && !client.company_name.trim()) return 'กรุณากรอกชื่อผู้ติดต่อหรือชื่อบริษัทอย่างน้อยหนึ่งรายการ'
  if (client.email && !isValidEmail(client.email)) return 'รูปแบบอีเมลไม่ถูกต้อง'
  return null
}
