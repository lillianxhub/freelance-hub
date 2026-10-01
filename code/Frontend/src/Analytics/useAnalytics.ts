import { useContext, useMemo } from 'react'
import { AnalyticsContext } from './AnalyticsContext'
import { summarizeTime } from '../lib/analytics'

export function useAnalytics() {
  const workspace = useContext(AnalyticsContext)
  if (!workspace) throw new Error('useAnalytics must be used inside AnalyticsProvider')
  const summary = useMemo(() => summarizeTime(workspace.data.time_entries), [workspace.data.time_entries])
  return { ...workspace, summary }
}
