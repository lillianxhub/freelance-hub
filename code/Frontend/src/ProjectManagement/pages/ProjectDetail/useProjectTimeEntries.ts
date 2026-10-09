import { useEffect, useState } from 'react'
import { getErrorMessage } from '../../../api/apiError'
import { listTimeEntriesPage, summarizeTimeEntries } from '../../../services/timeTracking'
import type { TimeEntry } from '../../../types/timeTracking'

const PAGE_SIZE = 5

export function useProjectTimeEntries(projectId: string) {
  const [page, setPage] = useState(1)
  const [entries, setEntries] = useState<TimeEntry[]>([])
  const [total, setTotal] = useState(0)
  const [trackedSeconds, setTrackedSeconds] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!projectId) return undefined
    let active = true
    Promise.all([
      listTimeEntriesPage({ projectId, page, limit: PAGE_SIZE }),
      summarizeTimeEntries({ projectId }),
    ])
      .then(([result, summary]) => {
        if (!active) return
        setEntries(result.entries)
        setTotal(result.meta.total)
        setTrackedSeconds(summary.totalSeconds)
      })
      .catch((reason: unknown) => {
        if (!active) return
        setEntries([])
        setTotal(0)
        setTrackedSeconds(null)
        setError(getErrorMessage(reason, 'โหลดรายการเวลาไม่สำเร็จ'))
      })
    return () => { active = false }
  }, [page, projectId])

  return {
    entries,
    page,
    setPage,
    total,
    totalPages: Math.max(1, Math.ceil(total / PAGE_SIZE)),
    trackedSeconds,
    error,
  }
}
