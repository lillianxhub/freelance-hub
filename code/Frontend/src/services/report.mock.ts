import type { ProjectStatus } from '../types/project'
import type { ReportProjectUsage, ReportSummaryData, ReportSummaryQuery } from '../types/analytics'
import { inDateRange } from '../utils/date'

interface MockProject extends Omit<ReportProjectUsage, 'trackedSeconds' | 'usagePercent'> {
  entries: Array<{ date: string; durationSeconds: number }>
  previousPeriodSeconds: number
}

const clients = [
  { id: 'client-northstar', name: 'Northstar Studio' },
  { id: 'client-orbit', name: 'Orbit Labs' },
  { id: 'client-good-goods', name: 'Good Goods Co.' },
  { id: 'client-self', name: 'Self Project' },
]

const projects: MockProject[] = [
  { projectId: 'project-website', projectName: 'Website Redesign', clientId: 'client-northstar', clientName: 'Northstar Studio', color: '#4F6BFF', targetSeconds: 40 * 3600, taskProgressPercent: 75, status: 'ACTIVE', previousPeriodSeconds: 22 * 3600, entries: [{ date: '2026-10-01', durationSeconds: 12 * 3600 }, { date: '2026-10-03', durationSeconds: 16 * 3600 }] },
  { projectId: 'project-mobile', projectName: 'Mobile App', clientId: 'client-orbit', clientName: 'Orbit Labs', color: '#7C3AED', targetSeconds: 40 * 3600, taskProgressPercent: 50, status: 'ACTIVE', previousPeriodSeconds: 20 * 3600, entries: [{ date: '2026-10-02', durationSeconds: 10 * 3600 }, { date: '2026-10-04', durationSeconds: 12 * 3600 }] },
  { projectId: 'project-brand', projectName: 'Brand Identity', clientId: 'client-good-goods', clientName: 'Good Goods Co.', color: '#10B981', targetSeconds: 20 * 3600, taskProgressPercent: 60, status: 'ON_HOLD', previousPeriodSeconds: 10 * 3600, entries: [{ date: '2026-10-01', durationSeconds: 12 * 3600 }] },
  { projectId: 'project-marketing', projectName: 'Marketing Campaign', clientId: 'client-self', clientName: 'Self Project', color: '#F59E0B', targetSeconds: 20 * 3600, taskProgressPercent: 33, status: 'PLANNED', previousPeriodSeconds: 6 * 3600, entries: [{ date: '2026-10-03', durationSeconds: 8 * 3600 }] },
  { projectId: 'project-portfolio', projectName: 'Portfolio Refresh', clientId: 'client-self', clientName: 'Self Project', color: '#64748B', targetSeconds: null, taskProgressPercent: 0, status: 'PLANNED', previousPeriodSeconds: 0, entries: [] },
  { projectId: 'project-store', projectName: 'Online Store', clientId: 'client-northstar', clientName: 'Northstar Studio', color: '#EF4444', targetSeconds: 30 * 3600, taskProgressPercent: 100, status: 'COMPLETED', previousPeriodSeconds: 4 * 3600, entries: [{ date: '2026-09-29', durationSeconds: 6 * 3600 }] },
]

export function createReportSummaryMock(query: ReportSummaryQuery): ReportSummaryData {
  const selectedProjects = projects.filter((project) =>
    (!query.clientId || project.clientId === query.clientId)
    && (!query.projectId || project.projectId === query.projectId),
  )
  const rows = selectedProjects.map((project) => {
    const matchingEntries = project.entries.filter((entry) => inDateRange(entry.date, query.from, query.to))
    const trackedSeconds = matchingEntries.reduce((total, entry) => total + entry.durationSeconds, 0)
    return {
      projectId: project.projectId,
      projectName: project.projectName,
      clientId: project.clientId,
      clientName: project.clientName,
      color: project.color,
      targetSeconds: project.targetSeconds,
      trackedSeconds,
      usagePercent: project.targetSeconds ? trackedSeconds / project.targetSeconds * 100 : null,
      taskProgressPercent: project.taskProgressPercent,
      status: project.status as ProjectStatus,
      timeEntryCount: matchingEntries.length,
      previousPeriodSeconds: project.previousPeriodSeconds,
    }
  })
  const totalTrackedSeconds = rows.reduce((total, row) => total + row.trackedSeconds, 0)
  const previousSeconds = rows.reduce((total, row) => total + row.previousPeriodSeconds, 0)
  const clientTotals = new Map<string, number>()
  rows.forEach((row) => clientTotals.set(row.clientId, (clientTotals.get(row.clientId) || 0) + row.trackedSeconds))

  return {
    generatedAt: new Date().toISOString(),
    filters: {
      clients,
      projects: projects.map((project) => ({ id: project.projectId, name: project.projectName, clientId: project.clientId })),
    },
    summary: {
      totalTrackedSeconds,
      trackedTimeTrendPercent: previousSeconds ? (totalTrackedSeconds - previousSeconds) / previousSeconds * 100 : totalTrackedSeconds ? 100 : 0,
      timeEntryCount: rows.reduce((total, row) => total + row.timeEntryCount, 0),
      projectsWithTime: rows.filter((row) => row.trackedSeconds > 0).length,
      totalProjects: rows.length,
      clientsWithTime: [...clientTotals.values()].filter((seconds) => seconds > 0).length,
      totalClients: query.clientId ? 1 : clients.length,
    },
    timeByClient: [...clientTotals.entries()].filter(([, seconds]) => seconds > 0).map(([clientId, trackedSeconds]) => ({
      clientId,
      clientName: clients.find((client) => client.id === clientId)?.name || 'ไม่ระบุลูกค้า',
      trackedSeconds,
      percent: totalTrackedSeconds ? trackedSeconds / totalTrackedSeconds * 100 : 0,
    })).sort((first, second) => second.trackedSeconds - first.trackedSeconds),
    projectUsage: rows.map((row) => ({
      projectId: row.projectId,
      projectName: row.projectName,
      clientId: row.clientId,
      clientName: row.clientName,
      color: row.color,
      targetSeconds: row.targetSeconds,
      trackedSeconds: row.trackedSeconds,
      usagePercent: row.usagePercent,
      taskProgressPercent: row.taskProgressPercent,
      status: row.status,
    })),
  }
}
