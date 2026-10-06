package th.ac.kku.freelance_hub.dto.response.report;

import java.time.LocalDate;
import java.util.List;

import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;

public record ReportWorkTrendResponse(
        ReportGranularity granularity,
        List<Point> points
) {
    public record Point(
            LocalDate periodStart,
            long trackedSeconds
    ) {}
}