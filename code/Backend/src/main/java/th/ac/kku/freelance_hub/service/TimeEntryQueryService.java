package th.ac.kku.freelance_hub.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;

/** Read operations for time entries owned by the authenticated user. */
public interface TimeEntryQueryService {

    TimeEntryResponse getById(UUID ownerId, UUID entryId);

    Page<TimeEntryResponse> list(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );

    TimeEntrySummaryResponse summarize(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );

    /** Sums completed entries by their start date in Thailand. */
    long sumCompletedSeconds(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    );

    /** Includes zero-second days for a continuous Dashboard chart. */
    List<DailySeconds> sumDailySeconds(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    );

    /** Includes owned, visible projects even when their total is zero. */
    List<ProjectSeconds> sumSecondsByProject(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    );

    record DailySeconds(LocalDate day, long totalSeconds) { }

    record ProjectSeconds(UUID projectId, String projectName, long totalSeconds) { }
}
