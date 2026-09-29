import { api } from '../api/apiClient'
import type { ApiTimeEntry } from '../types/api'
import type { ResourceInput } from '../types/workspace'
import type { ManualTimeEntryPayload, StartTimerPayload, TimeEntry, UpdateTimeEntryPayload } from '../types/timeTracking'
import { toTimeEntry } from './workspace'

export async function listTimeEntries(projectId?: string): Promise<TimeEntry[]> {
  const query = new URLSearchParams({ size: '100', sortBy: 'startedAt', direction: 'DESC' })
  if (projectId) query.set('projectId', projectId)
  const response = await api.get<ApiTimeEntry[]>(`/time-entries?${query.toString()}`)
  return response.data.map(toTimeEntry)
}

export async function saveTimeEntry(entry: ResourceInput<'time_entries'>): Promise<TimeEntry> {
  if (!entry.project_id) throw new Error('รายการเวลาต้องระบุโปรเจกต์')
  if (entry.id) {
    return updateTimeEntry(entry.id, {
      projectId: entry.project_id,
      ...(entry.task_id ? { taskId: entry.task_id } : { clearTask: true }),
      description: entry.description || undefined,
      startedAt: entry.started_at,
      endedAt: entry.ended_at || undefined,
    })
  }
  return createManualTimeEntry({
    projectId: entry.project_id,
    taskId: entry.task_id || null,
    description: entry.description || '',
    startedAt: entry.started_at,
    endedAt: entry.ended_at || undefined,
    durationMinutes: entry.duration_minutes || undefined,
  })
}

export async function createManualTimeEntry(payload: ManualTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/time-entries', payload)
  return toTimeEntry(response.data)
}

export async function updateTimeEntry(id: string, payload: UpdateTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.patch<ApiTimeEntry>(`/time-entries/${encodeURIComponent(id)}`, payload)
  return toTimeEntry(response.data)
}

export async function deleteTimeEntry(id: string): Promise<void> {
  await api.delete(`/time-entries/${encodeURIComponent(id)}`)
}

export async function startTimer(payload: StartTimerPayload): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/timer/start', payload)
  return toTimeEntry(response.data)
}

export async function stopTimer(): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/timer/stop')
  return toTimeEntry(response.data)
}

export async function cancelTimer(): Promise<void> {
  await api.delete<void>('/timer/current')
}
