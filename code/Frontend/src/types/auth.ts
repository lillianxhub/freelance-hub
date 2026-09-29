import type { ChangeEventHandler, HTMLInputTypeAttribute, InputHTMLAttributes } from 'react'

export interface AuthInputProps extends Pick<
  InputHTMLAttributes<HTMLInputElement>,
  'autoComplete' | 'inputMode' | 'maxLength' | 'minLength' | 'onBlur' | 'pattern' | 'required'
> {
  label: string
  type: HTMLInputTypeAttribute
  name: string
  value: string
  onChange: ChangeEventHandler<HTMLInputElement>
  error?: string
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

export interface RegisterInput {
  displayName: string
  firstName: string
  lastName: string
  email: string
  phone: string
  password: string
}

export interface RegisterFormValues extends RegisterInput {
  confirmPassword: string
}

export type RegisterFieldErrors = Partial<Record<keyof RegisterFormValues, string>>

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
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}
