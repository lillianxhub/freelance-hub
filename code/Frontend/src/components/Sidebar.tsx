import { NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/authContextValue'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { initials } from '../utils/formatters'

const workspaceLinks = [
  { to: '/dashboard', label: 'Overview', icon: '⌂' },
  { to: '/projects', label: 'Projects', icon: '▦' },
  { to: '/time-tracker', label: 'Time tracker', icon: '◷' },
  { to: '/clients', label: 'Clients', icon: '♧' },
]

const manageLinks = [
  { to: '/finances', label: 'Finances', icon: '฿' },
  { to: '/invoices', label: 'Invoices', icon: '▤' },
  { to: '/reports', label: 'Reports', icon: '◒' },
  { to: '/settings', label: 'Settings', icon: '⚙' },
]

interface SidebarLinkProps {
  to: string
  label: string
  icon: string
  badge?: number
}

interface SidebarProps {
  open: boolean
  onClose: () => void
}

function SidebarLink({ to, label, icon, badge }: SidebarLinkProps) {
  return (
    <NavLink to={to} className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
      <span className="nav-icon" aria-hidden="true">{icon}</span>
      <span>{label}</span>
      {badge !== undefined && <span className="nav-count">{badge}</span>}
    </NavLink>
  )
}

function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth()
  const { data } = useWorkspace()
  const profile = data?.profiles?.[0]
  const projectCount = data?.projects?.filter((project) => project.status === 'ACTIVE').length || 0
  const name = profile?.full_name || user?.user_metadata?.full_name || 'Freelancer'

  return (
    <>
      <button className={`sidebar-backdrop${open ? ' visible' : ''}`} type="button" aria-label="ปิดเมนู" onClick={onClose} />
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="sidebar-header">
          <div className="logo-box">FH</div>
          <div>
            <div className="sidebar-title">freelance hub</div>
            <div className="sidebar-subtitle">workspace</div>
          </div>
        </div>

        <div className="workspace-switcher">
          <span className="avatar avatar-small">{initials(name)}</span>
          <span><strong>Personal workspace</strong><small>Freelancer</small></span>
          <span className="workspace-chevron">⌄</span>
        </div>

        <div className="sidebar-section-label">Workspace</div>
        <nav className="sidebar-nav" onClick={onClose}>
          {workspaceLinks.map((link) => (
            <SidebarLink key={link.to} {...link} badge={link.to === '/projects' ? projectCount : undefined} />
          ))}
        </nav>

        <div className="sidebar-section-label">Manage</div>
        <nav className="sidebar-nav" onClick={onClose}>
          {manageLinks.map((link) => <SidebarLink key={link.to} {...link} />)}
        </nav>

        <div className="sidebar-bottom">
          <NavLink className="help-card" to="/reports" onClick={onClose}>
            <span className="help-icon">?</span>
            <span><strong>Need a hand?</strong><small>ดูรายงานและคู่มือ</small></span>
            <span>↗</span>
          </NavLink>
          <div className="profile-row">
            <span className="avatar">{initials(name)}</span>
            <span className="profile-copy"><strong>{name}</strong><small>{profile?.email || user?.email}</small></span>
          </div>
        </div>
      </aside>
    </>
  )
}

export default Sidebar
