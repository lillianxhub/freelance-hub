import { api } from '../api/apiClient'
import type { ApiResponse } from '../types/api'

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
  if (!profileId) return ''
  return getStorage()?.getItem(profileImageKey(profileId)) || ''
}

export function saveStoredProfileImage(profileId: string | undefined, imageData: string): void {
  if (!profileId) return
  getStorage()?.setItem(profileImageKey(profileId), imageData)
}

export function removeStoredProfileImage(profileId: string | undefined): void {
  if (!profileId) return
  getStorage()?.removeItem(profileImageKey(profileId))
}

export function changePassword(oldPassword: string, newPassword: string): Promise<ApiResponse<void>> {
  return api.patch<void>('/users/me/password', { oldPassword, newPassword })
}
