package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.UUID;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

public record ActiveProjectResponse(
        UUID id,
        String name,
        String clientName,
        String color,
        ProjectStatus status,
        long completedTaskCount,
        long totalTaskCount,
        BigDecimal taskProgressPercent
) {}