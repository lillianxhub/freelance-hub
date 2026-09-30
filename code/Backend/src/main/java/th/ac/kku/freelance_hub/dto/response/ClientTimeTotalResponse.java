package th.ac.kku.freelance_hub.dto.response;

import java.util.UUID;

/** Time spent on completed entries, grouped by the Client of each Project. */
public record ClientTimeTotalResponse(UUID clientId, String clientName, long totalSeconds) {
}
