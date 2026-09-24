import { calculateTimeValue } from './formatters.js'

export function inDateRange(value, from, to) {
  const date = String(value || '').slice(0, 10)
  return (!from || date >= from) && (!to || date <= to)
}

export function summarizeTime(entries = []) {
  const trackedMinutes = entries.reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
  const billableMinutes = entries.filter((entry) => entry.billable).reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0)
  const unbilledValue = entries.filter((entry) => entry.billable && !entry.invoice_id).reduce((sum, entry) => sum + calculateTimeValue(entry), 0)
  return { trackedMinutes, billableMinutes, utilization: trackedMinutes ? (billableMinutes / trackedMinutes) * 100 : 0, unbilledValue }
}

export function groupTimeBy(entries, keyForEntry) {
  const groups = new Map()
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

export function toCsv(rows) {
  return rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\n')
}

export function downloadCsv(filename, rows) {
  const blob = new Blob([`\ufeff${toCsv(rows)}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
