import { NavLink } from 'react-router-dom'
import { manageLinks, workspaceLinks } from '../constants/navigation'
import type { SidebarLinkProps, SidebarProps } from '../types/ui'
import { Sheet, SheetContent, SheetTitle } from './ui/sheet'

function SidebarLink({ to, label, icon, badge }: SidebarLinkProps) {
  const Icon = icon
  return (
    <NavLink to={to} className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
      <span className="nav-icon" aria-hidden="true"><Icon /></span>
      <span>{label}</span>
      {badge !== undefined && <span className="nav-count">{badge}</span>}
    </NavLink>
  )
}

function SidebarContent({ onClose }: Pick<SidebarProps, 'onClose'>) {
  return (
    <>
        <div className="sidebar-header">
          <div className="logo-box">FH</div>
          <div>
            <div className="sidebar-title">freelance hub</div>
            <div className="sidebar-subtitle">พื้นที่ทำงาน</div>
          </div>
        </div>

        {/* <div className="workspace-switcher">
          <span className="avatar avatar-small">{initials(name)}</span>
          <span><strong>พื้นที่ทำงานส่วนตัว</strong><small>ฟรีแลนซ์</small></span>
          <span className="workspace-chevron">⌄</span>
        </div> */}

        <div className="sidebar-section-label">งานของคุณ</div>
        <nav className="sidebar-nav" onClick={onClose}>
          {workspaceLinks.map((link) => (
            <SidebarLink key={link.to} {...link} />
          ))}
        </nav>

        <div className="sidebar-section-label">จัดการ</div>
        <nav className="sidebar-nav" onClick={onClose}>
          {manageLinks.map((link) => <SidebarLink key={link.to} {...link} />)}
        </nav>

        {/* <div className="sidebar-bottom">
          <NavLink className="help-card" to="/reports" onClick={onClose}>
            <span className="help-icon">?</span>
            <span><strong>ต้องการความช่วยเหลือ?</strong><small>ดูรายงานและคู่มือ</small></span>
            <span>↗</span>
          </NavLink>
          <div className="profile-row">
            <span className="avatar">{initials(name)}</span>
            <span className="profile-copy"><strong>{name}</strong><small>{profile?.email || user?.email}</small></span>
          </div>
        </div> */}
    </>
  )
}

function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <aside className="sidebar"><SidebarContent onClose={onClose} /></aside>
      <Sheet open={open} onOpenChange={(nextOpen) => { if (!nextOpen) onClose() }}>
        <SheetContent side="left" className="w-[258px] gap-0 overflow-y-auto px-[18px] py-6 sm:max-w-[258px] min-[821px]:hidden">
          <SheetTitle className="sr-only">เมนูหลัก</SheetTitle>
          <SidebarContent onClose={onClose} />
        </SheetContent>
      </Sheet>
    </>
  )
}

export default Sidebar
