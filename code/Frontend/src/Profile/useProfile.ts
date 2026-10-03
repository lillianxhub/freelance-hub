import { useContext } from 'react'
import { ProfileContext } from './ProfileContext'
import type { ProfileContextValue } from './ProfileContext'

export function useProfile(): ProfileContextValue {
  const context = useContext(ProfileContext)
  if (!context) throw new Error('useProfile must be used inside ProfileProvider')
  return context
}
