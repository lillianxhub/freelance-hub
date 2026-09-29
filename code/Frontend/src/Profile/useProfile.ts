import { useContext } from 'react'
import { ProfileContext } from './ProfileContext'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export function useProfile(): WorkspaceContextValue {
  const context = useContext(ProfileContext)
  if (!context) throw new Error('useProfile must be used inside ProfileProvider')
  return context
}
