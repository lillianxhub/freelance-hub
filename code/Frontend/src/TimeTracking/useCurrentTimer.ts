import { useContext } from 'react'
import { TimerContext } from './timer-context'

export function useCurrentTimer() {
  const context = useContext(TimerContext)
  if (!context) throw new Error('useCurrentTimer ต้องใช้ภายใน TimerProvider')
  return context
}
