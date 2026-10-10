import { useCallback, useEffect, useRef, useState, type PropsWithChildren } from 'react'
import { TimerContext } from './timer-context'
import { getCurrentTimer } from '../services/timeTracking'
import type { ApiCurrentTimer } from '../types/api'

export function TimerProvider({ children }: PropsWithChildren) {
  const [currentTimer, setCurrentTimer] = useState<ApiCurrentTimer | null>(null)
  const requestId = useRef(0)
  const invalidateRequest = useCallback(() => {
    requestId.current++
  }, [])

  const refreshCurrentTimer = useCallback(async () => {
    const currentRequest = ++requestId.current
    try {
      const timer = await getCurrentTimer()
      if (currentRequest === requestId.current) setCurrentTimer(timer)
    } catch {
      if (currentRequest === requestId.current) setCurrentTimer(null)
    }
  }, [])

  useEffect(() => {
    const currentRequest = ++requestId.current
    getCurrentTimer()
      .then((timer) => {
        if (currentRequest === requestId.current) setCurrentTimer(timer)
      })
      .catch(() => {
        if (currentRequest === requestId.current) setCurrentTimer(null)
      })
    return invalidateRequest
  }, [invalidateRequest])

  return (
    <TimerContext.Provider value={{ currentTimer, refreshCurrentTimer }}>
      {children}
    </TimerContext.Provider>
  )
}
