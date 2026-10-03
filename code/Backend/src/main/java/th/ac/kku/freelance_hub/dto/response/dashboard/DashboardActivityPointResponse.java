package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.time.LocalDate;

public record DashboardActivityPointResponse(
        LocalDate date,
        long trackedSeconds
) {}
