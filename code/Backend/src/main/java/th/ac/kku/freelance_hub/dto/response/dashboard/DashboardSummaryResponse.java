package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long weekTrackedSeconds,
        BigDecimal weekTrendPercent,
        long activeProjectCount,
        long activeProjectTrackedSeconds,
        long activeProjectTargetSeconds,
        BigDecimal targetUsagePercent,
        long completedTaskCount,
        long totalTaskCount,
        BigDecimal completedTaskPercent
) {}