import { lazy } from "react";
import { Navigate, useRoutes, type RouteObject } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import ProtectedRoute from "../Authentication/components/ProtectedRoute";
import { AnalyticsProvider } from "../Analytics/AnalyticsContext";
import { ClientsProvider } from "../ClientManagement/ClientsContext";
import { ProfileProvider } from "../Profile/ProfileContext";
import { ProjectsProvider } from "../ProjectManagement/ProjectsContext";
import { TimeEntriesProvider } from "../TimeTracking/TimeEntriesContext";
import type { AppRouteDefinition } from "../types/routes";
import ProfilePage from "../Profile/pages/Profile/page";

const LoginPage = lazy(() => import("../Authentication/pages/Login/page"));
const RegisterPage = lazy(
    () => import("../Authentication/pages/Register/page"),
);
const DashboardPage = lazy(() => import("../Analytics/pages/Dashboard/page"));
const ClientsPage = lazy(
    () => import("../ClientManagement/pages/Clients/page"),
);
const ClientDetailPage = lazy(
    () => import("../ClientManagement/pages/ClientDetail/page"),
);
const ProjectsPage = lazy(
    () => import("../ProjectManagement/pages/Projects/page"),
);
const ProjectDetailPage = lazy(
    () => import("../ProjectManagement/pages/ProjectDetail/page"),
);
const TimeTrackerPage = lazy(
    () => import("../TimeTracking/pages/TimeTracker/page"),
);
const ReportsPage = lazy(() => import("../Analytics/pages/Reports/page"));

const publicRoutes: AppRouteDefinition[] = [
    { path: "/login", label: "Login", element: <LoginPage /> },
    { path: "/register", label: "Register", element: <RegisterPage /> },
];

const protectedRoutes: AppRouteDefinition[] = [
    {
        path: "/dashboard",
        label: "Dashboard",
        element: (
            <AnalyticsProvider>
                <DashboardPage />
            </AnalyticsProvider>
        ),
    },
    {
        path: "/clients",
        label: "ลูกค้า",
        element: (
            <ClientsProvider>
                <ClientsPage />
            </ClientsProvider>
        ),
    },
    {
        path: "/clients/:clientId",
        label: "Clients detail",
        element: (
            <ClientsProvider>
                <ClientDetailPage />
            </ClientsProvider>
        ),
    },
    {
        path: "/projects",
        label: "โปรเจกต์",
        element: (
            <ProjectsProvider>
                <ProjectsPage />
            </ProjectsProvider>
        ),
    },
    {
        path: "/projects/:projectId",
        label: "Projects detail",
        element: (
            <ProjectsProvider>
                <ProjectDetailPage />
            </ProjectsProvider>
        ),
    },
    {
        path: "/time-tracker",
        label: "บันทึกเวลา",
        element: <TimeTrackerPage />,
    },
    {
        path: "/reports",
        label: "รายงาน",
        element: (
            <AnalyticsProvider>
                <ReportsPage />
            </AnalyticsProvider>
        ),
    },
    {
        path: "/profile",
        label: "โปรไฟล์",
        element: (
            <ProfileProvider>
                <ProfilePage />
            </ProfileProvider>
        ),
    },
    // {
    //     path: "/setting",
    //     label: "โปรไฟล์",
    //     element: <Navigate to="/profile" replace />,
    // },
    // {
    //     path: "/settings",
    //     label: "โปรไฟล์",
    //     element: <Navigate to="/profile" replace />,
    // },
];

function ProtectedWorkspace() {
    return (
        <ProtectedRoute>
            <TimeEntriesProvider>
                <AppLayout />
            </TimeEntriesProvider>
        </ProtectedRoute>
    );
}

const routeObjects: RouteObject[] = [
    ...publicRoutes.map(({ path, element }) => ({ path, element })),
    {
        element: <ProtectedWorkspace />,
        children: protectedRoutes.map(({ path, element }) => ({
            path,
            element,
        })),
    },
    { path: "/", element: <Navigate to="/dashboard" replace /> },
    { path: "*", element: <Navigate to="/dashboard" replace /> },
];

function AppRoutes() {
    return useRoutes(routeObjects);
}

export default AppRoutes;
