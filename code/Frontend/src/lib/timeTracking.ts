import type { TimeEntry } from '../types/timeTracking'

export function calculateTimeValue(
  entry: Pick<TimeEntry, 'billable' | 'duration_minutes' | 'rate_snapshot'>,
): number {
  if (!entry.billable) return 0
  return ((Number(entry.duration_minutes) || 0) / 60) * (Number(entry.rate_snapshot) || 0)
}
