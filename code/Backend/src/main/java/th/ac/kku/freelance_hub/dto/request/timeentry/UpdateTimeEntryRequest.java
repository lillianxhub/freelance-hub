package th.ac.kku.freelance_hub.dto.request.timeentry;

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

    @NotNull(message = "กรุณาระบุโปรเจกต์")
    private UUID projectId;

    private UUID taskId;

    private String description;

    @NotNull(message = "กรุณาระบุเวลาเริ่มต้น")
    private Instant startedAt;

    private Instant endedAt;

    @Positive(message = "ระยะเวลาต้องมากกว่าศูนย์")
    private Long durationSeconds;

    @AssertTrue(message = "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง")
    @JsonIgnore
    public boolean isTimeInputExclusive() {
        return (endedAt == null) != (durationSeconds == null);
    }

    @AssertTrue(message = "เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startedAt == null
                || endedAt == null
                || endedAt.isAfter(startedAt);
    }

}
