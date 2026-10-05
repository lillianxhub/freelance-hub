package th.ac.kku.freelance_hub.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.common.response.PaginationMeta;
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.dto.response.report.ReportDistributionResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkPatternResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkTrendResponse;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
import th.ac.kku.freelance_hub.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportController {
    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/summary")
    public ResponseEntity<ApiResult<ReportSummaryResponse>> getSummary(
            @Valid @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResult.success(
                "ดึงข้อมูลสรุปรายงานสำเร็จ",
                reportService.getSummary(ownerId(), filter)
        ));
    }

    @GetMapping("/work-trend")
    public ResponseEntity<ApiResult<ReportWorkTrendResponse>> getWorkTrend(
            @Valid @ModelAttribute ReportFilterRequest filter,
            @RequestParam(defaultValue = "DAY") ReportGranularity granularity
    ) {
        return ResponseEntity.ok(ApiResult.success(
                "ดึงแนวโน้มเวลาทำงานสำเร็จ",
                reportService.getWorkTrend(ownerId(), filter, granularity)
        ));
    }

    @GetMapping("/distribution")
    public ResponseEntity<ApiResult<ReportDistributionResponse>> getDistribution(
            @Valid @ModelAttribute ReportFilterRequest filter,
            @RequestParam(defaultValue = "CLIENT") ReportGroupBy groupBy
    ) {
        return ResponseEntity.ok(ApiResult.success(
                "ดึงสัดส่วนเวลาทำงานสำเร็จ",
                reportService.getDistribution(ownerId(), filter, groupBy)
        ));
    }

    @GetMapping("/work-pattern")
    public ResponseEntity<ApiResult<ReportWorkPatternResponse>> getWorkPattern(
            @Valid @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResult.success(
                "ดึงรูปแบบการทำงานสำเร็จ",
                reportService.getWorkPattern(ownerId(), filter)
        ));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResult<List<ReportProjectResponse>>> getProjects(
            @Valid @ModelAttribute ReportFilterRequest filter,
            @Valid @ModelAttribute ReportProjectsRequest request
    ) {
        var result = reportService.getProjects(ownerId(), filter, request);
        PaginationMeta meta = new PaginationMeta(
                request.getPage(),
                request.getLimit(),
                result.getTotalElements(),
                result.getTotalPages()
        );

        return ResponseEntity.ok(ApiResult.success(
                "ดึงรายงานโปรเจกต์สำเร็จ",
                result.getContent(),
                meta
        ));
    }

    private UUID ownerId() {
        return currentUserProvider.currentUserId();
    }
}