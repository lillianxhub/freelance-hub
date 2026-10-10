import { useCallback, useMemo, useState } from 'react'
import { getErrorMessage } from '../api/apiError'
import { useAsyncData } from '../hooks/useAsyncData'
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
  const [requestKey, setRequestKey] = useState(0)
  const key = useMemo(
    () => ({ period, requestKey, loadActivity }),
    [period, requestKey, loadActivity],
  )
  const load = useCallback(async () => {
    if (period === 'WEEK') return null
    try {
      return await loadActivity(period)
    } catch (reason: unknown) {
      throw new Error(getErrorMessage(reason, 'ไม่สามารถโหลดกราฟได้'), { cause: reason })
    }
  }, [period, loadActivity])
  const state = useAsyncData<DashboardActivity | null>(load, null, key)

  return {
    activity: state.data,
    loading: period !== 'WEEK' && state.loading,
    error: period === 'WEEK' ? '' : state.error,
    retry: () => setRequestKey((value) => value + 1),
  }
}
