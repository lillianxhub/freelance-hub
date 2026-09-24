import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import ProtectedRoute from './components/ProtectedRoute'
import { WorkspaceProvider } from './contexts/WorkspaceContext'
import './App.css'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const ClientsPage = lazy(() => import('./pages/ClientsPage'))
const ClientDetailPage = lazy(() => import('./pages/ClientDetailPage'))
const ProjectsPage = lazy(() => import('./pages/ProjectsPage'))
const ProjectDetailPage = lazy(() => import('./pages/ProjectDetailPage'))
const TimeTrackerPage = lazy(() => import('./pages/TimeTrackerPage'))
const FinancesPage = lazy(() => import('./pages/FinancesPage'))
const InvoicesPage = lazy(() => import('./pages/InvoicesPage'))
const NewInvoicePage = lazy(() => import('./pages/NewInvoicePage'))
const InvoiceDetailPage = lazy(() => import('./pages/InvoiceDetailPage'))
const ReportsPage = lazy(() => import('./pages/ReportsPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))

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
      <Suspense fallback={<div className="view-state route-loading"><div className="loading-spinner" /><p>กำลังเปิดหน้า...</p></div>}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route element={<ProtectedWorkspace />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/clients" element={<ClientsPage />} />
            <Route path="/clients/:clientId" element={<ClientDetailPage />} />
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
            <Route path="/time-tracker" element={<TimeTrackerPage />} />
            <Route path="/finances" element={<FinancesPage />} />
            <Route path="/invoices" element={<InvoicesPage />} />
            <Route path="/invoices/new" element={<NewInvoicePage />} />
            <Route path="/invoices/:invoiceId/edit" element={<NewInvoicePage />} />
            <Route path="/invoices/:invoiceId" element={<InvoiceDetailPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default App
