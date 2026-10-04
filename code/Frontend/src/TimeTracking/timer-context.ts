import { createContext } from 'react'
import type { CurrentTimerContextValue } from '../types/timer'

export const TimerContext = createContext<CurrentTimerContextValue | null>(null)
