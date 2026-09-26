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
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

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
}