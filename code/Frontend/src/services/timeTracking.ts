import type { ResourceInput } from '../types/workspace'
import { deleteResource, saveResource } from './workspace'

export async function listTimeEntries(projectId?: string) {
  throw new Error(`Time Tracking${projectId ? ` สำหรับ project ${projectId}` : ''} ยังไม่มี endpoint ใน Swagger ของ backend`)
}

export function saveTimeEntry(entry: ResourceInput<'time_entries'>) {
  return saveResource('time_entries', entry)
}

export function deleteTimeEntry(id: string) {
  return deleteResource('time_entries', id)
}
