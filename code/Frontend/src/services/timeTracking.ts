import { api } from '../api/apiClient'
import type { ApiTimeEntry } from '../types/api'
import type { ResourceInput } from '../types/workspace'
import type { ManualTimeEntryPayload, StartTimerPayload, TimeEntry, UpdateTimeEntryPayload } from '../types/timeTracking'
import { deleteResource, saveResource, toTimeEntry } from './workspace'

export async function listTimeEntries(projectId?: string): Promise<TimeEntry[]> {
  const query = new URLSearchParams({ size: '100', sortBy: 'startedAt', direction: 'DESC' })
  if (projectId) query.set('projectId', projectId)
  const response = await api.get<ApiTimeEntry[]>(`/time-entries?${query.toString()}`)
  return response.data.map(toTimeEntry)
}

export function saveTimeEntry(entry: ResourceInput<'time_entries'>) {
  return saveResource('time_entries', entry)
}

export async function createManualTimeEntry(payload: ManualTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/time-entries', payload)
  return toTimeEntry(response.data)
}

export async function updateTimeEntry(id: string, payload: UpdateTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.patch<ApiTimeEntry>(`/time-entries/${encodeURIComponent(id)}`, payload)
  return toTimeEntry(response.data)
}

export function deleteTimeEntry(id: string) {
  return deleteResource('time_entries', id)
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
