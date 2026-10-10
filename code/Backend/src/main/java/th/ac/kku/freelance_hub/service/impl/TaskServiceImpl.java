package th.ac.kku.freelance_hub.service.impl;

import java.util.Map;
import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.mapper.TaskMapper;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.dto.request.task.ChangeTaskStatusRequest;
import th.ac.kku.freelance_hub.dto.request.task.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
@Service
public class TaskServiceImpl implements TaskService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "name", "status", "sortOrder",
            "completedAt", "createdAt", "updatedAt"
    );

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;
    private final TimeEntryService timeEntryService;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            TaskMapper taskMapper,
            TimeEntryService timeEntryService
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.taskMapper = taskMapper;
        this.timeEntryService = timeEntryService;
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
            boolean isActive,
            Pageable pageable
    ) {
        findOwnedProject(ownerId, projectId);

        return taskRepository.findAllByProjectIdAndProjectOwnerIdAndIsActive(
                projectId,
                ownerId,
                isActive,
                checkPageable(pageable)
        ).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID ownerId, UUID taskId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(taskId, "taskId is required");

        Task task = taskRepository
                .findByIdAndProjectOwnerIdAndProjectDeletedAtIsNull(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return taskMapper.toResponse(task);
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
    @Transactional(readOnly = true)
    public Optional<String> getLatestTimeEntryTaskName(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(1)
                .limit(1)
                .sortBy("startedAt")
                .direction(Sort.Direction.DESC)
                .build();

        return timeEntryService.list(ownerId, filter).stream()
                .findFirst()
                .map(TimeEntryResponse::getTaskName);
    }

    @Override
    @Transactional
    public TaskResponse update(UUID ownerId, UUID taskId, UpdateTaskRequest request) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(taskId, "taskId is required");
        Objects.requireNonNull(request, "request is required");

        Task task = taskRepository
                .findByIdAndProjectOwnerIdAndProjectDeletedAtIsNull(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return update(ownerId, task.getProject().getId(), taskId, request);
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
    public TaskResponse changeStatus(
            UUID ownerId,
            UUID taskId,
            ChangeTaskStatusRequest request
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(taskId, "taskId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.getStatus(), "status is required");

        Task task = taskRepository
                .findByIdAndProjectOwnerIdAndProjectDeletedAtIsNull(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        findEditableProjectForUpdate(ownerId, task.getProject().getId());
        taskRepository.refresh(task);
        requireStartedBeforeCompletion(task, request.getStatus());
        task.changeStatus(request.getStatus(), Instant.now());

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
        requireStartedBeforeCompletion(task, TaskStatus.COMPLETED);
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
    public void delete(UUID ownerId, UUID taskId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(taskId, "taskId is required");

        Task task = taskRepository
                .findByIdAndProjectOwnerIdAndProjectDeletedAtIsNull(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        delete(ownerId, task.getProject().getId(), taskId);
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

        task.softDelete();
        taskRepository.saveAndFlush(task);

        saveOrders(orderedTasks(ownerId, projectId));
    }

    private Project findOwnedProject(UUID ownerId, UUID projectId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(projectId, "projectId is required");

        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private static void requireStartedBeforeCompletion(Task task, TaskStatus nextStatus) {
        if (nextStatus == TaskStatus.COMPLETED && task.getStatus() == TaskStatus.OPEN) {
            throw new InvalidStateException("กรุณาเริ่มงานก่อนเปลี่ยนเป็นเสร็จสิ้น");
        }
    }

    private Project findEditableProjectForUpdate(
            UUID ownerId,
            UUID projectId
    ) {
        Project project = findOwnedProject(ownerId, projectId);

        // ล็อก Project เพื่อให้การสร้าง/ย้ายลำดับ Task ไม่ชนกัน
        projectRepository.refreshForUpdate(project);

        if(!project.canEditTasks()) {
            throw new InvalidStateException(
                    "ไม่สามารถแก้ไขงานของโปรเจกต์ที่เสร็จสิ้นหรือจัดเก็บแล้ว"
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
                        .stream()
                        .filter(task -> Boolean.TRUE.equals(task.getIsActive()))
                        .toList()
        );
    }

    private static int requirePosition(
            Integer position,
            int taskCount,
            boolean allowEnd
    ) {
        int maximum = allowEnd ? taskCount : taskCount - 1;

        if (position == null || position < 0 || position > maximum) {
            throw new InvalidArgumentException(
                    "ลำดับงานอยู่นอกช่วงที่อนุญาต", Map.of("field", "sortOrder", "min", 0, "max", maximum)
            );
        }

        return position;
    }

    private static Pageable checkPageable(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable is required");

        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new InvalidArgumentException(
                    "จำนวนงานต่อหน้าต้องอยู่ระหว่าง 1 ถึง 100"
            );
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidArgumentException(
                        "ฟิลด์ที่ใช้เรียงลำดับไม่ถูกต้อง", Map.of("field", "sortBy")
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

        Project project = tasks.get(0).getProject();
        tasks = new ArrayList<>(tasks);
        // เลขลำดับต้องไม่ซ้ำแม้เป็นงานที่ลบแล้ว จึงเก็บงานที่ไม่ใช้งานไว้ท้ายรายการ
        tasks.addAll(taskRepository
                .findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
                        project.getId(), project.getOwner().getId()
                )
                .stream()
                .filter(task -> !Boolean.TRUE.equals(task.getIsActive()))
                .toList());

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

            long temporaryStart = Math.max(maxOrder, tasks.size() - 1L) + 1;
            if (temporaryStart + tasks.size() - 1 > Integer.MAX_VALUE) {
                throw new InvalidStateException(
                        "ไม่สามารถเรียงงานใหม่ได้ เนื่องจากลำดับงานถึงขีดจำกัด"
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
