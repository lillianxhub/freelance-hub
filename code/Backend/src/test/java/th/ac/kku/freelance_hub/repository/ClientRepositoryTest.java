package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

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
import th.ac.kku.freelance_hub.dto.response.ClientTimeTotalResponse;
import th.ac.kku.freelance_hub.service.ClientService;

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
