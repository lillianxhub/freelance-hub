import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { deleteResource, saveResource } from '../services/workspace'
import { deleteTimeEntry, saveTimeEntry } from '../services/timeTracking'
import type { ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from '../types/workspace'
import type { WorkspaceContextValue } from '../types/workspaceContext'

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'ไม่สามารถโหลดข้อมูลได้'
}

export function useScopedWorkspace(load: () => Promise<WorkspaceData>): WorkspaceContextValue {
  const [data, setData] = useState<WorkspaceData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const initialLoadStarted = useRef(false)
  const hasLoadedData = useRef(false)

  const refresh = useCallback(async () => {
    if (!hasLoadedData.current) setLoading(true)
    setError('')
    try {
      setData(await load())
      hasLoadedData.current = true
    } catch (reason) {
      setError(errorMessage(reason))
    } finally {
      setLoading(false)
    }
  }, [load])

  useEffect(() => {
    if (initialLoadStarted.current) return
    initialLoadStarted.current = true
    void refresh()
  }, [refresh])

  return useMemo<WorkspaceContextValue>(() => ({
    data: data ?? { profiles: [], clients: [], projects: [], tasks: [], time_entries: [] },
    loading,
    error,
    refresh,
    async save<K extends ResourceName>(resource: K, record: ResourceInput<K>): Promise<ResourceRecord<K>> {
      const saved = resource === 'time_entries'
        ? await saveTimeEntry(record as ResourceInput<'time_entries'>)
        : await saveResource(resource, record)
      await refresh()
      return saved
    },
    async remove(resource: ResourceName, id: string): Promise<void> {
      const projectId = resource === 'tasks' ? data?.tasks.find((task) => task.id === id)?.project_id : undefined
      if (resource === 'time_entries') await deleteTimeEntry(id)
      else await deleteResource(resource, id, projectId)
      await refresh()
    },
  }), [data, error, loading, refresh])
}
