import type { ManualTimeForm } from '../types/timeTrackerPage'

export function localDateValue(date: Date = new Date()): string {
  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return adjusted.toISOString().slice(0, 10)
}

export const createEmptyManualForm = (projectId = ''): ManualTimeForm => ({
  project_id: projectId, task_id: '', description: '', entry_date: localDateValue(), start_time: '09:00', end_time: '10:00',
  manual_mode: 'RANGE', duration_minutes: 60, billable: true, rate_snapshot: '', currency: 'THB',
})
