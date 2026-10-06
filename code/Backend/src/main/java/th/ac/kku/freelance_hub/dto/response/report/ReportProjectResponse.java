package th.ac.kku.freelance_hub.dto.response.report;

import java.math.BigDecimal;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public record ReportProjectResponse(
        UUID projectId,
        String projectName,
        UUID clientId,
        String clientName,
        String color,
        Long targetSeconds,
        long trackedSeconds,
        BigDecimal usagePercent,
        BigDecimal taskProgressPercent,
        ProjectStatus status
) {}