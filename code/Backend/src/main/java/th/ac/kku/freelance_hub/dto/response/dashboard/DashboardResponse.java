package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        Instant generatedAt,
        DashboardSummaryResponse summary,
        List<DailyWorkResponse> dailyWork,
        List<ActiveProjectResponse> activeProjects,
        List<OpenTaskResponse> openTasks
) {}