package th.ac.kku.freelance_hub.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fields that may be changed on an unlocked time entry.
 *
 * <p>Null fields are generally left unchanged. If a time range is updated,
 * both its start and end must be supplied; the domain permits this operation
 * only for a completed entry.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTimeEntryRequest {

    private UUID projectId;

    private UUID taskId;

    /** True removes the current task association; cannot be used with taskId. */
    private Boolean clearTask;

    private String description;

    private Instant startedAt;

    private Instant endedAt;

    @AssertTrue(message = "At least one field must be provided")
    @JsonIgnore
    public boolean isAnyFieldProvided() {
        return projectId != null
                || taskId != null
                || isClearTask()
                || description != null
                || startedAt != null
                || endedAt != null;
    }

    @AssertTrue(message = "Task ID and clear task cannot be used together")
    @JsonIgnore
    public boolean isTaskUpdateValid() {
        return taskId == null || !isClearTask();
    }

    @AssertTrue(message = "Start time and end time must be provided together")
    @JsonIgnore
    public boolean isTimeRangeComplete() {
        return (startedAt == null) == (endedAt == null);
    }

    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startedAt == null
                || endedAt == null
                || endedAt.isAfter(startedAt);
    }

    public boolean isClearTask() {
        return Boolean.TRUE.equals(clearTask);
    }
}
