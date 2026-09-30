package th.ac.kku.freelance_hub.repository;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;



@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectRepository projectRepository;

        @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void filtersProjectsByOwnerStatusAndClient() {
        User owner = userRepository.saveAndFlush(User.builder()
                .email("project-owner@example.com")
                .passwordHash("test-hash")
                .build());

        User otherOwner = userRepository.saveAndFlush(User.builder()
                .email("other-owner@example.com")
                .passwordHash("test-hash")
                .build());

        Client firstClient = clientRepository.saveAndFlush(
                new Client(owner, "First Client"));
        Client secondClient = clientRepository.saveAndFlush(
                new Client(owner, "Second Client"));
        Client otherClient = clientRepository.saveAndFlush(
                new Client(otherOwner, "Other Client"));

        Project planned = projectRepository.saveAndFlush(
                new Project(owner, firstClient, "Planned Project"));

        Project activeProject = new Project(
                owner, secondClient, "Active Project");
        activeProject.changeStatus(ProjectStatus.ACTIVE);
        activeProject = projectRepository.saveAndFlush(activeProject);

        Project otherProject = projectRepository.saveAndFlush(
                new Project(otherOwner, otherClient, "Other Project"));

        PageRequest page = PageRequest.of(0, 10);

        assertThat(projectRepository.findByIdAndOwnerId(
                planned.getId(), owner.getId()
        )).isPresent();

        assertThat(projectRepository.findByIdAndOwnerId(
                otherProject.getId(), owner.getId()
        )).isEmpty();

        assertThat(projectRepository.existsByIdAndOwnerId(
                planned.getId(), owner.getId()
        )).isTrue();

        assertThat(projectRepository.existsByIdAndOwnerId(
                otherProject.getId(), owner.getId()
        )).isFalse();

        assertThat(projectRepository.findAllByOwnerId(
                owner.getId(), page
        ).getContent())
                .extracting(Project::getId)
                .containsExactlyInAnyOrder(
                        planned.getId(), activeProject.getId());

        assertThat(projectRepository.findAllByOwnerIdAndStatus(
                owner.getId(), ProjectStatus.PLANNED, page
        ).getContent())
                .extracting(Project::getId)
                .containsExactly(planned.getId());

        assertThat(projectRepository.findAllByOwnerIdAndClientId(
                owner.getId(), firstClient.getId(), page
        ).getContent())
                .extracting(Project::getId)
                .containsExactly(planned.getId());
    }

        @Test
    void softDeletePreservesDataAndHidesDeletedProjectFromApiQueries() {
        User owner = userRepository.saveAndFlush(
                User.builder()
                        .email("soft-delete-owner@example.com")
                        .passwordHash("test-hash")
                        .build()
        );

        Client client = clientRepository.saveAndFlush(
                new Client(owner, "My Client")
        );

        Project archivedProject = new Project(
                owner,
                client,
                "Archived but not deleted"
        );
        archivedProject.changeStatus(ProjectStatus.ARCHIVED);
        archivedProject = projectRepository.saveAndFlush(archivedProject);

        Project deletedProject = projectRepository.saveAndFlush(
                new Project(owner, client, "Project to delete")
        );

        Task task = taskRepository.saveAndFlush(
                new Task(deletedProject, "Existing Task", 0)
        );

        projectService.archive(owner.getId(), deletedProject.getId());

        projectRepository.flush();
        entityManager.clear();

        // โหลดใหม่จากฐานข้อมูล เพื่อยืนยันว่าข้อมูลยังอยู่
        Project storedProject = projectRepository
                .findById(deletedProject.getId())
                .orElseThrow();

        assertThat(storedProject.getStatus())
                .isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(storedProject.getIsActive()).isFalse();
        assertThat(storedProject.getDeletedAt()).isNotNull();

        assertThat(taskRepository.findById(task.getId())).isPresent();

        // การค้นหาสำหรับ API ต้องไม่พบ Project ที่ลบแล้ว
        assertThat(projectRepository.findByIdAndOwnerId(
                deletedProject.getId(),
                owner.getId()
        )).isEmpty();

        assertThatThrownBy(() -> projectService.getById(
                owner.getId(),
                deletedProject.getId()
        )).isInstanceOf(ProjectNotFoundException.class);

        // ARCHIVED ที่ยังไม่ถูกลบต้องยังปรากฏในรายการ
        var result = projectService.list(
                owner.getId(),
                null,
                ProjectStatus.ARCHIVED,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(response -> response.getId())
                .containsExactly(archivedProject.getId());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listHidesArchivedProjectsUnlessStatusIsExplicitlyFiltered() {
        User owner = userRepository.saveAndFlush(User.builder()
                .email("archived-list-owner@example.com")
                .passwordHash("test-hash")
                .build());
        Client client = clientRepository.saveAndFlush(
                new Client(owner, "Archived List Client"));

        Project planned = projectRepository.saveAndFlush(
                new Project(owner, client, "Planned"));
        Project active = new Project(owner, client, "Active");
        active.changeStatus(ProjectStatus.ACTIVE);
        active = projectRepository.saveAndFlush(active);
        Project archived = new Project(owner, client, "Archived");
        archived.changeStatus(ProjectStatus.ARCHIVED);
        archived = projectRepository.saveAndFlush(archived);

        var defaultList = projectService.list(
                owner.getId(), null, null, null, PageRequest.of(0, 10));
        assertThat(defaultList.getContent())
                .extracting(response -> response.getId())
                .containsExactlyInAnyOrder(planned.getId(), active.getId());
        assertThat(defaultList.getTotalElements()).isEqualTo(2);

        var paginatedList = projectService.list(
                owner.getId(), null, null, null, PageRequest.of(0, 1));
        assertThat(paginatedList.getContent()).hasSize(1);
        assertThat(paginatedList.getTotalElements()).isEqualTo(2);
        assertThat(paginatedList.getTotalPages()).isEqualTo(2);

        var archivedList = projectService.list(
                owner.getId(), null, ProjectStatus.ARCHIVED,
                null, PageRequest.of(0, 10));
        assertThat(archivedList.getContent())
                .extracting(response -> response.getId())
                .containsExactly(archived.getId());
        assertThat(archivedList.getTotalElements()).isEqualTo(1);

        var activeList = projectService.list(
                owner.getId(), null, ProjectStatus.ACTIVE,
                null, PageRequest.of(0, 10));
        assertThat(activeList.getContent())
                .extracting(response -> response.getId())
                .containsExactly(active.getId());
    }

    @Test
    void projectDetailCountsOnlyItsActiveTasksAndChecksOwnership() {
        User owner = userRepository.saveAndFlush(User.builder()
                .email("detail-owner@example.com").passwordHash("test-hash").build());
        Client client = clientRepository.saveAndFlush(new Client(owner, "Detail Client"));
        Project project = new Project(owner, client, "Detail Project");
        project.updateDetails("Detail Project", "Description", null, null, null, 2160);
        project = projectRepository.saveAndFlush(project);
        UUID projectId = project.getId();

        taskRepository.saveAndFlush(new Task(project, "Open Task", 0));
        Task completed = new Task(project, "Completed Task", 1);
        completed.complete(java.time.Instant.now());
        taskRepository.saveAndFlush(completed);
        Task deleted = new Task(project, "Deleted Completed Task", 2);
        deleted.complete(java.time.Instant.now());
        deleted.softDelete();
        taskRepository.saveAndFlush(deleted);

        Project otherProject = projectRepository.saveAndFlush(
                new Project(owner, client, "Other Project"));
        Task otherTask = new Task(otherProject, "Other Completed Task", 0);
        otherTask.complete(java.time.Instant.now());
        taskRepository.saveAndFlush(otherTask);

        entityManager.clear();
        var result = projectService.getById(owner.getId(), projectId);
        assertThat(result.getId()).isEqualTo(projectId);
        assertThat(result.getClient().getId()).isEqualTo(client.getId());
        assertThat(result.getClient().getName()).isEqualTo("Detail Client");
        assertThat(result.getTargetHours()).isEqualByComparingTo("36");
        assertThat(result.getTaskProgress().getTotalTasks()).isEqualTo(2);
        assertThat(result.getTaskProgress().getCompletedTasks()).isEqualTo(1);
        assertThat(result.getTaskProgress().getPercent()).isEqualByComparingTo("50");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThatThrownBy(() -> projectService.getById(java.util.UUID.randomUUID(), projectId))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
