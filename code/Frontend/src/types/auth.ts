import type { ChangeEventHandler, HTMLInputTypeAttribute } from 'react'

export interface AuthInputProps {
  label: string
  type: HTMLInputTypeAttribute
  name: string
  value: string
  onChange: ChangeEventHandler<HTMLInputElement>
  autoComplete?: string
  minLength?: number
}

export interface BackendUser {
  id: string
  email: string
  displayName?: string
}

export interface AuthResponse {
  token: string
  user: BackendUser
}

export interface AuthUser {
  id: string
  email: string
  user_metadata: { full_name: string }
}

export interface AuthSession {
  user: AuthUser
}

export interface AuthContextValue {
  session: AuthSession | null
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}
