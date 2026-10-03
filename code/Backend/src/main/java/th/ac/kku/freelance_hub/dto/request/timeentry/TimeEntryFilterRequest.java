package th.ac.kku.freelance_hub.dto.request.timeentry;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Sort;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.EntryType;

/** Filters and pagination options for listing the current user's time entries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryFilterRequest {

    private UUID clientId;

    private UUID projectId;

    private UUID taskId;

    private EntryType entryType;

    /** Inclusive lower bound for {@code startedAt}. */
    private Instant from;

    /** Exclusive upper bound for {@code startedAt}. */
    private Instant to;

    @Builder.Default
    @Min(value = 1, message = "Page must be at least 1")
    private int page = 1;

    @Builder.Default
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private int limit = 20;

    /** Allow only known TimeEntry properties as sort keys. */
    @Builder.Default
    @NotBlank(message = "Sort field is required")
    @Pattern(
            regexp = "startedAt|endedAt|durationSeconds|createdAt|updatedAt",
            message = "Sort field is not supported"
    )
    private String sortBy = "startedAt";

    @Builder.Default
    @NotNull(message = "Sort direction is required")
    private Sort.Direction direction = Sort.Direction.DESC;

    @AssertTrue(message = "From time must be before to time")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return from == null || to == null || from.isBefore(to);
    }
}
