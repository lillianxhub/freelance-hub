import type { TimeSummaryEntry } from '../types/analytics'
import { calculateTimeValue } from './timeTracking'

export function summarizeTime(entries: readonly TimeSummaryEntry[] = []) {
  const trackedMinutes = entries.reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
  const billableMinutes = entries
    .filter((entry) => entry.billable)
    .reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
  return {
    trackedMinutes,
    billableMinutes,
    utilization: trackedMinutes ? (billableMinutes / trackedMinutes) * 100 : 0,
  }
}

export function groupTimeBy<T extends TimeSummaryEntry>(
  entries: readonly T[],
  keyForEntry: (entry: T) => string,
) {
  const groups = new Map<string, { key: string; minutes: number; billableMinutes: number; value: number }>()
  entries.forEach((entry) => {
    const key = keyForEntry(entry)
    const current = groups.get(key) || { key, minutes: 0, billableMinutes: 0, value: 0 }
    current.minutes += Number(entry.duration_minutes || 0)
    if (entry.billable) current.billableMinutes += Number(entry.duration_minutes || 0)
    current.value += calculateTimeValue(entry)
    groups.set(key, current)
  })
  return [...groups.values()].sort((a, b) => b.minutes - a.minutes)
}
