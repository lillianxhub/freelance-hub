package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aggregated duration of completed time entries within a requested range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntrySummaryResponse {

    /** Inclusive lower bound used to calculate the summary. */
    private Instant from;

    /** Exclusive upper bound used to calculate the summary. */
    private Instant to;

    /** Number of completed entries included in the total. */
    private long entryCount;

    /** Sum of duration minutes from completed entries only. */
    private long totalMinutes;
}
