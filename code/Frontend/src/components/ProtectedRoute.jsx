import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/authContextValue'
import { LoadingState } from './ViewState'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="auth-loading"><LoadingState label="กำลังตรวจสอบการเข้าสู่ระบบ..." /></div>
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default ProtectedRoute
