import { isValidPhone } from '../Authentication/authentication.validators'
import type { Profile, ProfileFieldErrors, ProfileFieldName } from '../types/profile'

const requiredFields: Exclude<ProfileFieldName, 'tax_id'>[] = [
  'display_name',
  'first_name',
  'last_name',
  'phone',
  'address',
  'province',
  'district',
  'sub_district',
  'postal_code',
]

export function validateProfileField(field: ProfileFieldName, profile: Pick<Profile, ProfileFieldName>): string | undefined {
  const value = profile[field].trim()
  if (field !== 'tax_id' && !value) return 'กรุณากรอกข้อมูลในช่องนี้'
  if (field === 'phone' && !isValidPhone(profile.phone)) return 'กรุณากรอกเบอร์โทรศัพท์ 10 หลัก'
  if (field === 'postal_code' && !/^\d{5}$/.test(profile.postal_code)) return 'กรุณาเลือกรหัสไปรษณีย์ 5 หลัก'
  if (field === 'tax_id' && value && !/^\d{13}$/.test(profile.tax_id)) return 'กรุณากรอกเลขประจำตัวผู้เสียภาษี 13 หลัก'
  return undefined
}

export function validateProfileFields(profile: Pick<Profile, ProfileFieldName>): ProfileFieldErrors {
  const fields: ProfileFieldName[] = [...requiredFields, 'tax_id']
  return fields.reduce<ProfileFieldErrors>((errors, field) => {
    const error = validateProfileField(field, profile)
    if (error) errors[field] = error
    return errors
  }, {})
}
