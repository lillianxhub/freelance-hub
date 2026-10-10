import { useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react'
import {
  getCurrentSession,
  signIn,
  signOut,
  signUp,
  subscribeToAuthChanges,
} from '../services/auth'
import type { AuthContextValue, AuthSession, RegisterInput } from '../types/auth'
import { AuthContext } from './useAuthentication'

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null)
  const [loading, setLoading] = useState(true)
  const sessionVersion = useRef(0)

  useEffect(() => {
    let active = true
    const version = sessionVersion.current
    getCurrentSession()
      .then((currentSession) => {
        if (active && version === sessionVersion.current) setSession(currentSession)
      })
      .catch(() => {
        if (active && version === sessionVersion.current) setSession(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    const unsubscribe = subscribeToAuthChanges((nextSession) => {
      sessionVersion.current++
      setSession(nextSession)
    })
    return () => {
      active = false
      unsubscribe()
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      user: session?.user || null,
      loading,
      async login(email: string, password: string) {
        const version = ++sessionVersion.current
        const result = await signIn(email, password)
        if (version === sessionVersion.current) setSession({ user: result.user })
      },
      async register(input: RegisterInput) {
        await signUp(input)
      },
      async logout() {
        await signOut()
      },
    }),
    [loading, session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
