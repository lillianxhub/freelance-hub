package th.ac.kku.freelance_hub.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.event.TimerStoppedEvent;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.RunningTimerNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.TimerService;

/** Coordinates the lifecycle of the authenticated user's running timer. */
@Service
public class TimerServiceImpl implements TimerService {

    private static final String RUNNING_TIMER_CONSTRAINT =
            "uk_time_entries_owner_running_timer";

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TimeEntryMapper timeEntryMapper;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public TimerServiceImpl(
            TimeEntryRepository timeEntryRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TimeEntryMapper timeEntryMapper,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.timeEntryMapper = timeEntryMapper;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
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
        Task task = findTask(request.getTaskId(), project.getId(), ownerId);

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
            if (isRunningTimerConstraintViolation(ex)) {
                throw new TimerAlreadyRunningException();
            }
            throw ex;
        }
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

        TimeEntry runningTimer = findLockedRunningTimer(ownerId);
        runningTimer.stop(Instant.now(clock));

        eventPublisher.publishEvent(new TimerStoppedEvent(
                runningTimer.getId(),
                runningTimer.getOwner().getId(),
                runningTimer.getProject().getId(),
                runningTimer.getTask() == null
                        ? null
                        : runningTimer.getTask().getId(),
                runningTimer.getDurationMinutes(),
                runningTimer.getStartedAt(),
                runningTimer.getEndedAt()
        ));

        return timeEntryMapper.toResponse(runningTimer);
    }

    @Override
    @Transactional
    public void cancelTimer(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        timeEntryRepository.delete(findLockedRunningTimer(ownerId));
    }

    private TimeEntry findLockedRunningTimer(UUID ownerId) {
        return timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        ownerId,
                        EntryType.TIMER
                )
                .orElseThrow(RunningTimerNotFoundException::new);
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
}
