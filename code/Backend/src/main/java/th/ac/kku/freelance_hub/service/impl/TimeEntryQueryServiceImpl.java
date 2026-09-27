package th.ac.kku.freelance_hub.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;

/** Read-only list and summary queries for time entries. */
@Service
@Transactional(readOnly = true)
public class TimeEntryQueryServiceImpl implements TimeEntryQueryService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "startedAt",
            "endedAt",
            "durationMinutes",
            "createdAt",
            "updatedAt"
    );

    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryMapper timeEntryMapper;

    public TimeEntryQueryServiceImpl(
            TimeEntryRepository timeEntryRepository,
            TimeEntryMapper timeEntryMapper
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryMapper = timeEntryMapper;
    }

    @Override
    public Page<TimeEntryResponse> list(
            UUID ownerId,
            TimeEntryFilterRequest filter
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");
        validateListFilter(filter);

        PageRequest pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(filter.getDirection(), filter.getSortBy())
        );

        return timeEntryRepository.findAll(
                buildSpecification(ownerId, filter, false),
                pageable
        ).map(timeEntryMapper::toResponse);
    }

    @Override
    public TimeEntrySummaryResponse summarize(
            UUID ownerId,
            TimeEntryFilterRequest filter
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");
        validateTimeRange(filter);

        List<TimeEntry> completedEntries = timeEntryRepository.findAll(
                buildSpecification(ownerId, filter, true)
        );

        long entryCount = completedEntries.stream()
                .filter(entry -> !entry.isRunning())
                .filter(entry -> entry.getEndedAt() != null)
                .filter(entry -> entry.getDurationMinutes() != null)
                .count();
        long totalMinutes = completedEntries.stream()
                .filter(entry -> !entry.isRunning())
                .filter(entry -> entry.getEndedAt() != null)
                .map(TimeEntry::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        return TimeEntrySummaryResponse.builder()
                .from(filter.getFrom())
                .to(filter.getTo())
                .entryCount(entryCount)
                .totalMinutes(totalMinutes)
                .build();
    }

    private static Specification<TimeEntry> buildSpecification(
            UUID ownerId,
            TimeEntryFilterRequest filter,
            boolean completedOnly
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(
                    root.get("owner").get("id"),
                    ownerId
            );

            if (completedOnly) {
                predicate = cb.and(
                        predicate,
                        cb.isNotNull(root.get("endedAt")),
                        cb.isNotNull(root.get("durationMinutes"))
                );
            }
            if (filter.getClientId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                root.get("project")
                                        .get("client")
                                        .get("id"),
                                filter.getClientId()
                        )
                );
            }
            if (filter.getProjectId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                root.get("project").get("id"),
                                filter.getProjectId()
                        )
                );
            }
            if (filter.getTaskId() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(
                                root.get("task").get("id"),
                                filter.getTaskId()
                        )
                );
            }
            if (filter.getEntryType() != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("entryType"), filter.getEntryType())
                );
            }
            if (filter.getFrom() != null) {
                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(
                                root.get("startedAt"),
                                filter.getFrom()
                        )
                );
            }
            if (filter.getTo() != null) {
                predicate = cb.and(
                        predicate,
                        cb.lessThan(root.get("startedAt"), filter.getTo())
                );
            }

            return predicate;
        };
    }

    private static void validateListFilter(TimeEntryFilterRequest filter) {
        if (filter.getPage() < 0
                || filter.getSize() < 1
                || filter.getSize() > 100) {
            throw new IllegalArgumentException(
                    "Time entry page size must be between 1 and 100"
            );
        }
        if (filter.getSortBy() == null
                || !SORT_FIELDS.contains(filter.getSortBy())) {
            throw new IllegalArgumentException(
                    "Unsupported time entry sort field: "
                            + filter.getSortBy()
            );
        }
        if (filter.getDirection() == null) {
            throw new IllegalArgumentException(
                    "Time entry sort direction is required"
            );
        }
        validateTimeRange(filter);
    }

    private static void validateTimeRange(TimeEntryFilterRequest filter) {
        if (!filter.isTimeRangeValid()) {
            throw new IllegalArgumentException(
                    "From time must be before to time"
            );
        }
    }
}
