import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import ProtectedRoute from './components/ProtectedRoute'
import { WorkspaceProvider } from './contexts/WorkspaceContext'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import PlaceholderPage from './pages/PlaceholderPage'
import ClientsPage from './pages/ClientsPage'
import ClientDetailPage from './pages/ClientDetailPage'
import ProjectsPage from './pages/ProjectsPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import TimeTrackerPage from './pages/TimeTrackerPage'
import FinancesPage from './pages/FinancesPage'
import InvoicesPage from './pages/InvoicesPage'
import NewInvoicePage from './pages/NewInvoicePage'
import InvoiceDetailPage from './pages/InvoiceDetailPage'
import './App.css'

function ProtectedWorkspace() {
  return (
    <ProtectedRoute>
      <WorkspaceProvider>
        <AppLayout />
      </WorkspaceProvider>
    </ProtectedRoute>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route element={<ProtectedWorkspace />}>
          <Route path="/dashboard" element={<PlaceholderPage title="Overview" description="ภาพรวมธุรกิจฟรีแลนซ์ของคุณ" />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/clients/:clientId" element={<ClientDetailPage />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
          <Route path="/time-tracker" element={<TimeTrackerPage />} />
          <Route path="/finances" element={<FinancesPage />} />
          <Route path="/invoices" element={<InvoicesPage />} />
          <Route path="/invoices/new" element={<NewInvoicePage />} />
          <Route path="/invoices/:invoiceId" element={<InvoiceDetailPage />} />
          <Route path="/reports" element={<PlaceholderPage title="Reports" description="วิเคราะห์เวลา รายได้ และ productivity" />} />
          <Route path="/settings" element={<PlaceholderPage title="Settings" description="ตั้งค่าโปรไฟล์ Invoice และรูปแบบการแสดงผล" />} />
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
