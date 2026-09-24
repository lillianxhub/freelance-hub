import { lazy, type ReactNode } from 'react'
import { Navigate, useRoutes, type RouteObject } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import ProtectedRoute from '../components/ProtectedRoute'
import { WorkspaceProvider } from '../contexts/WorkspaceContext'

const LoginPage = lazy(() => import('../pages/LoginPage'))
const RegisterPage = lazy(() => import('../pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('../pages/ForgotPasswordPage'))
const DashboardPage = lazy(() => import('../pages/DashboardPage'))
const ClientsPage = lazy(() => import('../pages/ClientsPage'))
const ClientDetailPage = lazy(() => import('../pages/ClientDetailPage'))
const ProjectsPage = lazy(() => import('../pages/ProjectsPage'))
const ProjectDetailPage = lazy(() => import('../pages/ProjectDetailPage'))
const TimeTrackerPage = lazy(() => import('../pages/TimeTrackerPage'))
const FinancesPage = lazy(() => import('../pages/FinancesPage'))
const InvoicesPage = lazy(() => import('../pages/InvoicesPage'))
const NewInvoicePage = lazy(() => import('../pages/NewInvoicePage'))
const InvoiceDetailPage = lazy(() => import('../pages/InvoiceDetailPage'))
const ReportsPage = lazy(() => import('../pages/ReportsPage'))
const SettingsPage = lazy(() => import('../pages/SettingsPage'))

type AppRouteDefinition = {
  path: string
  label: string
  element: ReactNode
}

const publicRoutes: AppRouteDefinition[] = [
  { path: '/login', label: 'Login', element: <LoginPage /> },
  { path: '/register', label: 'Register', element: <RegisterPage /> },
  { path: '/forgot-password', label: 'Forgot password', element: <ForgotPasswordPage /> },
]

const protectedRoutes: AppRouteDefinition[] = [
  { path: '/dashboard', label: 'Dashboard', element: <DashboardPage /> },
  { path: '/clients', label: 'Clients', element: <ClientsPage /> },
  { path: '/clients/:clientId', label: 'Client detail', element: <ClientDetailPage /> },
  { path: '/projects', label: 'Projects', element: <ProjectsPage /> },
  { path: '/projects/:projectId', label: 'Project detail', element: <ProjectDetailPage /> },
  { path: '/time-tracker', label: 'Time tracker', element: <TimeTrackerPage /> },
  { path: '/finances', label: 'Finances', element: <FinancesPage /> },
  { path: '/invoices', label: 'Invoices', element: <InvoicesPage /> },
  { path: '/invoices/new', label: 'Create invoice', element: <NewInvoicePage /> },
  { path: '/invoices/:invoiceId/edit', label: 'Edit invoice draft', element: <NewInvoicePage /> },
  { path: '/invoices/:invoiceId', label: 'Invoice detail', element: <InvoiceDetailPage /> },
  { path: '/reports', label: 'Reports', element: <ReportsPage /> },
  { path: '/settings', label: 'Settings', element: <SettingsPage /> },
]

function ProtectedWorkspace() {
  return (
    <ProtectedRoute>
      <WorkspaceProvider>
        <AppLayout />
      </WorkspaceProvider>
    </ProtectedRoute>
  )
}

const routeObjects: RouteObject[] = [
  ...publicRoutes.map(({ path, element }) => ({ path, element })),
  {
    element: <ProtectedWorkspace />,
    children: protectedRoutes.map(({ path, element }) => ({ path, element })),
  },
  { path: '/', element: <Navigate to="/dashboard" replace /> },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]

function AppRoutes() {
  return useRoutes(routeObjects)
}

export default AppRoutes
