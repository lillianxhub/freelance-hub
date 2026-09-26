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
 * Data submitted when creating a manual time entry.
 *
 * <p>The request must supply a start time and exactly one way to determine the
 * end of the entry: either {@code endedAt} or {@code durationMinutes}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualTimeEntryRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID taskId;

    private String description;

    @NotNull(message = "Start time is required")
    private Instant startedAt;

    private Instant endedAt;

    @Positive(message = "Duration minutes must be greater than zero")
    private Integer durationMinutes;

    @AssertTrue(message = "Provide either end time or duration minutes, but not both")
    @JsonIgnore
    public boolean isTimeInputExclusive() {
        return (endedAt == null) != (durationMinutes == null);
    }

    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startedAt == null
                || endedAt == null
                || endedAt.isAfter(startedAt);
    }
}
