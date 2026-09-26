package th.ac.kku.freelance_hub.service.impl;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
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

    public TimeEntryServiceImpl(
            TimeEntryRepository timeEntryRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TimeEntryMapper timeEntryMapper
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.timeEntryMapper = timeEntryMapper;
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
            entry = TimeEntry.createManualWithDuration(
                    owner,
                    project,
                    task,
                    request.getDescription(),
                    request.getStartedAt(),
                    request.getDurationMinutes()
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
            entry.updateTimeRange(request.getStartedAt(), request.getEndedAt());
        }
        if (detailsChanged) {
            entry.updateDetails(targetProject, targetTask, targetDescription);
        }

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

        timeEntryRepository.delete(entry);
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
        return projectChanged ? null : entry.getTask();
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
}
