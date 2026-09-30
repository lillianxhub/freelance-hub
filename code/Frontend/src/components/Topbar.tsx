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
    <header className="sticky top-0 z-[25] flex h-[66px] items-center justify-between border-b border-border bg-white/90 px-[34px] backdrop-blur-[14px] max-[820px]:px-[18px] print:hidden">
      <Button variant="ghost"
        className="hidden size-[34px] place-items-center rounded-[9px] border border-border bg-white p-0 text-[#667087] max-[820px]:grid"
        type="button"
        aria-label="เปิดเมนู"
        onClick={onMenu}
      >
        <FiMenu aria-hidden="true" />
      </Button>
      <div className="flex items-center gap-2 text-sm text-text-secondary max-[820px]:hidden">
        <span>พื้นที่ทำงาน</span>
        <span>/</span>
        <strong className="text-text-primary">{label}</strong>
      </div>
      <div className="flex items-center gap-[9px] max-[820px]:ml-auto">
        <TopbarTimer />
        {/* <span className="mode-badge connected">
          <i />Backend API
        </span> */}
        {/* <button className="icon-button" type="button" aria-label="ค้นหา">⌕</button> */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost"
              className="flex h-auto gap-2 rounded-[10px] border-0 bg-transparent px-1 py-[3px] text-left text-text-primary hover:bg-surface-soft"
              type="button"
            >
              <Avatar title={user?.email}><AvatarFallback className="bg-primary text-primary-foreground">{initials(displayName)}</AvatarFallback></Avatar>
            <span className="flex min-w-28 flex-col max-[820px]:hidden">
                <strong className="block truncate text-sm">{displayName}</strong>
                <small className="mt-0.5 block truncate text-xs text-text-secondary">{user?.email}</small>
              </span>
              <span className="text-text-secondary" aria-hidden="true"><FiChevronDown /></span>
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
