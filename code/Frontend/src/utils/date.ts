export function formatDate(
  date: string | Date | null | undefined,
  options: Intl.DateTimeFormatOptions = {},
): string {
  if (!date) return '—'
  return new Intl.DateTimeFormat('th-TH', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    ...options,
  }).format(new Date(date))
}

export function inDateRange(value: string, from: string, to: string): boolean {
  const date = String(value || '').slice(0, 10)
  return (!from || date >= from) && (!to || date <= to)
}
