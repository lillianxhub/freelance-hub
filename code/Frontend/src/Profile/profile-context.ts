import { createContext } from 'react'
import type { ProfileContextValue } from './ProfileContext'

export const ProfileContext = createContext<ProfileContextValue | null>(null)
