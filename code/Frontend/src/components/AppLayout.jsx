import { Suspense, useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'

function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="app-shell">
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="app-main">
        <Topbar onMenu={() => setMenuOpen(true)} />
        <main className="page-body"><Suspense fallback={<div className="view-state"><div className="loading-spinner" /><p>กำลังโหลดข้อมูลหน้า...</p></div>}><Outlet /></Suspense></main>
      </div>
    </div>
  )
}

export default AppLayout
