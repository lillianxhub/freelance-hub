package th.ac.kku.freelance_hub.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;

/** Read-only detail, list, and aggregate queries for time entries. */
@Service
@Transactional(readOnly = true)
public class TimeEntryQueryServiceImpl implements TimeEntryQueryService {

    private static final ZoneId THAILAND_ZONE = ZoneId.of("Asia/Bangkok");

    private static final Set<String> SORT_FIELDS = Set.of(
            "startedAt",
            "endedAt",
            "durationSeconds",
            "createdAt",
            "updatedAt"
    );

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryMapper timeEntryMapper;

    public TimeEntryQueryServiceImpl(
            TimeEntryRepository timeEntryRepository,
            ProjectRepository projectRepository,
            TimeEntryMapper timeEntryMapper
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.timeEntryMapper = timeEntryMapper;
    }

    @Override
    public TimeEntryResponse getById(UUID ownerId, UUID entryId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(entryId, "entryId is required");

        return timeEntryRepository
                .findByIdAndOwnerIdAndIsActiveTrue(entryId, ownerId)
                .map(timeEntryMapper::toResponse)
                .orElseThrow(() -> new TimeEntryNotFoundException(entryId));
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
                filter.getPage() - 1,
                filter.getLimit(),
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
                .filter(entry -> entry.getDurationSeconds() != null)
                .count();
        long totalSeconds = completedEntries.stream()
                .filter(entry -> !entry.isRunning())
                .filter(entry -> entry.getEndedAt() != null)
                .map(TimeEntry::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        return TimeEntrySummaryResponse.builder()
                .from(filter.getFrom())
                .to(filter.getTo())
                .entryCount(entryCount)
                .totalSeconds(totalSeconds)
                .build();
    }

    @Override
    public long sumCompletedSeconds(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    ) {
        return findCompleted(
                ownerId,
                fromInclusive,
                toExclusive,
                TimeEntryRepository.AllocationView.class
        ).stream()
                .filter(TimeEntryQueryServiceImpl::isVisibleAllocation)
                .mapToLong(TimeEntryRepository.AllocationView::getDurationSeconds)
                .sum();
    }

    @Override
    public List<DailySeconds> sumDailySeconds(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    ) {
        List<TimeEntryRepository.AllocationView> entries = findCompleted(
                ownerId,
                fromInclusive,
                toExclusive,
                TimeEntryRepository.AllocationView.class
        );

        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        for (LocalDate day = fromInclusive; day.isBefore(toExclusive); day = day.plusDays(1)) {
            totals.put(day, 0L);
        }
        for (TimeEntryRepository.AllocationView entry : entries) {
            if (!isVisibleAllocation(entry)) {
                continue;
            }
            LocalDate day = entry.getStartedAt()
                    .atZone(THAILAND_ZONE)
                    .toLocalDate();
            totals.merge(day, entry.getDurationSeconds(), Long::sum);
        }

        return totals.entrySet().stream()
                .map(item -> new DailySeconds(item.getKey(), item.getValue()))
                .toList();
    }

    @Override
    public List<ProjectSeconds> sumSecondsByProject(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive
    ) {
        List<TimeEntryRepository.AllocationView> entries = findCompleted(
                ownerId, fromInclusive, toExclusive,
                TimeEntryRepository.AllocationView.class
        );
        Specification<Project> visibleOwnedProjects = (root, query, cb) -> cb.and(
                cb.equal(root.get("owner").get("id"), ownerId),
                cb.isTrue(root.get("isActive"))
        );
        List<Project> projects = projectRepository.findAll(
                visibleOwnedProjects, Sort.by("name", "id")
        );
        Map<UUID, Long> totals = new LinkedHashMap<>();
        for (Project project : projects) {
            totals.put(project.getId(), 0L);
        }
        for (TimeEntryRepository.AllocationView entry : entries) {
            if (isVisibleAllocation(entry)
                    && totals.containsKey(entry.getProject().getId())) {
                totals.merge(entry.getProject().getId(),
                        entry.getDurationSeconds(), Long::sum);
            }
        }
        return projects.stream()
                .map(project -> new ProjectSeconds(
                        project.getId(), project.getName(), totals.get(project.getId())
                ))
                .toList();
    }

    private static boolean isVisibleAllocation(
            TimeEntryRepository.AllocationView entry
    ) {
        TimeEntryRepository.ProjectView project = entry.getProject();
        if (project == null || !Boolean.TRUE.equals(project.getIsActive())) {
            return false;
        }
        TimeEntryRepository.TaskView task = entry.getTask();
        return task == null || Boolean.TRUE.equals(task.getIsActive());
    }

    private <T> List<T> findCompleted(
            UUID ownerId,
            LocalDate fromInclusive,
            LocalDate toExclusive,
            Class<T> projectionType
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(fromInclusive, "fromInclusive is required");
        Objects.requireNonNull(toExclusive, "toExclusive is required");
        if (!fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException(
                    "fromInclusive must be before toExclusive"
            );
        }

        return timeEntryRepository
                .findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        ownerId,
                        fromInclusive.atStartOfDay(THAILAND_ZONE).toInstant(),
                        toExclusive.atStartOfDay(THAILAND_ZONE).toInstant(),
                        projectionType
                );
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
            predicate = cb.and(
                    predicate,
                    cb.isTrue(root.get("isActive"))
            );

            if (completedOnly) {
                predicate = cb.and(
                        predicate,
                        cb.isNotNull(root.get("endedAt")),
                        cb.isNotNull(root.get("durationSeconds"))
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
        if (filter.getPage() < 1
                || filter.getLimit() < 1
                || filter.getLimit() > 100) {
            throw new IllegalArgumentException(
                    "Time entry page must be at least 1 and limit must be between 1 and 100"
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
