import { useEffect, useMemo, useState } from 'react'
import { getCurrentSession, signIn, signOut, signUp, subscribeToAuthChanges } from '../services/authService'
import { isDemoMode } from '../lib/supabase'
import { AuthContext } from './authContextValue'

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null)
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

  const value = useMemo(() => ({
    session,
    user: session?.user || null,
    loading,
    isDemoMode,
    async login(email, password) {
      const result = await signIn(email, password)
      setSession({ user: result.user })
    },
    async register(fullName, email, password) {
      return signUp(fullName, email, password)
    },
    async logout() {
      await signOut()
      setSession(null)
    },
  }), [loading, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
