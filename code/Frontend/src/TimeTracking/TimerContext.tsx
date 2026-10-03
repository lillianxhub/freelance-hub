import { createContext, useCallback, useEffect, useState, type PropsWithChildren } from 'react'
import { getCurrentTimer } from '../services/timeTracking'
import type { ApiCurrentTimer } from '../types/api'
import type { CurrentTimerContextValue } from '../types/timer'

export const TimerContext = createContext<CurrentTimerContextValue | null>(null)

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
    void refreshCurrentTimer()
  }, [refreshCurrentTimer])

  return (
    <TimerContext.Provider value={{ currentTimer, refreshCurrentTimer }}>
      {children}
    </TimerContext.Provider>
  )
}
