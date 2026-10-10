import { useCallback, useEffect, useState, type PropsWithChildren } from 'react'
import { TimerContext } from './timer-context'
import { getCurrentTimer } from '../services/timeTracking'
import type { ApiCurrentTimer } from '../types/api'

export function TimerProvider({ children }: PropsWithChildren) {
  const [currentTimer, setCurrentTimer] = useState<ApiCurrentTimer | null>(null)

  const refreshCurrentTimer = useCallback(async () => {
    try {
      setCurrentTimer(await getCurrentTimer())
    } catch {
      setCurrentTimer(null)
    }
  }, [])

  useEffect(() => {
    let active = true
    getCurrentTimer()
      .then((timer) => {
        if (active) setCurrentTimer(timer)
      })
      .catch(() => {
        if (active) setCurrentTimer(null)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <TimerContext.Provider value={{ currentTimer, refreshCurrentTimer }}>
      {children}
    </TimerContext.Provider>
  )
}
