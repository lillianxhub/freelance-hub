import { NavLink } from 'react-router-dom'
import { workspaceLinks } from '../constants/navigation'
import type { SidebarLinkProps, SidebarProps } from '../types/ui'
import { Sheet, SheetContent, SheetTitle } from './ui/sheet'

function SidebarLink({ to, label, icon, badge }: SidebarLinkProps) {
  const Icon = icon
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `group/nav grid min-h-10 grid-cols-[18px_minmax(0,1fr)_auto] items-center gap-2.5 rounded-[9px] px-[11px] py-2 text-sm no-underline transition-colors ${isActive ? 'active bg-primary-soft font-semibold text-primary-dark' : 'text-text-secondary hover:bg-surface-soft hover:text-text-primary'}`
      }
    >
      <span
        className="grid size-[18px] place-items-center text-base text-subtle group-[.active]/nav:text-primary"
        aria-hidden="true"
      >
        <Icon />
      </span>
      <span>{label}</span>
      {badge !== undefined && (
        <span className="min-w-[21px] rounded-full bg-[#eef1f7] px-1.5 py-0.5 text-center text-xs text-[#5c6780]">
          {badge}
        </span>
      )}
    </NavLink>
  )
}

function SidebarContent({ onClose }: Pick<SidebarProps, 'onClose'>) {
  return (
    <div className="flex h-full flex-col">
      <div className="mb-[22px] flex items-center gap-[11px] px-[5px]">
        <img src="/logo-light.svg" alt="" className="size-[39px] shrink-0 object-contain" />
        <div className="min-w-0">
          <div className="text-base font-bold tracking-[-0.02em]">freelance hub</div>
          <div className="mt-px text-sm text-text-secondary uppercase">พื้นที่ทำงาน</div>
        </div>
      </div>

      {/* <div className="workspace-switcher">
          <span className="avatar avatar-small">{initials(name)}</span>
          <span><strong>พื้นที่ทำงานส่วนตัว</strong><small>ฟรีแลนซ์</small></span>
          <span className="workspace-chevron">⌄</span>
        </div> */}

      <div className="mt-4 mb-[7px] px-[11px] text-xs font-bold tracking-[0.12em] text-subtle uppercase">
        งานของคุณ
      </div>
      <nav className="flex flex-col gap-[3px]" onClick={onClose}>
        {workspaceLinks.map((link) => (
          <SidebarLink key={link.to} {...link} />
        ))}
      </nav>

      {/* <div className="mt-4 mb-[7px] px-[11px] text-xs font-bold tracking-[0.12em] text-subtle uppercase">จัดการ</div>
        <nav className="flex flex-col gap-[3px]" onClick={onClose}>
          {manageLinks.map((link) => <SidebarLink key={link.to} {...link} />)}
        </nav> */}

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
    </div>
  )
}

function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <aside className="hidden h-screen w-[258px] shrink-0 flex-col border-r border-border bg-sidebar px-[18px] pt-6 pb-[18px] lg:sticky lg:top-0 lg:z-40 lg:flex print:hidden">
        <SidebarContent onClose={onClose} />
      </aside>
      <Sheet
        open={open}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) onClose()
        }}
      >
        <SheetContent
          side="left"
          className="w-[258px] gap-0 overflow-y-auto px-[18px] py-6 sm:max-w-[258px] lg:hidden"
        >
          <SheetTitle className="sr-only">เมนูหลัก</SheetTitle>
          <SidebarContent onClose={onClose} />
        </SheetContent>
      </Sheet>
    </>
  )
}

export default Sidebar
