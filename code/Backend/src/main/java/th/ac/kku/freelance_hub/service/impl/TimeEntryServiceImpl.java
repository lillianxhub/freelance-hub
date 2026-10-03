package th.ac.kku.freelance_hub.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
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

/** Commands that create, edit, or delete completed time entries. */
@Service
public class TimeEntryServiceImpl implements TimeEntryService {

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

        return timeEntryMapper.toResponse(timeEntryRepository.save(entry));
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

        Project targetProject = findOwnedProject(
                ownerId,
                request.getProjectId()
        );
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
            throw new IllegalStateException(
                    "a running timer must be cancelled"
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
            throw new IllegalStateException(
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
            throw new IllegalArgumentException(
                    "Provide either end time or duration seconds, but not both"
            );
        }
    }

    private static void validateUpdateRequest(UpdateTimeEntryRequest request) {
        if (request.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (request.getStartedAt() == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        boolean hasEndTime = request.getEndedAt() != null;
        boolean hasDuration = request.getDurationSeconds() != null;
        if (hasEndTime == hasDuration) {
            throw new IllegalArgumentException(
                    "Provide either end time or duration seconds, but not both"
            );
        }
        if (hasDuration && request.getDurationSeconds() <= 0) {
            throw new IllegalArgumentException(
                    "Duration seconds must be greater than zero"
            );
        }
        if (!request.isTimeRangeValid()) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }
}
