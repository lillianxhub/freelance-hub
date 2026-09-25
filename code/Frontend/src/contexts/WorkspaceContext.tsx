import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { createEmptyWorkspace, deleteResource, loadAllResources, resetDemoWorkspace, saveResource } from '../services/workspaceRepository'
import type { ResourceInput, ResourceName, WorkspaceData } from '../types/domain'
import { useAuth } from './authContextValue'
import { WorkspaceContext, type WorkspaceContextValue } from './workspaceContextValue'

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'ไม่สามารถโหลดข้อมูลได้'
}

export function WorkspaceProvider({ children }: PropsWithChildren) {
  const { user, isDemoMode } = useAuth()
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
    isDemoMode,
    refresh,
    async save<K extends ResourceName>(resource: K, record: ResourceInput<K>) {
      const saved = await saveResource(resource, record)
      await refresh()
      return saved
    },
    async remove(resource: ResourceName, id: string) {
      await deleteResource(resource, id)
      await refresh()
    },
    async resetDemo() {
      resetDemoWorkspace()
      await refresh()
    },
  }), [data, error, isDemoMode, loading, refresh])

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}
