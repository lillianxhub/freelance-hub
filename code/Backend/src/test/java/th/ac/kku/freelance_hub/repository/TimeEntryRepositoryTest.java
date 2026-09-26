package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimeEntryRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsEntryOnlyForItsOwner() {
        User owner = saveUser("time-owner@example.com");
        User otherOwner = saveUser("other-time-owner@example.com");
        Project project = saveActiveProject(owner, "Owner Project");

        TimeEntry entry = saveManualEntry(
                owner,
                project,
                null,
                "Owned entry",
                Instant.parse("2026-09-25T08:00:00Z")
        );

        assertThat(timeEntryRepository.findByIdAndOwnerId(
                entry.getId(), owner.getId()
        )).contains(entry);
        assertThat(timeEntryRepository.findByIdAndOwnerId(
                entry.getId(), otherOwner.getId()
        )).isEmpty();
        assertThat(timeEntryRepository.existsByIdAndOwnerId(
                entry.getId(), owner.getId()
        )).isTrue();
        assertThat(timeEntryRepository.existsByIdAndOwnerId(
                entry.getId(), otherOwner.getId()
        )).isFalse();
    }

    @Test
    void findsAndLocksRunningTimerForItsOwner() {
        User owner = saveUser("running-owner@example.com");
        User otherOwner = saveUser("other-running-owner@example.com");
        Project project = saveActiveProject(owner, "Running Project");
        Project otherProject = saveActiveProject(
                otherOwner,
                "Other Running Project"
        );

        TimeEntry running = timeEntryRepository.saveAndFlush(TimeEntry.startTimer(
                owner,
                project,
                null,
                "Current timer",
                Instant.parse("2026-09-25T08:00:00Z")
        ));
        timeEntryRepository.saveAndFlush(TimeEntry.startTimer(
                otherOwner,
                otherProject,
                null,
                "Other timer",
                Instant.parse("2026-09-25T09:00:00Z")
        ));

        assertThat(timeEntryRepository
                .findByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        owner.getId(), EntryType.TIMER
                )).contains(running);
        assertThat(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        owner.getId(), EntryType.TIMER
                )).isTrue();

        entityManager.clear();

        TimeEntry locked = timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNull(
                        owner.getId(), EntryType.TIMER
                )
                .orElseThrow();

        assertThat(locked.getId()).isEqualTo(running.getId());
        assertThat(entityManager.getLockMode(locked))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void listsOnlyOwnerEntriesWithPagination() {
        User owner = saveUser("list-owner@example.com");
        User otherOwner = saveUser("other-list-owner@example.com");
        Project project = saveActiveProject(owner, "List Project");
        Project otherProject = saveActiveProject(otherOwner, "Other List Project");

        TimeEntry first = saveManualEntry(
                owner,
                project,
                null,
                "First",
                Instant.parse("2026-09-25T08:00:00Z")
        );
        saveManualEntry(
                owner,
                project,
                null,
                "Second",
                Instant.parse("2026-09-25T09:00:00Z")
        );
        saveManualEntry(
                otherOwner,
                otherProject,
                null,
                "Other owner",
                Instant.parse("2026-09-25T07:00:00Z")
        );

        Page<TimeEntry> page = timeEntryRepository.findAllByOwnerId(
                owner.getId(),
                PageRequest.of(0, 1, Sort.by("startedAt").ascending())
        );

        assertThat(page.getContent())
                .extracting(TimeEntry::getId)
                .containsExactly(first.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void filtersEntriesByProjectAndTask() {
        User owner = saveUser("filter-owner@example.com");
        Project firstProject = saveActiveProject(owner, "First Project");
        Project secondProject = saveActiveProject(owner, "Second Project");
        Task firstTask = taskRepository.saveAndFlush(
                new Task(firstProject, "First Task", 0)
        );
        Task secondTask = taskRepository.saveAndFlush(
                new Task(secondProject, "Second Task", 0)
        );

        TimeEntry firstTaskEntry = saveManualEntry(
                owner,
                firstProject,
                firstTask,
                "First task entry",
                Instant.parse("2026-09-25T08:00:00Z")
        );
        TimeEntry noTaskEntry = saveManualEntry(
                owner,
                firstProject,
                null,
                "No task entry",
                Instant.parse("2026-09-25T09:00:00Z")
        );
        saveManualEntry(
                owner,
                secondProject,
                secondTask,
                "Second task entry",
                Instant.parse("2026-09-25T10:00:00Z")
        );

        PageRequest page = PageRequest.of(0, 10);

        assertThat(timeEntryRepository.findAllByOwnerIdAndProjectId(
                owner.getId(), firstProject.getId(), page
        ).getContent())
                .extracting(TimeEntry::getId)
                .containsExactlyInAnyOrder(
                        firstTaskEntry.getId(), noTaskEntry.getId()
                );
        assertThat(timeEntryRepository.findAllByOwnerIdAndTaskId(
                owner.getId(), firstTask.getId(), page
        ).getContent())
                .extracting(TimeEntry::getId)
                .containsExactly(firstTaskEntry.getId());
    }

    @Test
    void usesHalfOpenStartedAtRange() {
        User owner = saveUser("range-owner@example.com");
        Project project = saveActiveProject(owner, "Range Project");
        Instant rangeStart = Instant.parse("2026-09-25T08:00:00Z");
        Instant rangeEnd = Instant.parse("2026-09-25T10:00:00Z");

        TimeEntry atStart = saveManualEntry(
                owner, project, null, "At start", rangeStart
        );
        TimeEntry inside = saveManualEntry(
                owner,
                project,
                null,
                "Inside",
                Instant.parse("2026-09-25T09:59:00Z")
        );
        saveManualEntry(owner, project, null, "At end", rangeEnd);

        Page<TimeEntry> result = timeEntryRepository
                .findAllByOwnerIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        owner.getId(),
                        rangeStart,
                        rangeEnd,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .extracting(TimeEntry::getId)
                .containsExactlyInAnyOrder(atStart.getId(), inside.getId());
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .build());
    }

    private Project saveActiveProject(User owner, String name) {
        Client client = clientRepository.saveAndFlush(
                new Client(owner, name + " Client")
        );
        Project project = new Project(owner, client, name);
        project.changeStatus(ProjectStatus.ACTIVE);
        return projectRepository.saveAndFlush(project);
    }

    private TimeEntry saveManualEntry(
            User owner,
            Project project,
            Task task,
            String description,
            Instant startedAt
    ) {
        return timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDuration(
                owner,
                project,
                task,
                description,
                startedAt,
                30
        ));
    }
}
