package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.service.ClientService;
import th.ac.kku.freelance_hub.dto.response.client.ClientTimeTotalResponse;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void ownerScopedLookupAndPagesNeverReturnAnotherUsersClients() {
        User owner = saveUser("client-owner@example.com");
        User otherOwner = saveUser("other-client-owner@example.com");
        Client alpha = clientRepository.saveAndFlush(new Client(owner, "Alpha"));
        Client beta = clientRepository.saveAndFlush(new Client(owner, "Beta"));
        Client other = clientRepository.saveAndFlush(new Client(otherOwner, "Aardvark"));

        assertThat(clientRepository.findByIdAndOwnerId(alpha.getId(), owner.getId()))
            .contains(alpha);
        assertThat(clientRepository.findByIdAndOwnerId(other.getId(), owner.getId()))
            .isEmpty();
        assertThat(clientRepository.existsByIdAndOwnerId(beta.getId(), owner.getId()))
            .isTrue();
        assertThat(clientRepository.existsByIdAndOwnerId(other.getId(), owner.getId()))
            .isFalse();

        PageRequest firstPage = PageRequest.of(0, 1, Sort.by("name"));
        var pageZero = clientRepository.findAllByOwnerId(owner.getId(), firstPage);
        var pageOne = clientRepository.findAllByOwnerId(owner.getId(), firstPage.next());

        assertThat(pageZero.getTotalElements()).isEqualTo(2);
        assertThat(pageZero.getTotalPages()).isEqualTo(2);
        assertThat(pageZero.getContent()).extracting(Client::getId)
            .containsExactly(alpha.getId());
        assertThat(pageOne.getContent()).extracting(Client::getId)
            .containsExactly(beta.getId());
    }

    @Test
    void statusFilterKeepsArchivedClientsWithinTheirOwner() {
        User owner = saveUser("status-owner@example.com");
        User otherOwner = saveUser("other-status-owner@example.com");
        Client active = clientRepository.saveAndFlush(new Client(owner, "Active Client"));
        Client archived = new Client(owner, "Archived Client");
        archived.setActive(false);
        archived = clientRepository.saveAndFlush(archived);
        Client otherArchived = new Client(otherOwner, "Other Archived Client");
        otherArchived.setActive(false);
        clientRepository.saveAndFlush(otherArchived);

        PageRequest page = PageRequest.of(0, 10);
        assertThat(clientRepository.findAllByOwnerIdAndIsActive(
            owner.getId(), false, page
        ).getContent()).extracting(Client::getId).containsExactly(archived.getId());
        assertThat(clientRepository.findAllByOwnerIdAndIsActive(
            owner.getId(), true, page
        ).getContent()).extracting(Client::getId).containsExactly(active.getId());
    }

    @Test
    void timeTotalsGroupCompletedEntriesByClientWithinOwnerAndStartRange() {
        User owner = saveUser("time-summary-owner@example.com");
        User otherOwner = saveUser("time-summary-other@example.com");
        Client alpha = clientRepository.saveAndFlush(new Client(owner, "Alpha"));
        Client beta = clientRepository.saveAndFlush(new Client(owner, "Beta"));
        Client other = clientRepository.saveAndFlush(new Client(otherOwner, "Other"));
        Project alphaFirst = saveActiveProject(owner, alpha, "Alpha First");
        Project alphaSecond = saveActiveProject(owner, alpha, "Alpha Second");
        Project betaProject = saveActiveProject(owner, beta, "Beta Project");
        Project otherProject = saveActiveProject(otherOwner, other, "Other Project");
        Project archivedProject = saveActiveProject(owner, alpha, "Archived Project");
        Task deletedTask = taskRepository.saveAndFlush(new Task(alphaFirst, "Deleted Task", 0));
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        saveManualEntry(owner, alphaFirst, from, 3600);
        saveManualEntry(owner, alphaSecond, from.plusSeconds(60), 1800);
        saveManualEntry(owner, betaProject, from.plusSeconds(120), 7200);
        saveManualEntry(owner, alphaFirst, to, 9000); // Exclusive upper bound.
        saveManualEntry(otherOwner, otherProject, from, 12000);
        saveManualEntry(owner, archivedProject, from.plusSeconds(150), 4000);
        archivedProject.changeStatus(ProjectStatus.ARCHIVED);
        projectRepository.saveAndFlush(archivedProject);
        timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, alphaFirst, deletedTask, null, from.plusSeconds(160), 3000));
        deletedTask.softDelete();
        taskRepository.saveAndFlush(deletedTask);
        timeEntryRepository.saveAndFlush(TimeEntry.startTimer(
                owner, alphaFirst, null, null, from.plusSeconds(180)));
        TimeEntry deleted = saveManualEntry(owner, alphaFirst, from.plusSeconds(240), 5000);
        deleted.softDelete(from.plusSeconds(300));
        timeEntryRepository.saveAndFlush(deleted);

        var totals = clientService.summarizeTimeByClient(owner.getId(), from, to);

        assertThat(totals).extracting(ClientTimeTotalResponse::clientId)
                .containsExactly(beta.getId(), alpha.getId());
        assertThat(totals).extracting(ClientTimeTotalResponse::totalSeconds)
                .containsExactly(7200L, 5400L);
        assertThat(clientService.summarizeTimeByClient(otherOwner.getId(), from, to))
                .extracting(ClientTimeTotalResponse::clientId)
                .containsExactly(other.getId());
        assertThat(clientService.summarizeTimeByClient(owner.getId(), to.plusSeconds(1), to.plusSeconds(2)))
                .isEmpty();
        assertThatThrownBy(() -> clientService.summarizeTimeByClient(owner.getId(), to, from))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archivingClientArchivesOnlyItsNonDeletedProjectsAndPreservesHistory() {
        User owner = saveUser("cascade-owner@example.com");
        User otherOwner = saveUser("cascade-other-owner@example.com");
        Client client = clientRepository.saveAndFlush(new Client(owner, "Archived Client"));
        Client otherClient = clientRepository.saveAndFlush(new Client(owner, "Other Client"));
        Client foreignClient = clientRepository.saveAndFlush(new Client(otherOwner, "Foreign Client"));
        Project planned = projectRepository.saveAndFlush(new Project(owner, client, "Planned"));
        Project active = saveActiveProject(owner, client, "Active");
        Project onHold = saveActiveProject(owner, client, "On Hold");
        onHold.changeStatus(ProjectStatus.ON_HOLD);
        Project completed = saveActiveProject(owner, client, "Completed");
        completed.changeStatus(ProjectStatus.COMPLETED);
        Project archived = saveActiveProject(owner, client, "Already Archived");
        archived.changeStatus(ProjectStatus.ARCHIVED);
        Project deleted = saveActiveProject(owner, client, "Deleted");
        deleted.archive();
        entityManager.flush();
        entityManager.refresh(deleted);
        Instant deletedAt = deleted.getDeletedAt();
        Project otherProject = saveActiveProject(owner, otherClient, "Other Project");
        Project foreignProject = saveActiveProject(otherOwner, foreignClient, "Foreign Project");
        Task task = taskRepository.saveAndFlush(new Task(active, "Keep Task", 0));
        TimeEntry entry = saveManualEntry(owner, active, Instant.parse("2026-10-03T00:00:00Z"), 3600);

        clientService.changeStatus(owner.getId(), client.getId(), false);
        entityManager.flush();
        entityManager.clear();

        Client storedClient = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(storedClient.getIsActive()).isFalse();
        assertThat(storedClient.getDeletedAt()).isNull();
        for (Project project : List.of(planned, active, onHold, completed, archived)) {
            Project stored = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
            assertThat(stored.getIsActive()).isFalse();
            assertThat(stored.getDeletedAt()).isNull();
        }
        assertThat(projectRepository.findById(deleted.getId()).orElseThrow().getDeletedAt())
                .isEqualTo(deletedAt);
        for (Project project : List.of(otherProject, foreignProject)) {
            Project stored = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(stored.getIsActive()).isTrue();
            assertThat(stored.getDeletedAt()).isNull();
        }
        Task storedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(storedTask.getIsActive()).isTrue();
        assertThat(storedTask.getDeletedAt()).isNull();
        TimeEntry storedEntry = timeEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(storedEntry.getDurationSeconds()).isEqualTo(3600L);
        assertThat(storedEntry.getIsActive()).isTrue();
        assertThat(storedEntry.getDeletedAt()).isNull();
    }

    @Test
    void reactivatingClientDoesNotRestoreItsProjectsAndRepeatedArchiveIsAllowed() {
        User owner = saveUser("cascade-reactivate@example.com");
        Client client = clientRepository.saveAndFlush(new Client(owner, "Reactivate Client"));
        Project project = saveActiveProject(owner, client, "Keep Archived");

        clientService.changeStatus(owner.getId(), client.getId(), false);
        entityManager.flush();
        entityManager.clear();
        clientService.changeStatus(owner.getId(), client.getId(), false);
        clientService.changeStatus(owner.getId(), client.getId(), true);
        entityManager.flush();
        entityManager.clear();

        assertThat(clientRepository.findById(client.getId()).orElseThrow().getIsActive()).isTrue();
        Project stored = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(stored.getIsActive()).isFalse();
        assertThat(stored.getDeletedAt()).isNull();
    }

    @Test
    void anotherOwnerCannotArchiveClientOrItsProjects() {
        User owner = saveUser("cascade-protected@example.com");
        User otherOwner = saveUser("cascade-denied@example.com");
        Client client = clientRepository.saveAndFlush(new Client(owner, "Protected Client"));
        Project project = saveActiveProject(owner, client, "Protected Project");

        assertThatThrownBy(() -> clientService.changeStatus(otherOwner.getId(), client.getId(), false))
                .isInstanceOf(ClientNotFoundException.class);
        entityManager.flush();
        entityManager.clear();

        assertThat(clientRepository.findById(client.getId()).orElseThrow().getIsActive()).isTrue();
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectStatus.ACTIVE);
    }

    private Project saveActiveProject(User owner, Client client, String name) {
        Project project = new Project(owner, client, name);
        project.changeStatus(ProjectStatus.ACTIVE);
        return projectRepository.saveAndFlush(project);
    }

    private TimeEntry saveManualEntry(User owner, Project project, Instant startedAt, long seconds) {
        return timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, null, null, startedAt, seconds));
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
            .email(email)
            .passwordHash("test-hash")
            .build());
    }
}
