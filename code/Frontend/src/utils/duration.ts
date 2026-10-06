export interface TimeParts {
  hours: number
  minutes: number
  seconds: number
}

export function splitDurationSeconds(totalSeconds: number | null | undefined = 0): TimeParts {
  const safeSeconds = Math.max(0, Math.floor(Number(totalSeconds) || 0))
  return {
    hours: Math.floor(safeSeconds / 3600),
    minutes: Math.floor((safeSeconds % 3600) / 60),
    seconds: safeSeconds % 60,
  }
}

export function formatDurationSeconds(totalSeconds: number | null | undefined = 0): string {
  const { hours, minutes, seconds } = splitDurationSeconds(totalSeconds)
  const formatted = [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, '0'))
    .join(':')
  return `${formatted} นาที`
}

export function formatDuration(minutes: number | null | undefined = 0): string {
  const safeMinutes = Math.max(0, Math.round(Number(minutes) || 0))
  return formatDurationSeconds(safeMinutes * 60)
}

export function formatTimer(totalSeconds = 0): string {
  const { hours, minutes, seconds } = splitDurationSeconds(totalSeconds)
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}
