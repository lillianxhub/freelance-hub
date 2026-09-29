package th.ac.kku.freelance_hub.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete replacement data for an unlocked time entry.
 *
 * <p>The request must supply a project, a start time, and exactly one way to
 * determine the end of the entry. A null task removes the current task and a
 * null description clears the current description.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTimeEntryRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID taskId;

    private String description;

    @NotNull(message = "Start time is required")
    private Instant startedAt;

    private Instant endedAt;

    @Positive(message = "Duration seconds must be greater than zero")
    private Long durationSeconds;

    @AssertTrue(message = "Provide either end time or duration seconds, but not both")
    @JsonIgnore
    public boolean isTimeInputExclusive() {
        return (endedAt == null) != (durationSeconds == null);
    }

    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startedAt == null
                || endedAt == null
                || endedAt.isAfter(startedAt);
    }

}
