package th.ac.kku.freelance_hub.service;

import java.util.UUID;
import th.ac.kku.freelance_hub.dto.request.DashboardActivityPeriod;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(UUID ownerId);
    DashboardActivityResponse getActivity(UUID ownerId, DashboardActivityPeriod period);
}
