package th.ac.kku.freelance_hub.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.mapper.TaskMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.service.TaskService;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "name", "status", "sortOrder",
            "completedAt", "createdAt", "updatedAt"
    );

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TaskMapper taskMapper;
    private final EntityManager entityManager;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            TimeEntryRepository timeEntryRepository,
            TaskMapper taskMapper,
            EntityManager entityManager
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.taskMapper = taskMapper;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TaskResponse create(
            UUID ownerId,
            UUID projectId,
            CreateTaskRequest request
    ) {
        Objects.requireNonNull(request, "request is required");
        Project project = findEditableProjectForUpdate(ownerId, projectId);
        List<Task> tasks = orderedTasks(ownerId, projectId);

        int position = requirePosition(request.getSortOrder(), tasks.size(), true);

        Task task = new Task(project, request.getName(), position);
        task.updateDetails(request.getName(), request.getDescription());
        tasks.add(position, task);

        saveOrders(tasks);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> list(
            UUID ownerId,
            UUID projectId,
            Pageable pageable
    ) {
        findOwnedProject(ownerId, projectId);

        return taskRepository.findAllByProjectIdAndProjectOwnerId(
                projectId,
                ownerId,
                checkPageable(pageable)
        ).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(
            UUID ownerId,
            UUID projectId,
            UUID taskId
    ) {
        return taskMapper.toResponse(
                findOwnedTask(ownerId, projectId, taskId)
        );
    }

    @Override
    @Transactional
    public TaskResponse update(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            UpdateTaskRequest request
    ) {
        Objects.requireNonNull(request, "request is required");
        findEditableProjectForUpdate(ownerId, projectId);

        Task task = findOwnedTask(ownerId, projectId, taskId);
        task.updateDetails(request.getName(), request.getDescription());

        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Override
    @Transactional
    public TaskResponse start(
            UUID ownerId,
            UUID projectId,
            UUID taskId
    ) {
        findEditableProjectForUpdate(ownerId, projectId);

        Task task = findOwnedTask(ownerId, projectId, taskId);
        task.start();

        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Override
    @Transactional
    public TaskResponse complete(
            UUID ownerId,
            UUID projectId,
            UUID taskId
    ) {
        findEditableProjectForUpdate(ownerId, projectId);

        Task task = findOwnedTask(ownerId, projectId, taskId);
        task.complete(Instant.now());

        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Override
    @Transactional
    public TaskResponse reorder(
            UUID ownerId,
            UUID projectId,
            UUID taskId,
            ReorderTaskRequest request
    ) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(taskId, "taskId is required");
        findEditableProjectForUpdate(ownerId, projectId);

        List<Task> tasks = orderedTasks(ownerId, projectId);
        Task task = tasks.stream()
                .filter(item -> taskId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        int position = requirePosition(
                request.getSortOrder(),
                tasks.size(),
                false
        );

        tasks.remove(task);
        tasks.add(position, task);

        saveOrders(tasks);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public void delete(
            UUID ownerId,
            UUID projectId,
            UUID taskId
    ) {
        findEditableProjectForUpdate(ownerId, projectId);
        Task task = findOwnedTask(ownerId, projectId, taskId);

        boolean hasTimeEntries = timeEntryRepository
                .findAllByOwnerIdAndTaskId(
                        ownerId,
                        taskId,
                        PageRequest.of(0, 1)
                )
                .hasContent();

        if (hasTimeEntries) {
            throw new IllegalStateException(
                    "Cannot delete a task that has time entries"
            );
        }

        taskRepository.delete(task);
        taskRepository.flush();

        saveOrders(orderedTasks(ownerId, projectId));
    }

    private Project findOwnedProject(UUID ownerId, UUID projectId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");

        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Project findEditableProjectForUpdate(
            UUID ownerId,
            UUID projectId
    ) {
        Project project = findOwnedProject(ownerId, projectId);

        // ล็อก Project เพื่อให้การสร้าง/ย้ายลำดับ Task ไม่ชนกัน
        entityManager.refresh(project, LockModeType.PESSIMISTIC_WRITE);

        if(!project.canEditTasks()) {
            throw new IllegalStateException(
                    "Cannot change tasks in a completed or archived project"
            );
        }
        

        return project;
    }

    private Task findOwnedTask(
            UUID ownerId,
            UUID projectId,
            UUID taskId
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");
        Objects.requireNonNull(taskId, "taskId is required");

        return taskRepository
                .findByIdAndProjectIdAndProjectOwnerId(
                        taskId,
                        projectId,
                        ownerId
                )
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private List<Task> orderedTasks(UUID ownerId, UUID projectId) {
        return new ArrayList<>(
                taskRepository
                        .findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
                                projectId,
                                ownerId
                        )
        );
    }

    private static int requirePosition(
            Integer position,
            int taskCount,
            boolean allowEnd
    ) {
        int maximum = allowEnd ? taskCount : taskCount - 1;

        if (position == null || position < 0 || position > maximum) {
            throw new IllegalArgumentException(
                    "sortOrder must be between 0 and " + maximum
            );
        }

        return position;
    }

    private static Pageable checkPageable(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable is required");

        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException(
                    "Task page size must be between 1 and 100"
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Unsupported task sort field: "
                                + order.getProperty()
                );
            }
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "sortOrder")
            );
        }

        return pageable;
    }

    private void saveOrders(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        boolean alreadyOrdered = true;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getSortOrder() != i) {
                alreadyOrdered = false;
                break;
            }
        }

        if (!alreadyOrdered) {
            long maxOrder = tasks.stream()
                    .mapToLong(Task::getSortOrder)
                    .max()
                    .orElse(-1);

            long temporaryStart = maxOrder + 1;
            if (temporaryStart + tasks.size() - 1 > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "Cannot reorder tasks: sortOrder limit reached"
                );
            }

            // ย้ายทุก Task ไปยังเลขชั่วคราวก่อน เพราะฐานข้อมูลห้าม
            // (project_id, sort_order) ซ้ำ แม้จะซ้ำเพียงระหว่างสลับตำแหน่ง
            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).reorder((int) (temporaryStart + i));
            }
            taskRepository.saveAllAndFlush(tasks);

            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).reorder(i);
            }
        }

        taskRepository.saveAllAndFlush(tasks);
    }
}