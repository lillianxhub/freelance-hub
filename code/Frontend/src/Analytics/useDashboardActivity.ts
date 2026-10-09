import { useEffect, useState } from 'react'
import { getErrorMessage } from '../api/apiError'
import type { DashboardActivity, DashboardChartPeriod } from '../types/dashboard'

interface DashboardActivityState {
  activity: DashboardActivity | null
  loading: boolean
  error: string
  retry: () => void
}

export function useDashboardActivity(
  period: DashboardChartPeriod,
  loadActivity: (period: DashboardChartPeriod) => Promise<DashboardActivity>,
): DashboardActivityState {
  const [activity, setActivity] = useState<DashboardActivity | null>(null)
  const [loading, setLoading] = useState(() => period !== 'WEEK')
  const [error, setError] = useState('')
  const [requestKey, setRequestKey] = useState(0)

  useEffect(() => {
    if (period === 'WEEK') {
      return undefined
    }

    let active = true
    loadActivity(period)
      .then((result) => {
        if (active) setActivity(result)
      })
      .catch((reason: unknown) => {
        if (active) setError(getErrorMessage(reason, 'ไม่สามารถโหลดกราฟได้'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [loadActivity, period, requestKey])

  return { activity, loading, error, retry: () => setRequestKey((key) => key + 1) }
}
