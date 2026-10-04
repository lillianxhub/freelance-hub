import { createContext } from 'react'
import type { TimeEntriesContextValue } from './TimeEntriesContext'

export const TimeEntriesContext = createContext<TimeEntriesContextValue | null>(null)
