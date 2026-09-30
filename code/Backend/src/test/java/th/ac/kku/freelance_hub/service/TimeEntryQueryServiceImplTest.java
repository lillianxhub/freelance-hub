package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.service.impl.TimeEntryQueryServiceImpl;

@ExtendWith(MockitoExtension.class)
class TimeEntryQueryServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-26T08:30:00Z");

    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private ProjectRepository projectRepository;

    private TimeEntryQueryServiceImpl queryService;
    private User owner;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(OWNER_ID)
                .email("query-owner@example.com")
                .passwordHash("test-hash")
                .build();
        Client client = new Client(owner, "Query Client");
        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());
        project = new Project(owner, client, "Query Project");
        project.changeStatus(ProjectStatus.ACTIVE);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        task = new Task(project, "Query Task", 0);
        ReflectionTestUtils.setField(task, "id", TASK_ID);

        queryService = new TimeEntryQueryServiceImpl(
                timeEntryRepository,
                projectRepository,
                new TimeEntryMapper()
        );
    }

    @Test
    void getsOwnedEntryById() {
        TimeEntry entry = manualEntry(project, task);
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.of(entry));

        var response = queryService.getById(OWNER_ID, ENTRY_ID);

        assertThat(response.getId()).isEqualTo(ENTRY_ID);
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getTaskId()).isEqualTo(TASK_ID);
        verify(timeEntryRepository)
                .findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID);
    }

    @Test
    void rejectsMissingOrOtherOwnersEntryById() {
        when(timeEntryRepository.findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getById(OWNER_ID, ENTRY_ID))
                .isInstanceOf(TimeEntryNotFoundException.class);

        verify(timeEntryRepository)
                .findByIdAndOwnerIdAndIsActiveTrue(ENTRY_ID, OWNER_ID);
    }

    @Test
    void listsEntriesWithCombinedFiltersPaginationAndSorting() {
        TimeEntry entry = manualEntry(project, task);
        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.ASC, "durationSeconds")
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(entry), pageable, 6));
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .clientId(project.getClient().getId())
                .projectId(PROJECT_ID)
                .taskId(TASK_ID)
                .entryType(EntryType.MANUAL)
                .from(NOW.minusSeconds(60 * 60))
                .to(NOW.plusSeconds(60 * 60))
                .page(2)
                .limit(5)
                .sortBy("durationSeconds")
                .direction(Sort.Direction.ASC)
                .build();

        var result = queryService.list(OWNER_ID, filter);

        assertThat(result.getContent())
                .extracting(response -> response.getId())
                .containsExactly(ENTRY_ID);
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        );
    }

    @Test
    void returnsEmptyPageWhenNoEntriesMatchListFilters() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "startedAt")
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = queryService.list(
                OWNER_ID,
                TimeEntryFilterRequest.builder().build()
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void rejectsListWithInvalidPageOrLimit() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(0)
                .build();

        assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Time entry page must be at least 1 and limit must be between 1 and 100"
                );

        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                any(PageRequest.class)
        );
    }

    @Test
    void rejectsListWithUnsupportedSortField() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .sortBy("owner")
                .build();

        assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported time entry sort field: owner");
    }

    @Test
    void rejectsListWithoutSortDirection() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .direction(null)
                .build();

        assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Time entry sort direction is required");
    }

    @Test
    void rejectsListWithInvalidTimeRange() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .from(NOW)
                .to(NOW)
                .build();

        assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From time must be before to time");
    }

    @Test
    void summarizesCompletedEntriesAndExcludesRunningTimer() {
        Instant from = NOW.minusSeconds(24 * 60 * 60);
        Instant to = NOW.plusSeconds(1);
        TimeEntry oneMinuteEntry = manualEntry(project, task);
        TimeEntry thirtyMinuteEntry = TimeEntry.createManualWithDurationSeconds(
                owner,
                project,
                null,
                "Long task",
                NOW.minusSeconds(60 * 60),
                1800
        );
        TimeEntry runningTimer = TimeEntry.startTimer(
                owner,
                project,
                null,
                "Still running",
                NOW.minusSeconds(5 * 60)
        );
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of(
                oneMinuteEntry,
                thirtyMinuteEntry,
                runningTimer
        ));
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .projectId(PROJECT_ID)
                .from(from)
                .to(to)
                .build();

        var response = queryService.summarize(OWNER_ID, filter);

        assertThat(response.getFrom()).isEqualTo(from);
        assertThat(response.getTo()).isEqualTo(to);
        assertThat(response.getEntryCount()).isEqualTo(2);
        assertThat(response.getTotalSeconds()).isEqualTo(1860);
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
    }

    @Test
    void returnsZeroSummaryWhenNoCompletedEntriesMatch() {
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of());

        var response = queryService.summarize(
                OWNER_ID,
                TimeEntryFilterRequest.builder().build()
        );

        assertThat(response.getEntryCount()).isZero();
        assertThat(response.getTotalSeconds()).isZero();
        assertThat(response.getFrom()).isNull();
        assertThat(response.getTo()).isNull();
    }

    @Test
    void summaryIgnoresPaginationAndSortingOptions() {
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of());
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(0)
                .limit(0)
                .sortBy(null)
                .direction(null)
                .build();

        var response = queryService.summarize(OWNER_ID, filter);

        assertThat(response.getEntryCount()).isZero();
        verify(timeEntryRepository).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any(),
                any(PageRequest.class)
        );
    }

    @Test
    void rejectsSummaryWithInvalidTimeRange() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .from(NOW)
                .to(NOW)
                .build();

        assertThatThrownBy(() -> queryService.summarize(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From time must be before to time");

        verify(timeEntryRepository, never()).findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        );
    }

    @Test
    void convertsThaiDateBoundariesAndReturnsTotalSeconds() {
        LocalDate day = LocalDate.of(2026, 9, 29);
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-29T17:00:00Z");
        when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to,
                TimeEntryRepository.AllocationView.class
        )).thenReturn(List.of(durationEntry(from, 1800), durationEntry(from, 1800)));

        long total = queryService.sumCompletedSeconds(
                OWNER_ID, day, day.plusDays(1)
        );

        assertThat(total).isEqualTo(3600L);
        verify(timeEntryRepository).findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to,
                TimeEntryRepository.AllocationView.class
        );
    }

    @Test
    void groupsByThaiStartDateAndIncludesEmptyDays() {
        LocalDate fromDay = LocalDate.of(2026, 9, 29);
        LocalDate toDay = fromDay.plusDays(3);
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-10-01T17:00:00Z");
        when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to,
                TimeEntryRepository.AllocationView.class
        )).thenReturn(List.of(
                durationEntry(Instant.parse("2026-09-28T17:00:00Z"), 1800),
                durationEntry(Instant.parse("2026-09-29T16:59:00Z"), 900),
                durationEntry(Instant.parse("2026-09-30T17:00:00Z"), 600)
        ));

        var result = queryService.sumDailySeconds(OWNER_ID, fromDay, toDay);

        assertThat(result).containsExactly(
                new TimeEntryQueryService.DailySeconds(fromDay, 2700),
                new TimeEntryQueryService.DailySeconds(fromDay.plusDays(1), 0),
                new TimeEntryQueryService.DailySeconds(fromDay.plusDays(2), 600)
        );
    }

    @Test
    void returnsZeroForEveryDayWhenThereAreNoEntries() {
        LocalDate fromDay = LocalDate.of(2026, 9, 29);
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-30T17:00:00Z");
        when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to,
                TimeEntryRepository.AllocationView.class
        )).thenReturn(List.of());

        var result = queryService.sumDailySeconds(
                OWNER_ID, fromDay, fromDay.plusDays(2)
        );

        assertThat(result).containsExactly(
                new TimeEntryQueryService.DailySeconds(fromDay, 0),
                new TimeEntryQueryService.DailySeconds(fromDay.plusDays(1), 0)
        );
    }

    @Test
    void sumsByProjectAndIncludesProjectWithNoEntries() {
        LocalDate day = LocalDate.of(2026, 9, 29);
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-29T17:00:00Z");
        Project empty = new Project(owner, project.getClient(), "Empty Project");
        ReflectionTestUtils.setField(empty, "id", UUID.randomUUID());
        when(projectRepository.findAll(
                ArgumentMatchers.<Specification<Project>>any(),
                eq(Sort.by("name", "id"))
        )).thenReturn(List.of(empty, project));
        when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to, TimeEntryRepository.AllocationView.class
        )).thenReturn(List.of(
                durationEntry(from, 90),
                durationEntry(from, 30, false, true),
                durationEntry(from, 40, true, false)
        ));

        assertThat(queryService.sumSecondsByProject(OWNER_ID, day, day.plusDays(1)))
                .containsExactly(
                        new TimeEntryQueryService.ProjectSeconds(empty.getId(), empty.getName(), 0),
                        new TimeEntryQueryService.ProjectSeconds(PROJECT_ID, project.getName(), 90)
                );
    }

    @Test
    void excludesInactiveProjectsAndDeletedTasksFromTotalAndDaily() {
        LocalDate day = LocalDate.of(2026, 9, 29);
        Instant from = Instant.parse("2026-09-28T17:00:00Z");
        Instant to = Instant.parse("2026-09-29T17:00:00Z");
        when(timeEntryRepository.findByOwnerIdAndIsActiveTrueAndEndedAtIsNotNullAndDurationSecondsIsNotNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                OWNER_ID, from, to, TimeEntryRepository.AllocationView.class
        )).thenReturn(List.of(
                durationEntry(from, 90),
                durationEntry(from, 30, false, true),
                durationEntry(from, 40, true, false)
        ));

        assertThat(queryService.sumCompletedSeconds(OWNER_ID, day, day.plusDays(1)))
                .isEqualTo(90);
        assertThat(queryService.sumDailySeconds(OWNER_ID, day, day.plusDays(1)))
                .containsExactly(new TimeEntryQueryService.DailySeconds(day, 90));
    }

    @Test
    void rejectsEmptyOrReversedAnalyticsDateRange() {
        LocalDate day = LocalDate.of(2026, 9, 29);

        assertThatThrownBy(() -> queryService.sumCompletedSeconds(
                OWNER_ID, day, day
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queryService.sumCompletedSeconds(
                OWNER_ID, day, day.minusDays(1)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queryService.sumDailySeconds(
                OWNER_ID, day, day
        )).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(timeEntryRepository);
    }

    private static TimeEntryRepository.AllocationView durationEntry(
            Instant startedAt,
            long durationSeconds
    ) {
        return durationEntry(startedAt, durationSeconds, true, true);
    }

    private static TimeEntryRepository.AllocationView durationEntry(
            Instant startedAt, long durationSeconds,
            boolean projectActive, boolean taskActive
    ) {
        return new TimeEntryRepository.AllocationView() {
            @Override
            public Instant getStartedAt() {
                return startedAt;
            }

            @Override
            public Long getDurationSeconds() {
                return durationSeconds;
            }

            @Override
            public TimeEntryRepository.ProjectView getProject() {
                return new TimeEntryRepository.ProjectView() {
                    public UUID getId() { return PROJECT_ID; }
                    public Boolean getIsActive() { return projectActive; }
                };
            }

            @Override
            public TimeEntryRepository.TaskView getTask() {
                return new TimeEntryRepository.TaskView() {
                    public Boolean getIsActive() { return taskActive; }
                };
            }
        };
    }

    private TimeEntry manualEntry(Project entryProject, Task entryTask) {
        TimeEntry entry = TimeEntry.createManual(
                owner,
                entryProject,
                entryTask,
                "Original work",
                NOW.minusSeconds(2 * 60),
                NOW.minusSeconds(60)
        );
        ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
        return entry;
    }
}

