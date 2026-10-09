import { useEffect, useState } from 'react'
import { getErrorMessage } from '../api/apiError'
import { loadThaiAddressData, type ThaiProvince } from '../services/thaiAddress'

export function useThaiAddress() {
  const [provinces, setProvinces] = useState<ThaiProvince[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    loadThaiAddressData()
      .then((items) => {
        if (active) setProvinces(items)
      })
      .catch((reason: unknown) => {
        if (active) setError(getErrorMessage(reason, 'ไม่สามารถโหลดข้อมูลจังหวัดได้'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return { provinces, loading, error }
}
