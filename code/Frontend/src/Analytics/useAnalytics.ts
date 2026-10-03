import { useContext, useMemo } from 'react'
import { AnalyticsContext } from './AnalyticsContext'
import { summarizeTime } from '../lib/analytics'

export function useAnalytics() {
  const context = useContext(AnalyticsContext)
  if (!context) throw new Error('useAnalytics must be used inside AnalyticsProvider')
  const summary = useMemo(() => summarizeTime(context.data.time_entries), [context.data.time_entries])
  return { ...context, summary }
}
