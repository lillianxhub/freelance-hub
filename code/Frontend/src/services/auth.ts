import { api, refreshApiToken, setApiToken } from '../api/apiClient'
import { ApiError } from '../api/apiError'
import type { AuthResponse, AuthSession, AuthUser, BackendUser, RegisterInput } from '../types/auth'

function toAuthUser(user: BackendUser): AuthUser {
  return { id: user.id, email: user.email, user_metadata: { full_name: user.displayName || user.email } }
}

export async function getCurrentSession(): Promise<AuthSession | null> {
  if (!await refreshApiToken()) return null
  try {
    const response = await api.get<BackendUser>('/users/me')
    return { user: toAuthUser(response.data) }
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      setApiToken(null)
      return null
    }
    throw error
  }
}

export async function signIn(email: string, password: string): Promise<AuthSession> {
  const response = await api.post<AuthResponse>('/auth/login', { email: email.trim(), password })
  setApiToken(response.data.token)
  if (typeof BroadcastChannel !== 'undefined') {
    const channel = new BroadcastChannel('freelance-hub-auth')
    channel.postMessage('login')
    channel.close()
  }
  return { user: toAuthUser(response.data.user) }
}

export async function signUp(input: RegisterInput): Promise<void> {
  await api.post<BackendUser>('/auth/register', {
    displayName: input.displayName.trim(),
    firstName: input.firstName.trim(),
    lastName: input.lastName.trim(),
    email: input.email.trim(),
    phone: input.phone.trim(),
    password: input.password,
  })
}

export async function signOut(): Promise<void> {
  try {
    await api.post<void>('/auth/logout')
  } finally {
    setApiToken(null)
    if (typeof BroadcastChannel !== 'undefined') {
      const channel = new BroadcastChannel('freelance-hub-auth')
      channel.postMessage('logout')
      channel.close()
    }
  }
}

export function subscribeToAuthChanges(callback: (session: AuthSession | null) => void): () => void {
  if (typeof BroadcastChannel === 'undefined') return () => {}
  const channel = new BroadcastChannel('freelance-hub-auth')
  channel.onmessage = (event: MessageEvent) => {
    if (event.data === 'logout') {
      setApiToken(null)
      callback(null)
    } else if (event.data === 'login') {
      getCurrentSession().then(callback).catch(() => callback(null))
    }
  }
  return () => channel.close()
}
