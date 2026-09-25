import { createContext, useContext } from 'react'
import type { AuthSession, AuthUser } from '../services/authService'

export interface AuthContextValue {
  session: AuthSession | null
  user: AuthUser | null
  loading: boolean
  isDemoMode: boolean
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
