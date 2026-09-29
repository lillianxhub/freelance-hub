import { createContext, useCallback, type PropsWithChildren } from 'react'
import { api } from '../api/apiClient'
import { useScopedWorkspace } from '../shared/useScopedWorkspace'
import { createEmptyWorkspace, toProfile } from '../services/workspace'
import type { ApiUser } from '../types/api'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export const ProfileContext = createContext<WorkspaceContextValue | null>(null)

export function ProfileProvider({ children }: PropsWithChildren) {
  const load = useCallback(async () => {
    const user = (await api.get<ApiUser>('/users/me')).data
    return { ...createEmptyWorkspace(), profiles: [toProfile(user)] }
  }, [])
  const value = useScopedWorkspace(load)
  return <ProfileContext.Provider value={value}>{children}</ProfileContext.Provider>
}
