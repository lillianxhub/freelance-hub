import { lazy } from 'react'
import { Navigate, useRoutes, type RouteObject } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import ProtectedRoute from '../Authentication/components/ProtectedRoute'
import { WorkspaceProvider } from '../Workspace/WorkspaceContext'
import type { AppRouteDefinition } from '../types/routes'

const LoginPage = lazy(() => import('../Authentication/pages/Login/page'))
const RegisterPage = lazy(() => import('../Authentication/pages/Register/page'))
const ForgotPasswordPage = lazy(() => import('../Authentication/pages/ForgotPassword/page'))
const DashboardPage = lazy(() => import('../Analytics/pages/Dashboard/page'))
const ClientsPage = lazy(() => import('../ClientManagement/pages/Clients/page'))
const ClientDetailPage = lazy(() => import('../ClientManagement/pages/ClientDetail/page'))
const ProjectsPage = lazy(() => import('../ProjectManagement/pages/Projects/page'))
const ProjectDetailPage = lazy(() => import('../ProjectManagement/pages/ProjectDetail/page'))
const TimeTrackerPage = lazy(() => import('../TimeTracking/pages/TimeTracker/page'))
const FinancesPage = lazy(() => import('../Billing/pages/Finances/page'))
const InvoicesPage = lazy(() => import('../Billing/pages/Invoices/page'))
const NewInvoicePage = lazy(() => import('../Billing/pages/NewInvoice/page'))
const InvoiceDetailPage = lazy(() => import('../Billing/pages/InvoiceDetail/page'))
const ReportsPage = lazy(() => import('../Analytics/pages/Reports/page'))
const SettingsPage = lazy(() => import('../Settings/pages/Settings/page'))

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
