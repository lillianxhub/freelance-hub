package th.ac.kku.freelance_hub.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.ProjectMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.service.TimerService;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.mapper.TaskMapper;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.dto.request.project.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.project.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.project.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.project.ProjectListItemResponse;
import th.ac.kku.freelance_hub.dto.response.project.ProjectResponse;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "name", "status", "startDate", "endDate",
            "targetMinutes", "createdAt", "updatedAt"
    );


    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TimeEntryService timeEntryService;
    private final TimerService timerService;



    public ProjectServiceImpl(
        ProjectRepository projectRepository,
        ClientRepository clientRepository,
        UserRepository userRepository,
        ProjectMapper projectMapper,
        TaskRepository taskRepository,
        TaskMapper taskMapper,
        TimeEntryService timeEntryService,
        TimerService timerService
    ) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.timeEntryService = timeEntryService;
        this.timerService = timerService;
    }

    @Override
    @Transactional
    public ProjectResponse create(
            UUID ownerId,
            CreateProjectRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(request, "request is required");

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId));

        Client client = findOwnedClient(ownerId, request.getClientId());

        Project project = new Project(owner, client, request.getName());
        project.updateDetails(
                request.getName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor(),
                request.getTargetMinutes()
        );

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectListItemResponse getById(UUID ownerId, UUID projectId) {
        Project project = findOwnedProject(ownerId, projectId);
        List<TaskRepository.TaskProgressSummary> summaries =
                taskRepository.summarizeProgressByProjectIds(
                        ownerId, List.of(projectId), TaskStatus.COMPLETED
                );

        long totalTasks = summaries.isEmpty() ? 0 : summaries.get(0).getTotalTasks();
        long completedTasks = summaries.isEmpty() ? 0 : summaries.get(0).getCompletedTasks();

        ProjectListItemResponse response = projectMapper.toListItemResponse(
                project, totalTasks, completedTasks
        );
        response.setTimeTracking(timeTracking(ownerId, project));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectStatusCounts countActiveAndCompleted(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        long activeCount = projectRepository
                .countByOwnerIdAndStatusAndDeletedAtIsNull(
                        ownerId, ProjectStatus.ACTIVE
                );
        long completedCount = projectRepository
                .countByOwnerIdAndStatusAndDeletedAtIsNull(
                        ownerId, ProjectStatus.COMPLETED
                );

        return new ProjectStatusCounts(activeCount, completedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectProgress getProgress(UUID ownerId, UUID projectId) {
        Project project = findOwnedProject(ownerId, projectId);
        long trackedSeconds = trackedSeconds(ownerId, projectId);

        Integer targetMinutes = project.getTargetMinutes();
        if (targetMinutes == null) {
            return new ProjectProgress(
                    projectId, null, trackedSeconds, null, ProgressLevel.NO_TARGET
            );
        }

        BigDecimal target = BigDecimal.valueOf(targetMinutes.longValue() * 60);
        BigDecimal tracked = BigDecimal.valueOf(trackedSeconds);
        BigDecimal percent = usagePercent(targetMinutes, trackedSeconds);

        ProgressLevel level;
        if (tracked.compareTo(target) >= 0) {
            level = ProgressLevel.REACHED_100;
        } else if (tracked.multiply(BigDecimal.valueOf(5))
                .compareTo(target.multiply(BigDecimal.valueOf(4))) >= 0) {
            level = ProgressLevel.REACHED_80;
        } else {
            level = ProgressLevel.BELOW_80;
        }

        return new ProjectProgress(
                projectId, targetMinutes, trackedSeconds, percent, level
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectListItemResponse> list(
            UUID ownerId,
            String search,
            ProjectStatus status,
            UUID clientId,
            Pageable pageable,
            boolean includeTasks,
            boolean allStatuses
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Pageable checkedPageable = checkPageable(pageable);

        String searchTerm = search == null ? null : search.trim();
        if (searchTerm != null && searchTerm.length() > 180) {
            throw new IllegalArgumentException(
                    "Search must not exceed 180 characters"
            );
        }

        Specification<Project> specification = (root, query, cb) -> {
            Predicate predicate = cb.and(
                cb.equal(
                    root.get("owner").get("id"),
                    ownerId
                ),
                cb.isNull(root.get("deletedAt"))
            );

            if (status != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("status"), status)
                );
            } else if (!allStatuses) {
                predicate = cb.and(
                        predicate,
                        cb.notEqual(root.get("status"), ProjectStatus.ARCHIVED)
                );
            }

            if (clientId != null) {
                predicate = cb.and(
                        predicate,
                        cb.equal(root.get("client").get("id"), clientId)
                );
            }

            if (searchTerm != null && !searchTerm.isBlank()) {
                String pattern = "%"
                        + escapeLike(searchTerm.toLowerCase(Locale.ROOT))
                        + "%";

                Predicate projectNameMatches = cb.like(
                        cb.lower(root.get("name")),
                        pattern,
                        '\\'
                );

                Predicate clientNameMatches = cb.like(
                        cb.lower(root.get("client").get("name")),
                        pattern,
                        '\\'
                );

                predicate = cb.and(
                        predicate,
                        cb.or(
                                projectNameMatches,
                                clientNameMatches
                        )
                );
            }

            return predicate;
        };

        Page<Project> projects = projectRepository.findAll(
            specification,
            checkedPageable
        );

        Map<UUID, TaskRepository.TaskProgressSummary> progressByProject =
                new HashMap<>();
        Map<UUID, List<TaskResponse>> tasksByProject = new HashMap<>();

        if (projects.hasContent()) {
            List<UUID> projectIds = projects.getContent()
                    .stream()
                    .map(Project::getId)
                    .toList();

            List<TaskRepository.TaskProgressSummary> summaries =
                    taskRepository.summarizeProgressByProjectIds(
                            ownerId,
                            projectIds,
                            TaskStatus.COMPLETED
                    );

            for (TaskRepository.TaskProgressSummary summary : summaries) {
                progressByProject.put(summary.getProjectId(), summary);
            }

            if (includeTasks) {
                for (Task task : taskRepository.findActiveByProjectIds(
                        ownerId, projectIds)) {
                    tasksByProject.computeIfAbsent(
                            task.getProject().getId(), ignored -> new ArrayList<>()
                    ).add(taskMapper.toResponse(task));
                }
            }
        }

        return projects.map(project -> {
            TaskRepository.TaskProgressSummary summary =
                    progressByProject.get(project.getId());

            long totalTasks = summary == null
                    ? 0
                    : summary.getTotalTasks();

            long completedTasks = summary == null
                    ? 0
                    : summary.getCompletedTasks();

            ProjectListItemResponse response = projectMapper.toListItemResponse(
                    project,
                    totalTasks,
                    completedTasks
            );
            response.setTimeTracking(timeTracking(ownerId, project));
            if (includeTasks) {
                response.setTasks(tasksByProject.getOrDefault(
                        project.getId(), List.of()
                ));
            }
            return response;
        });
    }

    @Override
    @Transactional
    public ProjectResponse update(
            UUID ownerId,
            UUID projectId,
            UpdateProjectRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        Project project = findOwnedProject(ownerId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("ไม่สามารถแก้ไขโปรเจกต์ที่จัดเก็บแล้วได้");
        }
        Client client = findOwnedClient(ownerId, request.getClientId());

        project.changeClient(client);
        project.updateDetails(
                request.getName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor(),
                request.getTargetMinutes()
        );

        return projectMapper.toResponse(projectRepository.save(project));
    }


    //Method Change status
    @Override
    @Transactional
    public ProjectResponse changeStatus(
            UUID ownerId,
            UUID projectId,
            ChangeProjectStatusRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        Project project = findOwnedProject(ownerId, projectId);
        ProjectStatus previousStatus = project.getStatus();
        requireNoRunningTimer(ownerId, projectId);
        
        if (previousStatus == ProjectStatus.ACTIVE
                && request.getStatus() == ProjectStatus.COMPLETED) {
            List<TaskRepository.TaskProgressSummary> summaries =
                    taskRepository.summarizeProgressByProjectIds(
                            ownerId, List.of(projectId), TaskStatus.COMPLETED);
            if (!summaries.isEmpty()
                    && summaries.get(0).getTotalTasks()
                            > summaries.get(0).getCompletedTasks()) {
                throw new IllegalStateException(
                        "ไม่สามารถเปลี่ยนโปรเจกต์เป็นเสร็จสิ้นได้ เพราะยังมีงานย่อยที่ไม่เสร็จ");
            }
        }
        project.changeStatus(request.getStatus());
        if (previousStatus != ProjectStatus.COMPLETED
                && project.getStatus() == ProjectStatus.COMPLETED) {
            timeEntryService.lockByProject(ownerId, projectId);
        }

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void archive(UUID ownerId, UUID projectId) {
        Project project = findOwnedProject(ownerId, projectId);
        requireNoRunningTimer(ownerId, projectId);
        project.archive();
        projectRepository.save(project);
    }

    private ProjectListItemResponse.TimeTracking timeTracking(
            UUID ownerId,
            Project project
    ) {
        long seconds = trackedSeconds(ownerId, project.getId());
        return ProjectListItemResponse.TimeTracking.builder()
                .trackedSeconds(seconds)
                .trackedHours(BigDecimal.valueOf(seconds)
                        .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP))
                .usagePercent(usagePercent(project.getTargetMinutes(), seconds))
                .build();
    }

    private long trackedSeconds(UUID ownerId, UUID projectId) {
        return timeEntryService.summarize(
                ownerId,
                TimeEntryFilterRequest.builder().projectId(projectId).build()
        ).getTotalSeconds();
    }

    private static BigDecimal usagePercent(Integer targetMinutes, long seconds) {
        if (targetMinutes == null) {
            return null;
        }
        return BigDecimal.valueOf(seconds)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(targetMinutes.longValue() * 60),
                        2, RoundingMode.HALF_UP);
    }

    private void requireNoRunningTimer(UUID ownerId, UUID projectId) {
        if (timerService.getCurrentTimer(ownerId)
                .filter(timer -> projectId.equals(timer.getProjectId()))
                .isPresent()) {
            throw new IllegalStateException("กรุณาหยุดจับเวลาก่อนเปลี่ยนสถานะโปรเจกต์");
        }
    }

    private Project findOwnedProject(UUID ownerId, UUID projectId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");

        return projectRepository
                .findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Client findOwnedClient(UUID ownerId, UUID clientId) {
        Objects.requireNonNull(clientId, "clientId is required");

        return clientRepository
                .findByIdAndOwnerId(clientId, ownerId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    private static Pageable checkPageable(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable is required");

        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException(
                    "Project page size must be between 1 and 100"
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Unsupported project sort field: "
                                + order.getProperty()
                );
            }
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return pageable;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
