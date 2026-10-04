package th.ac.kku.freelance_hub.dto.response.dashboard;

import java.time.Instant;
import java.util.UUID;

public record RecentTimeEntryResponse(
        UUID id,
        String projectName,
        String taskName,
        String description,
        Instant startedAt,
        long durationSeconds
) {}
