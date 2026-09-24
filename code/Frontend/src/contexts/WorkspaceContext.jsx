import { useCallback, useEffect, useMemo, useState } from 'react'
import { deleteResource, loadAllResources, resetDemoWorkspace, saveResource } from '../services/workspaceRepository'
import { useAuth } from './authContextValue'
import { WorkspaceContext } from './workspaceContextValue'

export function WorkspaceProvider({ children }) {
  const { user, isDemoMode } = useAuth()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    if (!user) return
    setLoading(true)
    setError('')
    try {
      setData(await loadAllResources())
    } catch (err) {
      setError(err.message || 'ไม่สามารถโหลดข้อมูลได้')
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
      .catch((err) => {
        if (active) setError(err.message || 'ไม่สามารถโหลดข้อมูลได้')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [user])

  const value = useMemo(() => ({
    data,
    loading,
    error,
    isDemoMode,
    refresh,
    async save(resource, record) {
      const saved = await saveResource(resource, record)
      await refresh()
      return saved
    },
    async remove(resource, id) {
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
