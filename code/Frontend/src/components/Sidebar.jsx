function Sidebar() {
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
        <a className="nav-item active">Overview</a>
        <a className="nav-item">Projects</a>
        <a className="nav-item">Time tracker</a>
        <a className="nav-item">Clients</a>
      </nav>

      <div className="sidebar-section-label">Manage</div>
      <nav className="sidebar-nav">
        <a className="nav-item">Reports</a>
        <a className="nav-item">Settings</a>
      </nav>
    </aside>
  )
}

export default Sidebar