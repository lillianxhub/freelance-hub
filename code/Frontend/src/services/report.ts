import type { ReportSummaryData, ReportSummaryQuery } from '../types/analytics'
import { createReportSummaryMock } from '../Analytics/reports.mock'

/**
 * Temporary mock implementation of GET /api/reports/summary.
 * Replace the body with api.get<ReportSummaryData>(...) when the backend endpoint is ready.
 */
export async function getReportSummary(query: ReportSummaryQuery): Promise<ReportSummaryData> {
  return Promise.resolve(createReportSummaryMock(query))
}
