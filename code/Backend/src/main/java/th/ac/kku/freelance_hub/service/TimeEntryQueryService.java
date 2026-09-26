package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;

/** Read operations for time entries owned by the authenticated user. */
public interface TimeEntryQueryService {

    Page<TimeEntryResponse> list(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );

    TimeEntrySummaryResponse summarize(
            UUID ownerId,
            TimeEntryFilterRequest filter
    );
}
