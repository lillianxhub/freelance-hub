import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { createEmptyWorkspace, deleteResource, loadAllResources, saveResource } from '../services/workspace'
import type { ResourceInput, ResourceName, WorkspaceData } from '../types/workspace'
import type { WorkspaceContextValue } from '../types/workspaceContext'
import { useAuth } from '../Authentication/useAuthentication'
import { WorkspaceContext } from './useWorkspace'

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'ไม่สามารถโหลดข้อมูลได้'
}

export function WorkspaceProvider({ children }: PropsWithChildren) {
  const { user } = useAuth()
  const [data, setData] = useState<WorkspaceData>(createEmptyWorkspace)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    if (!user) return
    setLoading(true)
    setError('')
    try {
      setData(await loadAllResources())
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (!user) return undefined
    let active = true
    loadAllResources()
      .then((workspace) => {
        if (active) setData(workspace)
      })
      .catch((err: unknown) => {
        if (active) setError(errorMessage(err))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [user])

  const value = useMemo<WorkspaceContextValue>(() => ({
    data,
    loading,
    error,
    refresh,
    async save<K extends ResourceName>(resource: K, record: ResourceInput<K>) {
      const saved = await saveResource(resource, record)
      await refresh()
      return saved
    },
    async remove(resource: ResourceName, id: string) {
      const projectId = resource === 'tasks' ? data.tasks.find((task) => task.id === id)?.project_id : undefined
      await deleteResource(resource, id, projectId)
      await refresh()
    },
  }), [data, error, loading, refresh])

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}
