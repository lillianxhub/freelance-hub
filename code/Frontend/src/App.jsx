import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import RegisterPage from './pages/RegisterPage'
import LoginPage from './pages/LoginPage'
import ProtectedRoute from './components/ProtectedRoute'
import PlaceholderPage from './pages/PlaceholderPage'

const protectedPage = (page) => (
  <ProtectedRoute>
    {page}
  </ProtectedRoute>
)

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/dashboard" element={protectedPage(<DashboardPage />)} />
        <Route path="/projects" element={protectedPage(<PlaceholderPage title="Projects" description="จัดการโปรเจกต์ งานย่อย และเป้าหมายชั่วโมง" />)} />
        <Route path="/time-tracker" element={protectedPage(<PlaceholderPage title="Time tracker" description="เริ่มจับเวลาและดูรายการเวลาทำงาน" />)} />
        <Route path="/clients" element={protectedPage(<PlaceholderPage title="Clients" description="จัดการข้อมูลลูกค้าและโปรเจกต์ของแต่ละราย" />)} />
        <Route path="/reports" element={protectedPage(<PlaceholderPage title="Reports" description="ดูรายงานเวลาและ productivity" />)} />
        <Route path="/settings" element={protectedPage(<PlaceholderPage title="Settings" description="ตั้งค่าโปรไฟล์ ภาษา และเขตเวลา" />)} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
