package th.ac.kku.freelance_hub.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;

@SpringBootTest
@ActiveProfiles({"test", "local-seed"})
class LocalSeedServiceTest {

    @Autowired LocalSeedService seedService;
    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired TimeEntryRepository timeEntryRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void seedsCompleteDatasetAndRerunKeepsEditsAndDoesNotDuplicateRunningTimer() {
        LocalSeedService.SeedResult first = seedService.seed("local-seed-test@example.test", "LocalSeedTest123", true);
        entityManager.flush();
        entityManager.clear();

        User user = userRepository.findWithProfileById(first.userId()).orElseThrow();
        List<Client> clients = clientRepository.findAll().stream().filter(c -> c.getOwner().getId().equals(user.getId())).toList();
        List<Project> projects = projectRepository.findAll().stream().filter(p -> p.getOwner().getId().equals(user.getId())).toList();
        List<Task> tasks = taskRepository.findAll().stream().filter(t -> t.getProject().getOwner().getId().equals(user.getId())).toList();
        List<TimeEntry> entries = timeEntryRepository.findAll().stream().filter(t -> t.getOwner().getId().equals(user.getId())).toList();

        assertThat(clients).hasSize(12);
        assertThat(clients).filteredOn(c -> c.getStatus().name().equals("ACTIVE")).hasSize(11);
        assertThat(projects).hasSize(12);
        assertThat(projects).extracting(Project::getStatus).containsExactlyInAnyOrder(
                ProjectStatus.ACTIVE, ProjectStatus.ACTIVE, ProjectStatus.ACTIVE, ProjectStatus.ACTIVE,
                ProjectStatus.ACTIVE, ProjectStatus.ACTIVE, ProjectStatus.ACTIVE, ProjectStatus.PLANNED,
                ProjectStatus.PLANNED, ProjectStatus.ON_HOLD, ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED);
        assertThat(tasks).hasSize(16);
        assertThat(tasks).extracting(Task::getStatus).contains(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
        assertThat(tasks).filteredOn(t -> t.getProject().getName().endsWith("Website Redesign")).hasSize(12);
        assertThat(entries).hasSize(15);
        assertThat(entries).filteredOn(entry -> entry.getEntryType() == EntryType.MANUAL).hasSize(12);
        assertThat(entries).filteredOn(entry -> entry.getEntryType() == EntryType.TIMER && !entry.isRunning()).hasSize(2);
        assertThat(entries).filteredOn(entry -> entry.getTask() == null).isNotEmpty();
        assertThat(entries).filteredOn(entry -> entry.getTask() != null).isNotEmpty();
        assertThat(entries).filteredOn(TimeEntry::isRunning).hasSize(1);
        assertThat(user.getProfile().getAddress()).isNotBlank();
        assertThat(user.getProfile().getBio()).isNotBlank();

        Client edited = clients.stream().filter(c -> c.getName().endsWith("Mekong Creative Studio")).findFirst().orElseThrow();
        UUID editedId = edited.getId();
        edited.updateDetails("My renamed client", "User edited company", "me@example.test", "0801234567", "A user address", "0000000000000", "User notes");
        Client softDeleted = clients.stream().filter(c -> c.getName().endsWith("Northstar Learning")).findFirst().orElseThrow();
        UUID softDeletedId = softDeleted.getId();
        softDeleted.softDelete();
        UUID deletedEntryId = jdbcTemplate.queryForObject(
                "select entity_id from local_seed_records where owner_id = ? and kind = 'entry' and seed_key = 'entry-14'",
                (rs, row) -> rs.getObject(1, UUID.class), user.getId());
        entityManager.remove(entityManager.find(TimeEntry.class, deletedEntryId));
        entityManager.flush();

        LocalSeedService.SeedResult second = seedService.seed("local-seed-test@example.test", "LocalSeedTest123", true);
        entityManager.flush();
        entityManager.clear();

        assertThat(second.userId()).isEqualTo(first.userId());
        assertThat(clientRepository.findById(editedId).orElseThrow().getName()).isEqualTo("My renamed client");
        assertThat(clientRepository.findById(softDeletedId).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(clientRepository.findAll().stream().filter(c -> c.getOwner().getId().equals(user.getId()))).hasSize(12);
        assertThat(projectRepository.findAll().stream().filter(p -> p.getOwner().getId().equals(user.getId()))).hasSize(12);
        assertThat(taskRepository.findAll().stream().filter(t -> t.getProject().getOwner().getId().equals(user.getId()))).hasSize(16);
        assertThat(timeEntryRepository.findAll().stream().filter(t -> t.getOwner().getId().equals(user.getId()))).hasSize(15);
        UUID replacementEntryId = jdbcTemplate.queryForObject(
                "select entity_id from local_seed_records where owner_id = ? and kind = 'entry' and seed_key = 'entry-14'",
                (rs, row) -> rs.getObject(1, UUID.class), user.getId());
        assertThat(replacementEntryId).isNotEqualTo(deletedEntryId);
        assertThat(timeEntryRepository.findByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(user.getId(), EntryType.TIMER)).isPresent();
    }
}
