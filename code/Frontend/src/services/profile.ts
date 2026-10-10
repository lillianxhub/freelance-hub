import { api } from '../api/apiClient'
import type { ApiResponse, ApiUser } from '../types/api'
import type { Profile, ProfileInput } from '../types/profile'

const profileImageKey = (profileId: string) => `freelance-hub:profile-image:${profileId}`
function getStorage(): Storage | null {
  if (typeof window === 'undefined') return null
  try {
    return window.localStorage
  } catch {
    return null
  }
}
export function getStoredProfileImage(profileId: string | undefined): string {
  return profileId ? getStorage()?.getItem(profileImageKey(profileId)) || '' : ''
}
export function saveStoredProfileImage(profileId: string | undefined, imageData: string): void {
  if (profileId) getStorage()?.setItem(profileImageKey(profileId), imageData)
}
export function removeStoredProfileImage(profileId: string | undefined): void {
  if (profileId) getStorage()?.removeItem(profileImageKey(profileId))
}
export function changePassword(
  oldPassword: string,
  newPassword: string,
): Promise<ApiResponse<void>> {
  return api.patch<void>('/users/me/password', { oldPassword, newPassword })
}

function emptyString(value: string | null | undefined): string {
  return value ?? ''
}
function normalizePhone(value: string | null | undefined): string {
  const digits = emptyString(value).replace(/\D/g, '')
  if (digits.startsWith('66') && digits.length === 11) return `0${digits.slice(2)}`
  return digits.slice(0, 10)
}
export function toProfile(user: ApiUser): Profile {
  const fullName =
    user.displayName || [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email
  return {
    id: user.id,
    owner_id: user.id,
    full_name: fullName,
    display_name: emptyString(user.displayName) || fullName,
    first_name: emptyString(user.firstName),
    last_name: emptyString(user.lastName),
    email: user.email,
    phone: normalizePhone(user.phone),
    address: emptyString(user.address),
    city: '',
    country: '',
    postal_code: emptyString(user.postalCode),
    province: emptyString(user.province),
    district: emptyString(user.district),
    sub_district: emptyString(user.subdistrict),
    tax_id: emptyString(user.taxId),
    logo_url: getStoredProfileImage(user.id),
    bank_name: '',
    bank_account_name: '',
    bank_account_number: '',
    timezone: 'Asia/Bangkok',
    currency: 'THB',
    default_tax_rate: 0,
    default_hourly_rate: 0,
    bio: emptyString(user.bio),
    created_at: user.createdAt,
  }
}
export async function loadProfile(): Promise<Profile> {
  return toProfile((await api.get<ApiUser>('/users/me')).data)
}
export async function updateProfile(input: ProfileInput): Promise<ApiResponse<Profile>> {
  const response = await api.patch<ApiUser>('/users/me', {
    displayName: input.display_name || undefined,
    firstName: input.first_name || undefined,
    lastName: input.last_name || undefined,
    phone: input.phone || undefined,
    address: input.address || undefined,
    subdistrict: input.sub_district || undefined,
    district: input.district || undefined,
    province: input.province || undefined,
    postalCode: input.postal_code || undefined,
    taxId: input.tax_id || undefined,
    bio: input.bio || undefined,
  })
  return { ...response, data: toProfile(response.data) }
}
