import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../Authentication/useAuthentication'
import { initials } from '../utils/formatters'
import type { TopbarProps } from '../types/ui'
import { FiChevronDown, FiMenu } from 'react-icons/fi'
import TopbarTimer from '../TimeTracking/components/TopbarTimer'

const labels: Record<string, string> = {
  dashboard: 'ภาพรวม', clients: 'ลูกค้า', projects: 'โปรเจกต์', 'time-tracker': 'บันทึกเวลา',
  finances: 'การเงิน', invoices: 'ใบแจ้งหนี้', reports: 'รายงาน', profile: 'โปรไฟล์',
}

function Topbar({ onMenu }: TopbarProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const segment = location.pathname.split('/').filter(Boolean)[0] || 'dashboard'
  const label = labels[segment] || 'พื้นที่ทำงาน'
  const displayName = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'โปรไฟล์'

  useEffect(() => {
    const closeOnPointerDown = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuOpen(false)
    }
    document.addEventListener('mousedown', closeOnPointerDown)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnPointerDown)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  const handleLogout = async () => {
    setMenuOpen(false)
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="topbar">
      <button className="mobile-menu" type="button" aria-label="เปิดเมนู" onClick={onMenu}><FiMenu aria-hidden="true" /></button>
      <div className="breadcrumbs"><span>พื้นที่ทำงาน</span><span>/</span><strong>{label}</strong></div>
      <div className="topbar-actions">
        <TopbarTimer />
        {/* <span className="mode-badge connected">
          <i />Backend API
        </span> */}
        {/* <button className="icon-button" type="button" aria-label="ค้นหา">⌕</button> */}
        <div className="account-menu" ref={menuRef}>
          <button
            className="account-trigger"
            type="button"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="avatar" title={user?.email}>
              {initials(displayName)}
            </span>
            <span className="account-copy">
              <strong>{displayName}</strong>
              <small>{user?.email}</small>
            </span>
            <span className="account-chevron" aria-hidden="true"><FiChevronDown /></span>
          </button>
          {menuOpen && (
            <div className="account-dropdown" role="menu">
              <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); navigate('/profile') }}>
                โปรไฟล์
              </button>
              <button type="button" role="menuitem" onClick={() => void handleLogout()}>
                ออกจากระบบ
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

export default Topbar
