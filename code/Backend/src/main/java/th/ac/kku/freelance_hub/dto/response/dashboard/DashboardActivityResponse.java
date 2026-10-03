package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.util.List;
import th.ac.kku.freelance_hub.dto.request.dashboard.DashboardActivityPeriod;
public record DashboardActivityResponse(
        DashboardActivityPeriod period,
        List<DashboardActivityPointResponse> points
) {}
