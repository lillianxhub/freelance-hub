import type { Session, User } from '@supabase/supabase-js'
import { demoUser, type DemoUser } from '../data/demoData'
import { getSupabaseClient, isDemoMode } from '../lib/supabase'

const demoUsersKey = 'freelance-hub-demo-users'
const demoSessionKey = 'freelance-hub-demo-session'

export type AuthUser = Pick<User, 'id' | 'email' | 'user_metadata'>
export type AuthSession = Pick<Session, 'user'> | { user: AuthUser }

const wait = (milliseconds = 350) => new Promise<void>((resolve) => setTimeout(resolve, milliseconds))

function parseStoredValue<T>(key: string): T | null {
  const stored = localStorage.getItem(key)
  if (!stored) return null
  try {
    return JSON.parse(stored) as T
  } catch {
    localStorage.removeItem(key)
    return null
  }
}

function loadDemoUsers(): DemoUser[] {
  return parseStoredValue<DemoUser[]>(demoUsersKey) || [demoUser]
}

function saveDemoUsers(users: DemoUser[]) {
  localStorage.setItem(demoUsersKey, JSON.stringify(users))
}

export async function getCurrentSession(): Promise<AuthSession | null> {
  if (isDemoMode) {
    const user = parseStoredValue<AuthUser>(demoSessionKey)
    return user ? { user } : null
  }

  const { data, error } = await getSupabaseClient().auth.getSession()
  if (error) throw error
  return data.session
}

export async function signIn(email: string, password: string): Promise<{ user: AuthUser }> {
  if (isDemoMode) {
    await wait()
    const normalizedEmail = email.trim().toLowerCase()
    const user = loadDemoUsers().find(
      (item) => item.email.toLowerCase() === normalizedEmail && item.password === password,
    )
    if (!user) throw new Error('อีเมลหรือรหัสผ่านไม่ถูกต้อง')

    const sessionUser: AuthUser = { id: user.id, email: user.email, user_metadata: { full_name: user.full_name } }
    localStorage.setItem(demoSessionKey, JSON.stringify(sessionUser))
    return { user: sessionUser }
  }

  const { data, error } = await getSupabaseClient().auth.signInWithPassword({ email, password })
  if (error) throw error
  if (!data.user) throw new Error('ไม่พบข้อมูลผู้ใช้หลังเข้าสู่ระบบ')
  return { user: data.user }
}

export async function signUp(fullName: string, email: string, password: string): Promise<void> {
  if (isDemoMode) {
    await wait()
    const users = loadDemoUsers()
    const normalizedEmail = email.trim().toLowerCase()
    if (users.some((user) => user.email.toLowerCase() === normalizedEmail)) throw new Error('อีเมลนี้ถูกใช้งานแล้ว')
    saveDemoUsers([...users, { id: crypto.randomUUID(), email: normalizedEmail, full_name: fullName.trim(), password }])
    return
  }

  const { error } = await getSupabaseClient().auth.signUp({
    email,
    password,
    options: { data: { full_name: fullName.trim() } },
  })
  if (error) throw error
}

export async function signOut(): Promise<void> {
  if (isDemoMode) {
    localStorage.removeItem(demoSessionKey)
    return
  }
  const { error } = await getSupabaseClient().auth.signOut()
  if (error) throw error
}

export async function sendPasswordReset(email: string): Promise<void> {
  if (isDemoMode) {
    await wait()
    if (!loadDemoUsers().some((user) => user.email.toLowerCase() === email.trim().toLowerCase())) throw new Error('ไม่พบบัญชีผู้ใช้นี้')
    return
  }

  const redirectTo = `${window.location.origin}/login`
  const { error } = await getSupabaseClient().auth.resetPasswordForEmail(email, { redirectTo })
  if (error) throw error
}

export function subscribeToAuthChanges(callback: (session: AuthSession | null) => void): () => void {
  if (isDemoMode) return () => undefined
  const { data } = getSupabaseClient().auth.onAuthStateChange((_event, session) => callback(session))
  return () => data.subscription.unsubscribe()
}
