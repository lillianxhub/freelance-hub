package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import th.ac.kku.freelance_hub.mapper.TimeEntryMapper;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
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
                new TimeEntryMapper()
        );
    }

    @Test
    void listsEntriesWithCombinedFiltersPaginationAndSorting() {
        TimeEntry entry = manualEntry(project, task);
        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.ASC, "durationMinutes")
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
                .page(1)
                .size(5)
                .sortBy("durationMinutes")
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
    void rejectsListWithInvalidPageOrSize() {
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(-1)
                .build();

        assertThatThrownBy(() -> queryService.list(OWNER_ID, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Time entry page size must be between 1 and 100");

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
        TimeEntry thirtyMinuteEntry = TimeEntry.createManualWithDuration(
                owner,
                project,
                null,
                "Long task",
                NOW.minusSeconds(60 * 60),
                30
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
        assertThat(response.getTotalMinutes()).isEqualTo(31);
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
        assertThat(response.getTotalMinutes()).isZero();
        assertThat(response.getFrom()).isNull();
        assertThat(response.getTo()).isNull();
    }

    @Test
    void summaryIgnoresPaginationAndSortingOptions() {
        when(timeEntryRepository.findAll(
                ArgumentMatchers.<Specification<TimeEntry>>any()
        )).thenReturn(List.of());
        TimeEntryFilterRequest filter = TimeEntryFilterRequest.builder()
                .page(-1)
                .size(0)
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

