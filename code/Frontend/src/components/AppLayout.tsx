import { Suspense, useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import { Spinner } from './ui/spinner'

function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="flex min-h-screen bg-bg text-text-primary">
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="min-w-0 flex-1">
        <Topbar onMenu={() => setMenuOpen(true)} />
        <main className="px-[clamp(22px,3.4vw,54px)] pt-[34px] pb-[60px] max-[820px]:px-[18px] max-[820px]:pt-[25px] max-[820px]:pb-[45px] print:p-0"><Suspense fallback={<div className="flex min-h-[300px] flex-col items-center justify-center gap-2.5 text-center text-sm text-text-secondary"><Spinner className="size-6 text-primary" /><p>กำลังโหลดข้อมูลหน้า...</p></div>}><Outlet /></Suspense></main>
      </div>
    </div>
  )
}

export default AppLayout
