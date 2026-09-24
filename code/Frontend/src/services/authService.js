import { demoUser } from '../data/demoData'
import { isDemoMode, supabase } from '../lib/supabase'

const demoUsersKey = 'freelance-hub-demo-users'
const demoSessionKey = 'freelance-hub-demo-session'

const wait = (milliseconds = 350) => new Promise((resolve) => setTimeout(resolve, milliseconds))

function loadDemoUsers() {
  try {
    return JSON.parse(localStorage.getItem(demoUsersKey)) || [demoUser]
  } catch {
    return [demoUser]
  }
}

function saveDemoUsers(users) {
  localStorage.setItem(demoUsersKey, JSON.stringify(users))
}

export async function getCurrentSession() {
  if (isDemoMode) {
    try {
      const user = JSON.parse(localStorage.getItem(demoSessionKey))
      return user ? { user } : null
    } catch {
      localStorage.removeItem(demoSessionKey)
      return null
    }
  }

  const { data, error } = await supabase.auth.getSession()
  if (error) throw error
  return data.session
}

export async function signIn(email, password) {
  if (isDemoMode) {
    await wait()
    const normalizedEmail = email.trim().toLowerCase()
    const user = loadDemoUsers().find(
      (item) => item.email.toLowerCase() === normalizedEmail && item.password === password,
    )
    if (!user) throw new Error('อีเมลหรือรหัสผ่านไม่ถูกต้อง')

    const sessionUser = { id: user.id, email: user.email, user_metadata: { full_name: user.full_name } }
    localStorage.setItem(demoSessionKey, JSON.stringify(sessionUser))
    return { user: sessionUser }
  }

  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw error
  return data
}

export async function signUp(fullName, email, password) {
  if (isDemoMode) {
    await wait()
    const users = loadDemoUsers()
    const normalizedEmail = email.trim().toLowerCase()
    if (users.some((user) => user.email.toLowerCase() === normalizedEmail)) {
      throw new Error('อีเมลนี้ถูกใช้งานแล้ว')
    }

    const user = {
      id: crypto.randomUUID(),
      email: normalizedEmail,
      full_name: fullName.trim(),
      password,
    }
    saveDemoUsers([...users, user])
    return { user }
  }

  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: { data: { full_name: fullName.trim() } },
  })
  if (error) throw error
  return data
}

export async function signOut() {
  if (isDemoMode) {
    localStorage.removeItem(demoSessionKey)
    return
  }
  const { error } = await supabase.auth.signOut()
  if (error) throw error
}

export async function sendPasswordReset(email) {
  if (isDemoMode) {
    await wait()
    if (!loadDemoUsers().some((user) => user.email.toLowerCase() === email.trim().toLowerCase())) {
      throw new Error('ไม่พบบัญชีผู้ใช้นี้')
    }
    return
  }

  const redirectTo = `${window.location.origin}/login`
  const { error } = await supabase.auth.resetPasswordForEmail(email, { redirectTo })
  if (error) throw error
}

export function subscribeToAuthChanges(callback) {
  if (isDemoMode) return () => undefined
  const { data } = supabase.auth.onAuthStateChange((_event, session) => callback(session))
  return () => data.subscription.unsubscribe()
}
