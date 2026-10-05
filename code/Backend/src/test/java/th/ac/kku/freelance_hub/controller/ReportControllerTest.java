package th.ac.kku.freelance_hub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.dto.response.report.ReportDistributionResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkTrendResponse;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
import th.ac.kku.freelance_hub.service.ReportService;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();

    @Mock private ReportService reportService;
    @Mock private CurrentUserProvider currentUserProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ReportController(reportService, currentUserProvider)
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void summaryPassesDateAndClientFilterToService() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER_ID);
        when(reportService.getSummary(
                eq(OWNER_ID), any(ReportFilterRequest.class)
        )).thenReturn(new ReportSummaryResponse(
                Instant.parse("2026-10-05T00:00:00Z"),
                new ReportSummaryResponse.Filters(List.of(), List.of()),
                new ReportSummaryResponse.Summary(
                        3600, BigDecimal.ZERO, 1, 1, 1, 1, 1
                )
        ));

        mockMvc.perform(get("/api/reports/summary")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-05")
                        .param("clientId", CLIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath(
                        "$.data.summary.totalTrackedSeconds"
                ).value(3600));

        verify(reportService).getSummary(
                eq(OWNER_ID),
                org.mockito.ArgumentMatchers.argThat(filter ->
                        filter.getFrom().equals(LocalDate.of(2026, 10, 1))
                        && filter.getTo().equals(LocalDate.of(2026, 10, 5))
                        && CLIENT_ID.equals(filter.getClientId())
                )
        );
    }

    @Test
    void trendAndDistributionPassTheirOptionsToService() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER_ID);
        when(reportService.getWorkTrend(
                eq(OWNER_ID),
                any(ReportFilterRequest.class),
                eq(ReportGranularity.WEEK)
        )).thenReturn(new ReportWorkTrendResponse(
                ReportGranularity.WEEK,
                List.of(new ReportWorkTrendResponse.Point(
                        LocalDate.of(2026, 9, 28), 5400
                ))
        ));
        when(reportService.getDistribution(
                eq(OWNER_ID),
                any(ReportFilterRequest.class),
                eq(ReportGroupBy.PROJECT)
        )).thenReturn(new ReportDistributionResponse(
                ReportGroupBy.PROJECT, List.of()
        ));

        mockMvc.perform(get("/api/reports/work-trend")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-05")
                        .param("granularity", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granularity").value("WEEK"))
                .andExpect(jsonPath(
                        "$.data.points[0].trackedSeconds"
                ).value(5400));

        mockMvc.perform(get("/api/reports/distribution")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-05")
                        .param("groupBy", "PROJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("PROJECT"));
    }

    @Test
    void projectsReturnsPaginationMeta() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER_ID);
        when(reportService.getProjects(
                eq(OWNER_ID),
                any(ReportFilterRequest.class),
                any(ReportProjectsRequest.class)
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(1, 10),
                12
        ));

        mockMvc.perform(get("/api/reports/projects")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-05")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.limit").value(10))
                .andExpect(jsonPath("$.meta.total").value(12))
                .andExpect(jsonPath("$.meta.totalPages").value(2));

        verify(reportService).getProjects(
                eq(OWNER_ID),
                any(ReportFilterRequest.class),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.getPage() == 2
                        && request.getLimit() == 10
                )
        );
    }

    @Test
    void invalidDateRangeDoesNotCallService() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .param("from", "2026-10-05")
                        .param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).getSummary(
                any(), any()
        );
    }

    @Test
    void summaryAcceptsNoDatesForAllTime() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(OWNER_ID);
        when(reportService.getSummary(
                eq(OWNER_ID), any(ReportFilterRequest.class)
        )).thenReturn(new ReportSummaryResponse(
                Instant.parse("2026-10-05T00:00:00Z"),
                new ReportSummaryResponse.Filters(List.of(), List.of()),
                new ReportSummaryResponse.Summary(
                        5400, null, 2, 1, 1, 1, 1
                )
        ));

        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.totalTrackedSeconds").value(5400));

        verify(reportService).getSummary(
                eq(OWNER_ID),
                org.mockito.ArgumentMatchers.argThat(filter ->
                        filter.getFrom() == null && filter.getTo() == null
                )
        );
    }
}
