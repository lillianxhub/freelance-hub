import { api } from '../api/apiClient'
import type {
  ReportDistributionData,
  ReportProjectPage,
  ReportProjectUsage,
  ReportSummaryData,
  ReportSummaryQuery,
  ReportWorkPatternData,
  ReportWorkTrendData,
} from '../types/analytics'

function reportParams(query: ReportSummaryQuery): URLSearchParams {
  const params = new URLSearchParams()
  if (query.from && query.to) {
    params.set('from', query.from)
    params.set('to', query.to)
  }
  if (query.clientId) params.set('clientId', query.clientId)
  if (query.projectId) params.set('projectId', query.projectId)
  if (query.status) params.set('status', query.status)
  return params
}

function reportUrl(path: string, params: URLSearchParams): string {
  const search = params.toString()
  return `/reports/${path}${search ? `?${search}` : ''}`
}

export async function getReportSummary(query: ReportSummaryQuery): Promise<ReportSummaryData> {
  return (await api.get<ReportSummaryData>(reportUrl('summary', reportParams(query)))).data
}

export async function getReportDistribution(
  query: ReportSummaryQuery,
  groupBy: 'CLIENT' | 'PROJECT',
): Promise<ReportDistributionData> {
  const params = reportParams(query)
  params.set('groupBy', groupBy)
  return (await api.get<ReportDistributionData>(reportUrl('distribution', params))).data
}

export async function getReportProjects(
  query: ReportSummaryQuery,
  page = 1,
): Promise<ReportProjectPage> {
  const params = reportParams(query)
  params.set('page', String(page))
  params.set('limit', '10')
  const response = await api.get<ReportProjectUsage[]>(reportUrl('projects', params))
  return {
    items: response.data,
    page: response.meta?.page ?? page,
    totalPages: response.meta?.totalPages ?? 1,
    total: response.meta?.total ?? response.data.length,
  }
}

export async function getReportWorkTrend(
  query: ReportSummaryQuery,
  granularity: 'DAY' | 'WEEK' | 'MONTH',
): Promise<ReportWorkTrendData> {
  const params = reportParams(query)
  params.set('granularity', granularity)
  return (await api.get<ReportWorkTrendData>(reportUrl('work-trend', params))).data
}

export async function getReportWorkPattern(
  query: ReportSummaryQuery,
): Promise<ReportWorkPatternData> {
  return (await api.get<ReportWorkPatternData>(reportUrl('work-pattern', reportParams(query)))).data
}
