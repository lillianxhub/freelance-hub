package th.ac.kku.freelance_hub.dto.response.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;

public record ReportDistributionResponse(
        ReportGroupBy groupBy,
        List<Item> items
) {
    public record Item(
            UUID id,
            String name,
            long trackedSeconds,
            BigDecimal percent
    ) {}
}