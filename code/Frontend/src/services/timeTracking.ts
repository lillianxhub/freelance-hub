import { api } from '../api/apiClient'
import type { ApiCurrentTimer, ApiMeta, ApiTimeEntry } from '../types/api'
import type { ResourceInput } from '../types/workspace'
import type { ManualTimeEntryPayload, StartTimerPayload, TimeEntry, UpdateTimeEntryPayload } from '../types/timeTracking'
import { toTimeEntry } from './workspace'

export interface TimeEntryListQuery {
  clientId?: string
  projectId?: string
  taskId?: string
  from?: string
  to?: string
  page?: number
  limit?: number
}

export interface TimeEntryPage {
  entries: TimeEntry[]
  meta: ApiMeta
}

export interface TimeEntrySummary {
  entryCount: number
  totalSeconds: number
}

export async function listTimeEntriesPage(query: TimeEntryListQuery = {}): Promise<TimeEntryPage> {
  const params = new URLSearchParams({
    page: String(query.page ?? 1),
    limit: String(query.limit ?? 5),
    sortBy: 'startedAt',
    direction: 'DESC',
  })
  if (query.clientId) params.set('clientId', query.clientId)
  if (query.projectId) params.set('projectId', query.projectId)
  if (query.taskId) params.set('taskId', query.taskId)
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)

  const response = await api.get<ApiTimeEntry[]>(`/time-entries?${params.toString()}`)
  const entries = response.data.map(toTimeEntry)
  return {
    entries,
    meta: response.meta ?? {
      page: query.page ?? 1,
      limit: query.limit ?? 5,
      total: entries.length,
      totalPages: entries.length ? 1 : 0,
    },
  }
}

export async function summarizeTimeEntries(query: TimeEntryListQuery = {}): Promise<TimeEntrySummary> {
  const params = new URLSearchParams()
  if (query.clientId) params.set('clientId', query.clientId)
  if (query.projectId) params.set('projectId', query.projectId)
  if (query.taskId) params.set('taskId', query.taskId)
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)

  const response = await api.get<TimeEntrySummary>(`/time-entries/summary?${params.toString()}`)
  return response.data
}

export async function listTimeEntries(projectId?: string): Promise<TimeEntry[]> {
  const query = new URLSearchParams({ page: '1', limit: '10', sortBy: 'startedAt', direction: 'DESC' })
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
    durationSeconds: entry.duration_minutes ? entry.duration_minutes * 60 : undefined,
  })
}

export async function createManualTimeEntry({ durationSeconds, ...payload }: ManualTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/time-entries', {
    ...payload,
    ...(durationSeconds === undefined ? {} : { durationSeconds }),
  })
  return toTimeEntry(response.data)
}

export async function updateTimeEntry(id: string, payload: UpdateTimeEntryPayload): Promise<TimeEntry> {
  const response = await api.put<ApiTimeEntry>(`/time-entries/${encodeURIComponent(id)}`, payload)
  return toTimeEntry(response.data)
}

export async function deleteTimeEntry(id: string): Promise<void> {
  await api.delete(`/time-entries/${encodeURIComponent(id)}`)
}

export async function startTimer(payload: StartTimerPayload): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/timer/start', payload)
  return toTimeEntry(response.data)
}

export async function getCurrentTimer(): Promise<ApiCurrentTimer> {
  const response = await api.get<ApiCurrentTimer>('/timer/current')
  return response.data
}

export async function stopTimer(): Promise<TimeEntry> {
  const response = await api.post<ApiTimeEntry>('/timer/stop')
  return toTimeEntry(response.data)
}

export async function cancelTimer(): Promise<void> {
  await api.delete<void>('/timer/current')
}
