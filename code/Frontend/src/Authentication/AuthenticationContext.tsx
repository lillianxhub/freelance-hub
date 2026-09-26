import { useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { getCurrentSession, signIn, signOut, signUp, subscribeToAuthChanges } from '../services/auth'
import type { AuthContextValue, AuthSession, RegisterInput } from '../types/auth'
import { AuthContext } from './useAuthentication'

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
    async login(email: string, password: string) {
      const result = await signIn(email, password)
      setSession({ user: result.user })
    },
    async register(input: RegisterInput) {
      await signUp(input)
    },
    async logout() {
      await signOut()
      setSession(null)
    },
  }), [loading, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
