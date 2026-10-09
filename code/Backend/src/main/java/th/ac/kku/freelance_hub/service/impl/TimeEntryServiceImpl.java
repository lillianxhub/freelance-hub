package th.ac.kku.freelance_hub.service.impl;

import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.timeentry.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.exception.TimeEntryLockedException;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.TimeEntryService;

/** Reads and manages time entries, including project totals and permanent locks. */
@Service
public class TimeEntryServiceImpl implements TimeEntryService {

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
    public TimeEntryResponse createManual(
            UUID ownerId,
            ManualTimeEntryRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(request, "request is required");
        validateManualTimeInput(request);

        User owner = findOwner(ownerId);
        Project project = findOwnedProject(ownerId, request.getProjectId());
        Task task = findTask(request.getTaskId(), project.getId(), ownerId);

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
            entry = TimeEntry.createManualWithDurationSeconds(
                    owner,
                    project,
                    task,
                    request.getDescription(),
                    request.getStartedAt(),
                    request.getDurationSeconds()
            );
        }

        // A completed task cannot receive new work. Check this before saving
        // the entry, then persist both changes in the same transaction.
        boolean taskStarted = task != null && task.start();
        TimeEntry savedEntry = timeEntryRepository.save(entry);
        if (taskStarted) {
            taskRepository.saveAndFlush(task);
        }
        return timeEntryMapper.toResponse(savedEntry);
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
        if (!entry.getProject().canTrackTime()) {
            throw new IllegalStateException("project must be active to track time");
        }

        Project targetProject = findOwnedProject(
                ownerId,
                request.getProjectId()
        );
        if (!targetProject.canTrackTime()) {
            throw new IllegalStateException("project must be active to track time");
        }
        Task targetTask = findTask(
                request.getTaskId(),
                targetProject.getId(),
                ownerId
        );

        if (request.getEndedAt() != null) {
            entry.updateTimeRange(request.getStartedAt(), request.getEndedAt());
        } else {
            entry.updateTimeRangeWithDurationSeconds(
                    request.getStartedAt(),
                    request.getDurationSeconds()
            );
        }
        entry.updateDetails(
                targetProject,
                targetTask,
                request.getDescription()
        );

        timeEntryRepository.flush();
        return timeEntryMapper.toResponse(entry);
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
            throw new InvalidStateException(
                    "กรุณายกเลิกตัวจับเวลาก่อนลบรายการเวลา"
            );
        }

        entry.softDelete(Instant.now(clock));
        timeEntryRepository.flush();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockByProject(UUID ownerId, UUID projectId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");

        List<TimeEntry> entries = timeEntryRepository
                .findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(ownerId, projectId);

        // Validate the entire batch before changing any entry.
        if (entries.stream().anyMatch(TimeEntry::isRunning)) {
            throw new InvalidStateException(
                    "กรุณาหยุดตัวจับเวลาก่อนล็อกรายการเวลาของโปรเจกต์"
            );
        }
        if (entries.isEmpty()) {
            return;
        }

        Instant lockedAt = Instant.now(clock);
        entries.forEach(entry -> entry.lock(lockedAt));
        // Managed entities are persisted in the caller's transaction.
        timeEntryRepository.flush();
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

    private Task findTask(UUID taskId, UUID projectId, UUID ownerId) {
        if (taskId == null) {
            return null;
        }
        return taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                taskId,
                projectId,
                ownerId
        ).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private TimeEntry findOwnedEntry(UUID ownerId, UUID entryId) {
        return timeEntryRepository
                .findByIdAndOwnerIdAndIsActiveTrue(entryId, ownerId)
                .orElseThrow(() -> new TimeEntryNotFoundException(entryId));
    }

    private static void validateManualTimeInput(
            ManualTimeEntryRequest request
    ) {
        boolean hasEndTime = request.getEndedAt() != null;
        boolean hasDuration = request.getDurationSeconds() != null;
        if (hasEndTime == hasDuration) {
            throw new InvalidArgumentException(
                    "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง", Map.of("fields", List.of("endedAt", "durationSeconds"))
            );
        }
    }

    private static void validateUpdateRequest(UpdateTimeEntryRequest request) {
        if (request.getProjectId() == null) {
            throw new InvalidArgumentException("กรุณาระบุโปรเจกต์", Map.of("field", "projectId"));
        }
        if (request.getStartedAt() == null) {
            throw new InvalidArgumentException("กรุณาระบุเวลาเริ่มต้น", Map.of("field", "startedAt"));
        }
        boolean hasEndTime = request.getEndedAt() != null;
        boolean hasDuration = request.getDurationSeconds() != null;
        if (hasEndTime == hasDuration) {
            throw new InvalidArgumentException(
                    "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง", Map.of("fields", List.of("endedAt", "durationSeconds"))
            );
        }
        if (hasDuration && request.getDurationSeconds() <= 0) {
            throw new InvalidArgumentException(
                    "ระยะเวลาต้องมากกว่าศูนย์", Map.of("field", "durationSeconds")
            );
        }
        if (!request.isTimeRangeValid()) {
            throw new InvalidArgumentException(
                    "เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น", Map.of("field", "endedAt")
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TimeEntryResponse getById(UUID ownerId, UUID entryId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(entryId, "entryId is required");

        return timeEntryRepository
                .findByIdAndOwnerIdAndIsActiveTrue(entryId, ownerId)
                .map(timeEntryMapper::toResponse)
                .orElseThrow(() -> new TimeEntryNotFoundException(entryId));
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
                .filter(TimeEntryServiceImpl::isVisibleAllocation)
                .mapToLong(TimeEntryRepository.AllocationView::getDurationSeconds)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
            throw new InvalidArgumentException(
                    "เวลาเริ่มต้นต้องอยู่ก่อนเวลาสิ้นสุด", Map.of("field", "from")
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
            throw new InvalidArgumentException(
                    "หมายเลขหน้าต้องเริ่มจาก 1 และจำนวนรายการต่อหน้าต้องอยู่ระหว่าง 1 ถึง 100"
            );
        }
        if (filter.getSortBy() == null
                || !SORT_FIELDS.contains(filter.getSortBy())) {
            throw new InvalidArgumentException(
                    "ฟิลด์ที่ใช้เรียงลำดับไม่ถูกต้อง", Map.of("field", "sortBy")
            );
        }
        if (filter.getDirection() == null) {
            throw new InvalidArgumentException(
                    "กรุณาระบุทิศทางการเรียงรายการเวลา", Map.of("field", "direction")
            );
        }
        validateTimeRange(filter);
    }

    private static void validateTimeRange(TimeEntryFilterRequest filter) {
        if (!filter.isTimeRangeValid()) {
            throw new InvalidArgumentException(
                    "เวลาเริ่มต้นต้องอยู่ก่อนเวลาสิ้นสุด", Map.of("field", "from")
            );
        }
    }
}
