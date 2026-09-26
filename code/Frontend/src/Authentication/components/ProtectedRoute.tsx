import type { PropsWithChildren } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../useAuthentication'
import { LoadingState } from '../../components/ViewState'

function ProtectedRoute({ children }: PropsWithChildren) {
  const { user, loading } = useAuth()
  if (loading) return <div className="auth-loading"><LoadingState label="กำลังตรวจสอบการเข้าสู่ระบบ..." /></div>
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default ProtectedRoute
