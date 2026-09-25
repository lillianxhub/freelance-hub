import { useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { getCurrentSession, signIn, signOut, signUp, subscribeToAuthChanges, type AuthSession } from '../services/authService'
import { isDemoMode } from '../lib/supabase'
import { AuthContext, type AuthContextValue } from './authContextValue'

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    getCurrentSession()
      .then((currentSession) => {
        if (active) setSession(currentSession)
      })
      .catch(() => {
        if (active) setSession(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    const unsubscribe = subscribeToAuthChanges((nextSession) => setSession(nextSession))
    return () => {
      active = false
      unsubscribe()
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    user: session?.user || null,
    loading,
    isDemoMode,
    async login(email: string, password: string) {
      const result = await signIn(email, password)
      setSession({ user: result.user })
    },
    async register(fullName: string, email: string, password: string) {
      await signUp(fullName, email, password)
    },
    async logout() {
      await signOut()
      setSession(null)
    },
  }), [loading, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
