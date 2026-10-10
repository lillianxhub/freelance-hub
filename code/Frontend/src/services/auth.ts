import {
  api,
  getApiToken,
  refreshApiToken,
  setApiToken,
  subscribeToSessionInvalidation,
} from '../api/apiClient'
import { ApiError } from '../api/apiError'
import type { AuthResponse, AuthSession, AuthUser, BackendUser, RegisterInput } from '../types/auth'

const authSender = crypto.randomUUID()

function toAuthUser(user: BackendUser): AuthUser {
  return {
    id: user.id,
    email: user.email,
    user_metadata: { full_name: user.displayName || user.email },
  }
}

export async function getCurrentSession(): Promise<AuthSession | null> {
  if (!(await refreshApiToken())) return null
  const token = getApiToken()
  try {
    const response = await api.get<BackendUser>('/users/me')
    if (getApiToken() !== token) return null
    return { user: toAuthUser(response.data) }
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      if (getApiToken() === token) setApiToken(null)
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
    channel.postMessage({ action: 'login', sender: authSender })
    channel.close()
  }
  return { user: toAuthUser(response.data.user) }
}

export async function signUp(input: RegisterInput): Promise<void> {
  await api.post<AuthResponse>('/auth/register', {
    displayName: input.displayName.trim(),
    firstName: input.firstName.trim(),
    lastName: input.lastName.trim(),
    email: input.email.trim(),
    phone: input.phone.trim(),
    password: input.password,
  })
}

export async function signOut(): Promise<void> {
  setApiToken(null)
  if (typeof BroadcastChannel !== 'undefined') {
    const channel = new BroadcastChannel('freelance-hub-auth')
    channel.postMessage({ action: 'logout', sender: authSender })
    channel.close()
  }
  await api.post<void>('/auth/logout')
}

export function subscribeToAuthChanges(
  callback: (session: AuthSession | null) => void,
): () => void {
  const unsubscribe = subscribeToSessionInvalidation(() => callback(null))
  if (typeof BroadcastChannel === 'undefined') return unsubscribe
  const channel = new BroadcastChannel('freelance-hub-auth')
  channel.onmessage = (event: MessageEvent<string | { action: string; sender: string }>) => {
    const message = event.data
    if (typeof message !== 'string' && message?.sender === authSender) return
    const action = typeof message === 'string' ? message : message?.action
    if (action === 'logout') {
      setApiToken(null)
    } else if (action === 'login') {
      getCurrentSession()
        .then(callback)
        .catch(() => callback(null))
    }
  }
  return () => {
    unsubscribe()
    channel.close()
  }
}
