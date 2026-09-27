import { api } from '../api/apiClient'
import type { ApiPage, ApiTimeEntry } from '../types/api'
import type { ResourceInput } from '../types/workspace'
import type { ManualTimeEntryPayload, StartTimerPayload, TimeEntry, UpdateTimeEntryPayload } from '../types/timeTracking'
import { deleteResource, saveResource, toTimeEntry } from './workspace'

export async function listTimeEntries(projectId?: string): Promise<TimeEntry[]> {
  const query = new URLSearchParams({ size: '100', sortBy: 'startedAt', direction: 'DESC' })
  if (projectId) query.set('projectId', projectId)
  const response = await api.get<ApiPage<ApiTimeEntry>>(`/time-entries?${query.toString()}`)
  return response.content.map(toTimeEntry)
}

export function saveTimeEntry(entry: ResourceInput<'time_entries'>) {
  return saveResource('time_entries', entry)
}

export function createManualTimeEntry(payload: ManualTimeEntryPayload) {
  return api.post<ApiTimeEntry>('/time-entries', payload)
}

export function updateTimeEntry(id: string, payload: UpdateTimeEntryPayload) {
  return api.patch<ApiTimeEntry>(`/time-entries/${encodeURIComponent(id)}`, payload)
}

export function deleteTimeEntry(id: string) {
  return deleteResource('time_entries', id)
}

export async function startTimer(payload: StartTimerPayload): Promise<TimeEntry> {
  return toTimeEntry(await api.post<ApiTimeEntry>('/timer/start', payload))
}

export async function stopTimer(): Promise<TimeEntry> {
  return toTimeEntry(await api.post<ApiTimeEntry>('/timer/stop'))
}

export function cancelTimer(): Promise<void> {
  return api.delete<void>('/timer/current')
}
