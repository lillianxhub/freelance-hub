import { createContext } from 'react'
import type { DashboardContextValue } from '../types/dashboard'

export const DashboardContext = createContext<DashboardContextValue | null>(null)
