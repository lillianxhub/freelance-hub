import { Button } from './ui/button'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../Authentication/useAuthentication'
import { initials } from '../utils/formatters'
import type { TopbarProps } from '../types/ui'
import { FiChevronDown, FiMenu } from 'react-icons/fi'
import TopbarTimer from '../TimeTracking/components/TopbarTimer'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu'
import { Avatar, AvatarFallback } from './ui/avatar'

const labels: Record<string, string> = {
  dashboard: 'ภาพรวม', clients: 'ลูกค้า', projects: 'โปรเจกต์', 'time-tracker': 'บันทึกเวลา',
  finances: 'การเงิน', invoices: 'ใบแจ้งหนี้', reports: 'รายงาน', profile: 'โปรไฟล์',
}

function Topbar({ onMenu }: TopbarProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const segment = location.pathname.split('/').filter(Boolean)[0] || 'dashboard'
  const label = labels[segment] || 'พื้นที่ทำงาน'
  const displayName = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'โปรไฟล์'

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="topbar">
      <Button variant="ghost" className="mobile-menu" type="button" aria-label="เปิดเมนู" onClick={onMenu}><FiMenu aria-hidden="true" /></Button>
      <div className="breadcrumbs"><span>พื้นที่ทำงาน</span><span>/</span><strong>{label}</strong></div>
      <div className="topbar-actions">
        <TopbarTimer />
        {/* <span className="mode-badge connected">
          <i />Backend API
        </span> */}
        {/* <button className="icon-button" type="button" aria-label="ค้นหา">⌕</button> */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
          <Button variant="ghost"
            className="account-trigger"
            type="button"
          >
            <Avatar title={user?.email}><AvatarFallback className="bg-primary text-primary-foreground">{initials(displayName)}</AvatarFallback></Avatar>
            <span className="account-copy">
              <strong>{displayName}</strong>
              <small>{user?.email}</small>
            </span>
            <span className="account-chevron" aria-hidden="true"><FiChevronDown /></span>
          </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-44">
            <DropdownMenuItem onSelect={() => navigate('/profile')}>โปรไฟล์</DropdownMenuItem>
            <DropdownMenuItem onSelect={() => void handleLogout()}>ออกจากระบบ</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}

export default Topbar
