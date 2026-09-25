import type { TimeEntry } from '../types/domain'

export function formatMoney(amount: number | string | null | undefined = 0, currency = 'THB') {
  return new Intl.NumberFormat('th-TH', { style: 'currency', currency, maximumFractionDigits: 2 }).format(Number(amount) || 0)
}

export function formatDuration(minutes: number | null | undefined = 0) {
  const safeMinutes = Math.max(0, Math.round(Number(minutes) || 0))
  const hours = Math.floor(safeMinutes / 60)
  const remainingMinutes = safeMinutes % 60
  return hours ? `${hours}ชม. ${remainingMinutes}น.` : `${remainingMinutes}น.`
}

export function formatTimer(totalSeconds = 0) {
  const safeSeconds = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const seconds = safeSeconds % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}

export function formatDate(date: string | Date | null | undefined, options: Intl.DateTimeFormatOptions = {}) {
  if (!date) return '—'
  return new Intl.DateTimeFormat('th-TH', { day: 'numeric', month: 'short', year: 'numeric', ...options }).format(new Date(date))
}

export function initials(value = '') {
  return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'FH'
}

export function calculateTimeValue(entry: Pick<TimeEntry, 'billable' | 'duration_minutes' | 'rate_snapshot'>) {
  return entry.billable ? ((Number(entry.duration_minutes) || 0) / 60) * (Number(entry.rate_snapshot) || 0) : 0
}
