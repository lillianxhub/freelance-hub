package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.User;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void findsOnlyOwnedTasksAndOrdersThemBySortOrder() {
        User owner = userRepository.saveAndFlush(User.builder()
                .email("task-owner@example.com")
                .passwordHash("test-hash")
                .build());

        User otherOwner = userRepository.saveAndFlush(User.builder()
                .email("other-task-owner@example.com")
                .passwordHash("test-hash")
                .build());

        Client client = clientRepository.saveAndFlush(
                new Client(owner, "My Client"));
        Client otherClient = clientRepository.saveAndFlush(
                new Client(otherOwner, "Other Client"));

        Project project = projectRepository.saveAndFlush(
                new Project(owner, client, "My Project"));
        Project otherProject = projectRepository.saveAndFlush(
                new Project(otherOwner, otherClient, "Other Project"));

        // บันทึกลำดับ 1 ก่อน เพื่อพิสูจน์ว่า query เรียงตาม sortOrder
        Task second = taskRepository.saveAndFlush(
                new Task(project, "Second Task", 1));
        Task first = taskRepository.saveAndFlush(
                new Task(project, "First Task", 0));
        Task otherTask = taskRepository.saveAndFlush(
                new Task(otherProject, "Other Task", 0));

        assertThat(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                first.getId(), project.getId(), owner.getId()
        )).isPresent();

        assertThat(taskRepository.findByIdAndProjectIdAndProjectOwnerId(
                otherTask.getId(), otherProject.getId(), owner.getId()
        )).isEmpty();

        assertThat(taskRepository.findAllByProjectIdAndProjectOwnerId(
                project.getId(), owner.getId(), PageRequest.of(0, 10)
        ).getContent())
                .extracting(Task::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());

        assertThat(taskRepository
                .findAllByProjectIdAndProjectOwnerIdOrderBySortOrderAsc(
                        project.getId(), owner.getId()
                ))
                .extracting(Task::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(taskRepository.countByProjectId(project.getId()))
                .isEqualTo(2);
    }
}