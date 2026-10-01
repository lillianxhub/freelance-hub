import { useCallback, useEffect, useRef, useState } from 'react'

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'ไม่สามารถโหลดข้อมูลได้'
}

export function useAsyncData<T>(load: () => Promise<T>, initialData: T, key?: unknown) {
  const [storedData, setStoredData] = useState<{ key: unknown; value: T }>({ key, value: initialData })
  const [status, setStatus] = useState<{ key: unknown; loading: boolean; error: string }>({ key, loading: true, error: '' })
  const requestId = useRef(0)
  const loadedKeys = useRef(new Set<unknown>())

  const refresh = useCallback(async () => {
    const currentRequest = ++requestId.current
    setStatus({ key, loading: !loadedKeys.current.has(key), error: '' })
    try {
      const result = await load()
      if (currentRequest === requestId.current) {
        setStoredData({ key, value: result })
        loadedKeys.current.add(key)
        setStatus({ key, loading: false, error: '' })
      }
    } catch (reason) {
      if (currentRequest === requestId.current) setStatus({ key, loading: false, error: errorMessage(reason) })
    }
  }, [key, load])

  useEffect(() => {
    void refresh()
    return () => { requestId.current++ }
  }, [refresh])

  return {
    data: Object.is(storedData.key, key) ? storedData.value : initialData,
    loading: !Object.is(status.key, key) || status.loading,
    error: Object.is(status.key, key) ? status.error : '',
    refresh,
  }
}
