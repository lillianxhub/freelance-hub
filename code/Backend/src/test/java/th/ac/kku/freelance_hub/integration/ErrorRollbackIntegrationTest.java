package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import th.ac.kku.freelance_hub.domain.entity.*;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.InvalidStateException;
import th.ac.kku.freelance_hub.repository.*;
import th.ac.kku.freelance_hub.service.ClientService;

/** Uses separate committed transactions to verify state after the service rolls back. */
@SpringBootTest
@ActiveProfiles("test")
class ErrorRollbackIntegrationTest {
    @Autowired PlatformTransactionManager transactions;
    @Autowired UserRepository users;
    @Autowired ClientRepository clients;
    @Autowired ProjectRepository projects;
    @Autowired TimeEntryRepository entries;
    @Autowired ClientService service;

    @Test
    void rejectingArchiveDuringTimerLeavesClientProjectsAndTimerUnchanged() {
        var tx = new TransactionTemplate(transactions);
        UUID[] ids = tx.execute(status -> {
            User user = users.saveAndFlush(User.builder().email("rollback-" + UUID.randomUUID() + "@example.com").passwordHash("hash").build());
            Client client = clients.saveAndFlush(new Client(user, "Rollback client"));
            Project project = new Project(user, client, "Running project");
            project.changeStatus(ProjectStatus.ACTIVE);
            projects.saveAndFlush(project);
            Project second = projects.saveAndFlush(new Project(user, client, "Planned project"));
            TimeEntry timer = entries.saveAndFlush(TimeEntry.startTimer(user, project, null, null, Instant.now()));
            return new UUID[] { user.getId(), client.getId(), project.getId(), second.getId(), timer.getId() };
        });
        assertThat(ids).isNotNull();
        try {
            assertThatThrownBy(() -> service.changeStatus(ids[0], ids[1], false))
                    .isInstanceOfSatisfying(InvalidStateException.class, ex -> {
                        assertThat(ex.getStatus().value()).isEqualTo(409);
                        assertThat(ex.getCode()).isEqualTo("INVALID_STATE");
                    });
            tx.executeWithoutResult(status -> {
                assertThat(clients.findById(ids[1]).orElseThrow().getIsActive()).isTrue();
                assertThat(projects.findById(ids[2]).orElseThrow().getStatus()).isEqualTo(ProjectStatus.ACTIVE);
                assertThat(projects.findById(ids[3]).orElseThrow().getStatus()).isEqualTo(ProjectStatus.PLANNED);
                assertThat(entries.findById(ids[4]).orElseThrow().isRunning()).isTrue();
            });
        } finally {
            tx.executeWithoutResult(status -> {
                entries.deleteById(ids[4]);
                entries.flush();
                projects.deleteById(ids[2]);
                projects.deleteById(ids[3]);
                projects.flush();
                clients.deleteById(ids[1]);
                clients.flush();
                users.deleteById(ids[0]);
            });
        }
    }
}
