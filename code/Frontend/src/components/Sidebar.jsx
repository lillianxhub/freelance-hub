import { NavLink, useNavigate } from 'react-router-dom'

const workspaceLinks = [
  { to: '/dashboard', label: 'Overview', icon: '⌂' },
  { to: '/projects', label: 'Projects', icon: '▦' },
  { to: '/time-tracker', label: 'Time tracker', icon: '◷' },
  { to: '/clients', label: 'Clients', icon: '♙' },
]

const manageLinks = [
  { to: '/reports', label: 'Reports', icon: '↗' },
  { to: '/settings', label: 'Settings', icon: '⚙' },
]

function SidebarLink({ to, label, icon }) {
  return (
    <NavLink to={to} className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
      <span className="nav-icon" aria-hidden="true">{icon}</span>
      <span>{label}</span>
    </NavLink>
  )
}

function Sidebar() {
  const navigate = useNavigate()

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('currentUser')
    navigate('/login', { replace: true })
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="logo-box">FH</div>
        <div>
          <div className="sidebar-title">freelance hub</div>
          <div className="sidebar-subtitle">Workspace</div>
        </div>
      </div>

      <div className="sidebar-section-label">Workspace</div>
      <nav className="sidebar-nav">
        {workspaceLinks.map((link) => <SidebarLink key={link.to} {...link} />)}
      </nav>

      <div className="sidebar-section-label">Manage</div>
      <nav className="sidebar-nav">
        {manageLinks.map((link) => <SidebarLink key={link.to} {...link} />)}
      </nav>

      <button className="logout-button" type="button" onClick={handleLogout}>ออกจากระบบ</button>
    </aside>
  )
}

export default Sidebar
