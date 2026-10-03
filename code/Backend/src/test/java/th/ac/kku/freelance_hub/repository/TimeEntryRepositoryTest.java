package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.IllegalTransactionStateException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.service.TimeEntryService;

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
    private TimeEntryQueryService timeEntryQueryService;

    @Autowired
    private TimeEntryService timeEntryService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void locksProjectEntriesIncludingDeletedEntriesAndPreservesExistingLocks() {
        User owner = saveUser("project-lock-owner@example.com");
        User otherOwner = saveUser("project-lock-other@example.com");
        Project project = saveActiveProject(owner, "Lock Target");
        Project otherProject = saveActiveProject(owner, "Other Project");
        Project otherAccountProject = saveActiveProject(otherOwner, "Other Account");
        Instant start = Instant.parse("2026-10-03T08:00:00Z");
        TimeEntry manual = saveManualEntry(owner, project, null, "Manual", start);
        TimeEntry deleted = saveManualEntry(owner, project, null, "Deleted", start);
        deleted.softDelete(start.plusSeconds(3600));
        TimeEntry existing = saveManualEntry(owner, project, null, "Locked", start);
        Instant originalLock = start.plusSeconds(4000);
        existing.lock(originalLock);
        TimeEntry timer = TimeEntry.startTimer(owner, project, null, "Timer", start);
        timer.stop(start.plusSeconds(90));
        timeEntryRepository.saveAndFlush(timer);
        TimeEntry unrelated = saveManualEntry(owner, otherProject, null, "Other", start);
        TimeEntry otherAccount = saveManualEntry(
                otherOwner, otherAccountProject, null, "Other account", start
        );
        timeEntryRepository.flush();
        entityManager.clear();

        assertThat(timeEntryRepository.findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
                otherOwner.getId(), project.getId()
        )).isEmpty();
        var candidates = timeEntryRepository.findLockedByOwnerIdAndProjectIdAndLockedAtIsNull(
                owner.getId(), project.getId()
        );
        assertThat(candidates).extracting(TimeEntry::getId)
                .containsExactlyInAnyOrder(manual.getId(), deleted.getId(), timer.getId());
        assertThat(candidates).allSatisfy(entry ->
                assertThat(entityManager.getLockMode(entry))
                        .isEqualTo(LockModeType.PESSIMISTIC_WRITE));

        timeEntryService.lockByProject(owner.getId(), project.getId());
        entityManager.clear();

        Instant lockTime = timeEntryRepository.findById(manual.getId()).orElseThrow().getLockedAt();
        assertThat(lockTime).isNotNull();
        assertThat(timeEntryRepository.findById(deleted.getId()).orElseThrow().getLockedAt())
                .isEqualTo(lockTime);
        assertThat(timeEntryRepository.findById(timer.getId()).orElseThrow().getLockedAt())
                .isEqualTo(lockTime);
        assertThat(timeEntryRepository.findById(existing.getId()).orElseThrow().getLockedAt())
                .isEqualTo(originalLock);
        assertThat(timeEntryRepository.findById(unrelated.getId()).orElseThrow().getLockedAt())
                .isNull();
        assertThat(timeEntryRepository.findById(otherAccount.getId()).orElseThrow().getLockedAt())
                .isNull();

        timeEntryService.lockByProject(owner.getId(), project.getId());
        entityManager.clear();
        assertThat(timeEntryRepository.findById(manual.getId()).orElseThrow().getLockedAt())
                .isEqualTo(lockTime);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void projectLockRequiresTheCallersTransaction() {
        assertThatThrownBy(() -> timeEntryService.lockByProject(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID()
        )).isInstanceOf(IllegalTransactionStateException.class);
    }

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

        assertThat(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(
                entry.getId(), owner.getId()
        )).contains(entry);
        assertThat(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(
                entry.getId(), otherOwner.getId()
        )).isEmpty();
        assertThat(timeEntryRepository.existsByIdAndOwnerIdAndIsActiveTrue(
                entry.getId(), owner.getId()
        )).isTrue();
        assertThat(timeEntryRepository.existsByIdAndOwnerIdAndIsActiveTrue(
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
                .findByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
                        owner.getId(), EntryType.TIMER
                )).contains(running);
        assertThat(timeEntryRepository
                .existsByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
                        owner.getId(), EntryType.TIMER
                )).isTrue();

        entityManager.clear();

        TimeEntry locked = timeEntryRepository
                .findLockedByOwnerIdAndEntryTypeAndEndedAtIsNullAndIsActiveTrue(
                        owner.getId(), EntryType.TIMER
                )
                .orElseThrow();

        assertThat(locked.getId()).isEqualTo(running.getId());
        assertThat(entityManager.getLockMode(locked))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void excludesSoftDeletedEntryFromActiveLookup() {
        User owner = saveUser("deleted-entry-owner@example.com");
        Project project = saveActiveProject(owner, "Deleted Entry Project");
        TimeEntry entry = saveManualEntry(
                owner,
                project,
                null,
                "Deleted entry",
                Instant.parse("2026-09-25T08:00:00Z")
        );

        entry.softDelete(Instant.parse("2026-09-26T08:00:00Z"));
        timeEntryRepository.flush();
        entityManager.clear();

        assertThat(timeEntryRepository.findById(entry.getId()))
                .isPresent();
        assertThat(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(
                entry.getId(),
                owner.getId()
        )).isEmpty();
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

    @Test
    void findsOnlyOwnedCompletedActiveDurationsByHalfOpenStartRange() {
        User owner = saveUser("analytics-owner@example.com");
        User otherOwner = saveUser("analytics-other@example.com");
        Project project = saveActiveProject(owner, "Analytics Project");
        Project otherProject = saveActiveProject(otherOwner, "Other Analytics Project");
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-29T17:00:00Z");

        // Starts before the Thai day and ends after midnight: count by start.
        saveManualEntry(owner, project, null, "Before day", from.minusSeconds(60));
        saveManualEntry(owner, project, null, "At start", from);
        saveManualEntry(owner, project, null, "Inside", to.minusSeconds(60));
        saveManualEntry(owner, project, null, "At end", to);
        saveManualEntry(otherOwner, otherProject, null, "Other owner", from);

        TimeEntry deleted = saveManualEntry(
                owner, project, null, "Deleted", from.plusSeconds(60)
        );
        deleted.softDelete(Instant.parse("2026-09-30T00:00:00Z"));
        timeEntryRepository.saveAndFlush(TimeEntry.startTimer(
                owner, project, null, "Still running", from.plusSeconds(120)
        ));
        timeEntryRepository.flush();

        assertThat(timeEntryRepository
                .findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        owner.getId(), from, to,
                        TimeEntryRepository.StartedAtDurationView.class
                ))
                .extracting(TimeEntryRepository.StartedAtDurationView::getStartedAt)
                .containsExactlyInAnyOrder(from, to.minusSeconds(60));
        assertThat(timeEntryRepository
                .findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        owner.getId(), to.plusSeconds(3600), to.plusSeconds(7200),
                        TimeEntryRepository.DurationSecondsView.class
                )).isEmpty();
    }

    @Test
    void projectsRelatedVisibilityForProjectTotalsIncludingUnassignedEntries() {
        User owner = saveUser("project-totals-owner@example.com");
        Project project = saveActiveProject(owner, "Totals Project");
        Task task = taskRepository.saveAndFlush(new Task(project, "Totals Task", 0));
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-29T17:00:00Z");
        saveManualEntry(owner, project, task, "Assigned", from);
        saveManualEntry(owner, project, null, "Unassigned", from.plusSeconds(60));
        entityManager.clear();

        var entries = timeEntryRepository
                .findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        owner.getId(), from, to,
                        TimeEntryRepository.AllocationView.class
                );

        assertThat(entries).hasSize(2);
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.getProject().getId()).isEqualTo(project.getId());
            assertThat(entry.getProject().getIsActive()).isTrue();
        });
        assertThat(entries).anySatisfy(entry -> assertThat(entry.getTask()).isNull());
        assertThat(entries).anySatisfy(entry -> {
            assertThat(entry.getTask()).isNotNull();
            assertThat(entry.getTask().getIsActive()).isTrue();
        });
    }

    @Test
    void sumsVisibleProjectsWithZeroRowsAndExcludesArchivedAndDeletedTaskTime() {
        User owner = saveUser("totals-owner@example.com");
        User otherOwner = saveUser("other-totals-owner@example.com");
        Project project = saveActiveProject(owner, "A Project");
        Project emptyProject = saveActiveProject(owner, "B Empty Project");
        Project archivedProject = saveActiveProject(owner, "C Archived Project");
        Project otherProject = saveActiveProject(otherOwner, "Other Project");
        Task task = taskRepository.saveAndFlush(new Task(project, "Task", 0));
        Task deletedTask = taskRepository.saveAndFlush(new Task(project, "Deleted Task", 1));
        Instant start = Instant.parse("2026-09-28T17:00:00Z");
        saveManualEntry(owner, project, task, "Assigned", start);
        saveManualEntry(owner, project, null, "Unassigned", start.plusSeconds(60));
        saveManualEntry(owner, project, deletedTask, "Deleted task", start.plusSeconds(120));
        saveManualEntry(owner, archivedProject, null, "Archived", start.plusSeconds(180));
        saveManualEntry(otherOwner, otherProject, null, "Other owner", start);
        deletedTask.softDelete();
        archivedProject.archive();
        taskRepository.flush();
        projectRepository.flush();
        entityManager.clear();

        var result = timeEntryQueryService.sumSecondsByProject(
                owner.getId(), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30)
        );

        assertThat(result).containsExactly(
                new TimeEntryQueryService.ProjectSeconds(project.getId(), project.getName(), 3600),
                new TimeEntryQueryService.ProjectSeconds(emptyProject.getId(), emptyProject.getName(), 0)
        );
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
        return timeEntryRepository.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner,
                project,
                task,
                description,
                startedAt,
                1800
        ));
    }
}
