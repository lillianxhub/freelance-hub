import { useContext } from 'react'
import { DashboardContext } from './DashboardContext'
import type { DashboardContextValue } from '../types/dashboard'

export function useDashboard(): DashboardContextValue {
  const context = useContext(DashboardContext)
  if (!context) throw new Error('useDashboard must be used inside DashboardProvider')
  return context
}
