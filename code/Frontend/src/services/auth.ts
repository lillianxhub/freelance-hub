import { api, getApiToken, setApiToken } from '../api/apiClient'
import { ApiError } from '../api/apiError'
import type { AuthResponse, AuthSession, AuthUser, BackendUser } from '../types/auth'

function toAuthUser(user: BackendUser): AuthUser {
  return { id: user.id, email: user.email, user_metadata: { full_name: user.displayName || user.email } }
}

export async function getCurrentSession(): Promise<AuthSession | null> {
  if (!getApiToken()) return null
  try {
    const user = await api.get<BackendUser>('/users/me')
    return { user: toAuthUser(user) }
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      setApiToken(null)
      return null
    }
    throw error
  }
}

export async function signIn(email: string, password: string): Promise<AuthSession> {
  const result = await api.post<AuthResponse>('/auth/login', { email: email.trim(), password })
  setApiToken(result.token)
  return { user: toAuthUser(result.user) }
}

export async function signUp(fullName: string, email: string, password: string): Promise<void> {
  await api.post<AuthResponse>('/auth/register', { displayName: fullName.trim(), email: email.trim(), password })
}

export async function signOut(): Promise<void> {
  setApiToken(null)
}

export function subscribeToAuthChanges(callback: (session: AuthSession | null) => void): () => void {
  const onStorage = (event: StorageEvent) => {
    if (event.key === 'freelance-hub-api-token') getCurrentSession().then(callback)
  }
  window.addEventListener('storage', onStorage)
  return () => window.removeEventListener('storage', onStorage)
}
