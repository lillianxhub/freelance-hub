import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../Authentication/useAuthentication'
import { initials } from '../utils/formatters'
import type { TopbarProps } from '../types/ui'

const labels: Record<string, string> = {
  dashboard: 'Overview', clients: 'Clients', projects: 'Projects', 'time-tracker': 'Time tracker',
  finances: 'Finances', invoices: 'Invoices', reports: 'Reports', settings: 'Settings',
}

function Topbar({ onMenu }: TopbarProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const segment = location.pathname.split('/').filter(Boolean)[0] || 'dashboard'
  const label = labels[segment] || 'Workspace'

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="topbar">
      <button className="mobile-menu" type="button" aria-label="เปิดเมนู" onClick={onMenu}>☰</button>
      <div className="breadcrumbs"><span>Workspace</span><span>/</span><strong>{label}</strong></div>
      <div className="topbar-actions">
        <span className="mode-badge connected">
          <i />Backend API
        </span>
        <button className="icon-button" type="button" aria-label="ค้นหา">⌕</button>
        <button className="avatar avatar-button" type="button" title={user?.email}>{initials(user?.user_metadata?.full_name || user?.email)}</button>
        <button className="text-button logout-link" type="button" onClick={handleLogout}>ออกจากระบบ</button>
      </div>
    </header>
  )
}

export default Topbar
