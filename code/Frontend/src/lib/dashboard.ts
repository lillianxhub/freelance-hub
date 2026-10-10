import type { ProductivityPoint } from '../types/analytics'
import type { DashboardDailyWork } from '../types/dashboard'
import { splitDurationSeconds } from '../utils/duration'

export function formatChartDuration(totalSeconds: number): string {
  const { hours, minutes, seconds } = splitDurationSeconds(totalSeconds)
  const parts = [
    hours ? `${hours} ชม.` : '',
    minutes ? `${minutes} นาที` : '',
    seconds ? `${seconds} วิ` : '',
  ].filter(Boolean)
  return parts.join(' ') || '0 วินาที'
}

export function weeklyChartPoints(days: DashboardDailyWork[]): ProductivityPoint[] {
  return days.map((item) => ({
    key: item.date,
    day: new Intl.DateTimeFormat('th-TH', { weekday: 'short' }).format(
      new Date(`${item.date}T12:00:00+07:00`),
    ),
    totalSeconds: item.trackedSeconds,
  }))
}

export function activityChartPoints(
  period: 'MONTH' | 'YEAR',
  points: DashboardDailyWork[],
): ProductivityPoint[] {
  return points.map((item) => ({
    key: item.date,
    day:
      period === 'YEAR'
        ? new Intl.DateTimeFormat('th-TH', { month: 'short' }).format(
            new Date(`${item.date}T12:00:00+07:00`),
          )
        : String(Number(item.date.slice(8, 10))),
    totalSeconds: item.trackedSeconds,
  }))
}
