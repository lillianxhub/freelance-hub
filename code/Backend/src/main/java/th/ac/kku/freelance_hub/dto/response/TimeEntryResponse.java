package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.EntryType;

/** Time-entry data returned by the API without exposing the JPA entity. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryResponse {

    private UUID id;

    private UUID clientId;

    private String clientName;

    private UUID projectId;

    private String projectName;

    private UUID taskId;

    private String taskName;

    private String description;

    private EntryType entryType;

    private Instant startedAt;

    private Instant endedAt;

    private Integer durationMinutes;

    private Instant lockedAt;

    private boolean running;

    private boolean locked;

    private Instant createdAt;

    private Instant updatedAt;

    private Long version;
}
