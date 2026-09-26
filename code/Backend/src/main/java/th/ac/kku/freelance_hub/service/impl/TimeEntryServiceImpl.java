package th.ac.kku.freelance_hub.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.RunningTimerNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.exception.TimeEntryLockedException;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.TimeEntryService;

@Service
public class TimeEntryServiceImpl implements TimeEntryService {

    private static final String RUNNING_TIMER_CONSTRAINT =
            "uk_time_entries_owner_running_timer";
    private static final Set<String> SORT_FIELDS = Set.of(
            "startedAt",
            "endedAt",
            "durationMinutes",
            "createdAt",
            "updatedAt"
    );

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TimeEntryMapper timeEntryMapper;
    private final Clock clock;

    public TimeEntryServiceImpl(
            TimeEntryRepository timeEntryRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TimeEntryMapper timeEntryMapper,
            Clock clock
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.timeEntryMapper = timeEntryMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TimeEntryResponse startTimer(
            UUID ownerId,
            StartTimerRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(request, "request is required");

        User owner = findOwner(ownerId);
        Project project = findOwnedProject(ownerId, request.getProjectId());
        Task task = findTask(
                request.getTaskId(),
                project.getId(),
                ownerId
        );

        TimeEntry entry = TimeEntry.startTimer(
                owner,
                project,
                task,
                request.getDescription(),
                Instant.now(clock)
        );

        if (timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        ownerId,
                        EntryType.TIMER
                )) {
            throw new TimerAlreadyRunningException();
        }

        try {
            return timeEntryMapper.toResponse(
                    timeEntryRepository.saveAndFlush(entry)
            );
        } catch (DataIntegrityViolationException ex) {
            // The database unique index closes the race between the pre-check
            // above and two concurrent timer inserts.
            if (isRunningTimerConstraintViolation(ex)) {
                throw new TimerAlreadyRunningException();
            }
            throw ex;
        }
    }

    private static boolean isRunningTimerConstraintViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.contains(RUNNING_TIMER_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Task findTask(
            UUID taskId,
            UUID projectId,
            UUID ownerId
    ) {
        if (taskId == null) {
            return null;
        }

        return taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                taskId,
                projectId,
                ownerId
        ).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public TimeEntryResponse getCurrentTimer(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        TimeEntry runningTimer = timeEntryRepository
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        ownerId,
                        EntryType.TIMER
                )
                .orElseThrow(RunningTimerNotFoundException::new);

        return timeEntryMapper.toResponse(runningTimer);
    }

    @Override
    @Transactional
    public TimeEntryResponse stopTimer(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        TimeEntry runningTimer = timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        ownerId,
                        EntryType.TIMER
                )
                .orElseThrow(RunningTimerNotFoundException::new);

        runningTimer.stop(Instant.now(clock));

