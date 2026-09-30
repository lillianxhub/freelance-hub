package th.ac.kku.freelance_hub.event;

import java.util.UUID;

public record ProjectProgressThresholdEvent(
        UUID ownerId,
        UUID projectId,
        int thresholdPercent
) {
}