import { createContext, useCallback, type PropsWithChildren } from 'react'
import { updateProfile as updateProfileRequest, loadProfile } from '../services/profile'
import { useAsyncData } from '../shared/useAsyncData'
import type { AsyncDataState } from '../types/asyncData'
import type { Profile, ProfileInput } from '../types/profile'
import type { ApiResponse } from '../types/api'

export interface ProfileContextValue extends AsyncDataState<Profile | null> {
  updateProfile: (input: ProfileInput) => Promise<ApiResponse<Profile>>
}
export const ProfileContext = createContext<ProfileContextValue | null>(null)

export function ProfileProvider({ children }: PropsWithChildren) {
  const load = useCallback(() => loadProfile(), [])
  const state = useAsyncData<Profile | null>(load, null)
  const value: ProfileContextValue = { ...state, async updateProfile(input) {
    const response = await updateProfileRequest(input)
    await state.refresh()
    return response
  } }
  return <ProfileContext.Provider value={value}>{children}</ProfileContext.Provider>
}