        return timeEntryMapper.toResponse(runningTimer);
    }

    @Override
    @Transactional
    public void cancelTimer(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        TimeEntry runningTimer = timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        ownerId,
                        EntryType.TIMER
                )
                .orElseThrow(RunningTimerNotFoundException::new);

        timeEntryRepository.delete(runningTimer);
    }

    @Override
    @Transactional
    public TimeEntryResponse createManual(
            UUID ownerId,
            ManualTimeEntryRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(request, "request is required");
        validateManualTimeInput(request);

        User owner = findOwner(ownerId);
        Project project = findOwnedProject(ownerId, request.getProjectId());
        Task task = findTask(
                request.getTaskId(),
                project.getId(),
                ownerId
        );

        TimeEntry entry;
        if (request.getEndedAt() != null) {
            entry = TimeEntry.createManual(
                    owner,
                    project,
                    task,
                    request.getDescription(),
                    request.getStartedAt(),
                    request.getEndedAt()
            );
        } else {
            entry = TimeEntry.createManualWithDuration(
                    owner,
                    project,
                    task,
                    request.getDescription(),
                    request.getStartedAt(),
                    request.getDurationMinutes()
            );
        }

        return timeEntryMapper.toResponse(
                timeEntryRepository.save(entry)
        );
    }

    private User findOwner(UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId));
    }

    private Project findOwnedProject(UUID ownerId, UUID projectId) {
        Objects.requireNonNull(projectId, "projectId is required");
        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private static void validateManualTimeInput(
            ManualTimeEntryRequest request
    ) {
        boolean hasEndTime = request.getEndedAt() != null;
        boolean hasDuration = request.getDurationMinutes() != null;
        if (hasEndTime == hasDuration) {
            throw new IllegalArgumentException(
                    "Provide either end time or duration minutes, but not both"
            );
        }
    }

    @Override
    @Transactional
    public TimeEntryResponse update(
            UUID ownerId,
            UUID entryId,
            UpdateTimeEntryRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(entryId, "entryId is required");
        Objects.requireNonNull(request, "request is required");
        validateUpdateRequest(request);

        TimeEntry entry = findOwnedEntry(ownerId, entryId);
        if (entry.isLocked()) {
            throw new TimeEntryLockedException(entryId);
        }

        boolean projectChanged = request.getProjectId() != null;
        boolean detailsChanged = projectChanged
                || request.getTaskId() != null
                || request.isClearTask()
                || request.getDescription() != null;

        Project targetProject = projectChanged
                ? findOwnedProject(ownerId, request.getProjectId())
                : entry.getProject();
        Task targetTask = resolveUpdatedTask(
                request,
                entry,
                targetProject,
                ownerId,
                projectChanged
        );
        String targetDescription = request.getDescription() != null
                ? request.getDescription()
                : entry.getDescription();

        if (request.getStartedAt() != null) {
            entry.updateTimeRange(
                    request.getStartedAt(),
                    request.getEndedAt()
            );
        }
        if (detailsChanged) {
            entry.updateDetails(
                    targetProject,
                    targetTask,
                    targetDescription
            );
        }

        return timeEntryMapper.toResponse(entry);
    }

    private TimeEntry findOwnedEntry(UUID ownerId, UUID entryId) {
        return timeEntryRepository.findByIdAndOwnerId(entryId, ownerId)
                .orElseThrow(() -> new TimeEntryNotFoundException(entryId));
    }

    private Task resolveUpdatedTask(
            UpdateTimeEntryRequest request,
            TimeEntry entry,
            Project targetProject,
            UUID ownerId,
            boolean projectChanged
    ) {
        if (request.isClearTask()) {
            return null;
        }
        if (request.getTaskId() != null) {
            return findTask(
                    request.getTaskId(),
                    targetProject.getId(),
                    ownerId
            );
        }
        if (projectChanged) {
            return null;
        }
        return entry.getTask();
    }

    private static void validateUpdateRequest(UpdateTimeEntryRequest request) {
        if (!request.isAnyFieldProvided()) {
            throw new IllegalArgumentException(
                    "At least one field must be provided"
            );
        }
        if (!request.isTaskUpdateValid()) {
            throw new IllegalArgumentException(
                    "Task ID and clear task cannot be used together"
            );
        }
        if (!request.isTimeRangeComplete()) {
            throw new IllegalArgumentException(
                    "Start time and end time must be provided together"
            );
        }
        if (!request.isTimeRangeValid()) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID entryId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(entryId, "entryId is required");

        TimeEntry entry = findOwnedEntry(ownerId, entryId);
        if (entry.isLocked()) {
            throw new TimeEntryLockedException(entryId);
        }
        if (entry.isRunning()) {
            throw new IllegalStateException(
                    "a running timer must be cancelled"
            );
        }

        timeEntryRepository.delete(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimeEntryResponse> list(
            UUID ownerId,
            TimeEntryFilterRequest filter
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");
        validateFilter(filter);

        Specification<TimeEntry> specification = buildSpecification(
                ownerId,
                filter,
                false
        );

        PageRequest pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(filter.getDirection(), filter.getSortBy())
        );

        return timeEntryRepository.findAll(specification, pageable)
                .map(timeEntryMapper::toResponse);
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
                        cb.equal(
                                root.get("entryType"),
                                filter.getEntryType()
                        )
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
                        cb.lessThan(
                                root.get("startedAt"),
                                filter.getTo()
                        )
                );
            }

            return predicate;
        };
    }

    private static void validateFilter(TimeEntryFilterRequest filter) {
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
        if (!filter.isTimeRangeValid()) {
            throw new IllegalArgumentException(
                    "From time must be before to time"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TimeEntrySummaryResponse summarize(
            UUID ownerId,
            TimeEntryFilterRequest filter
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");
        validateSummaryFilter(filter);

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

    private static void validateSummaryFilter(
            TimeEntryFilterRequest filter
    ) {
        if (!filter.isTimeRangeValid()) {
            throw new IllegalArgumentException(
                    "From time must be before to time"
            );
        }
    }
}
