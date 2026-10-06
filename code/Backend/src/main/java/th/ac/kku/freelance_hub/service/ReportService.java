package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.dto.response.report.ReportDistributionResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkPatternResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkTrendResponse;

public interface ReportService {
    ReportSummaryResponse getSummary(UUID ownerId, ReportFilterRequest filter);

    ReportWorkTrendResponse getWorkTrend(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportGranularity granularity
    );

    ReportDistributionResponse getDistribution(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportGroupBy groupBy
    );

    ReportWorkPatternResponse getWorkPattern(
            UUID ownerId,
            ReportFilterRequest filter
    );

    Page<ReportProjectResponse> getProjects(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportProjectsRequest request
    );
}