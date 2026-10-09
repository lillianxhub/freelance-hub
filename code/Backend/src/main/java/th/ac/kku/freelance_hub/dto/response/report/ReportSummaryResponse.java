package th.ac.kku.freelance_hub.dto.response.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportSummaryResponse(
        Instant generatedAt,
        Filters filters,
        Summary summary
) {
    public record Filters(
            List<ClientOption> clients,
            List<ProjectOption> projects
    ) {}

    public record ClientOption(UUID id, String name) {}

    public record ProjectOption(UUID id, String name, UUID clientId,
            th.ac.kku.freelance_hub.domain.enums.ProjectStatus status) {}

    public record Summary(
            long totalTrackedSeconds,
            BigDecimal trackedTimeTrendPercent,
            long timeEntryCount,
            long projectsWithTime,
            long totalProjects,
            long clientsWithTime,
            long totalClients
    ) {}
}
