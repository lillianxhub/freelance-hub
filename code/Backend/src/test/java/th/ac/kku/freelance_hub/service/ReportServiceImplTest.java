package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.mapper.ReportMapper;
import th.ac.kku.freelance_hub.repository.ReportQueryRepository;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.service.impl.ReportServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    @Mock private ReportQueryRepository repository;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(repository,
                Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC), new ReportMapper());
    }

    @Test
    void trendFillsDaysWithoutEntriesWithZero() {
        sourceProjects(project("Website", PROJECT_ID));
        datedEntries(entry(PROJECT_ID, "2026-10-02T03:00:00Z", 1800));
        assertThat(service.getWorkTrend(OWNER_ID, filter("2026-10-01", "2026-10-03"), ReportGranularity.DAY).points())
                .extracting(point -> point.trackedSeconds()).containsExactly(0L, 1800L, 0L);
    }

    @Test
    void distributionCombinesProjectsOfSameClient() {
        Project first = project("Website", PROJECT_ID);
        Project second = project("Mobile", UUID.randomUUID());
        sourceProjects(first, second);
        datedEntries(entry(first.getId(), "2026-10-01T03:00:00Z", 2400),
                entry(first.getId(), "2026-10-01T04:00:00Z", 1200),
                entry(second.getId(), "2026-10-01T05:00:00Z", 1800));
        var result = service.getDistribution(OWNER_ID, filter("2026-10-01", "2026-10-05"), ReportGroupBy.CLIENT);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(CLIENT_ID);
        assertThat(result.items().get(0).trackedSeconds()).isEqualTo(5400);
        assertThat(result.items().get(0).percent()).isEqualByComparingTo("100.00");
        verify(repository).findProjects(OWNER_ID);
    }

    @Test
    void projectReportUsesTrackedTimeAndCompletedTaskCount() {
        Project project = project("Website", PROJECT_ID);
        project.updateDetails("Website", null, null, null, "#4F6BFF", 120);
        sourceProjects(project);
        datedEntries(entry(PROJECT_ID, "2026-10-01T03:00:00Z", 3600));
        var taskRows = List.of(task(TaskStatus.COMPLETED), task(TaskStatus.COMPLETED),
                task(TaskStatus.COMPLETED), task(TaskStatus.OPEN));
        when(repository.findTasks(eq(OWNER_ID), any()))
                .thenReturn(taskRows);
        var row = service.getProjects(OWNER_ID, filter("2026-10-01", "2026-10-05"), new ReportProjectsRequest()).getContent().get(0);
        assertThat(row.targetSeconds()).isEqualTo(7200);
        assertThat(row.trackedSeconds()).isEqualTo(3600);
        assertThat(row.usagePercent()).isEqualByComparingTo("50.00");
        assertThat(row.taskProgressPercent()).isEqualByComparingTo("75.00");
    }

    @Test
    void workPatternHasNoTopDayOrHourWithoutEntries() {
        var result = service.getWorkPattern(OWNER_ID, filter("2026-10-01", "2026-10-03"));
        assertThat(result.mostProductiveDay()).isNull();
        assertThat(result.mostActiveHour()).isNull();
        assertThat(result.timeByWeekday()).hasSize(7);
        assertThat(result.timeByHour()).hasSize(24);
        assertThat(result.trackedTimeTrendPercent()).isNull();
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any());
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any(), any(), any());
    }

    @Test
    void invalidDateRangeFailsBeforeReadingDatabase() {
        assertThatThrownBy(() -> service.getSummary(OWNER_ID, filter("2026-10-05", "2026-10-01")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void trendWithoutDatesIsRejected() {
        assertThatThrownBy(() -> service.getWorkTrend(OWNER_ID, new ReportFilterRequest(), ReportGranularity.DAY))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void missingProjectUsesDomainException() {
        var filter = new ReportFilterRequest();
        filter.setProjectId(PROJECT_ID);
        assertThatThrownBy(() -> service.getDistribution(OWNER_ID, filter, ReportGroupBy.PROJECT))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any());
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any(), any(), any());
    }

    @Test
    void selectedStatusExcludesArchivedProjectsBeforeFetchingEntries() {
        Project archived = project("Archived", PROJECT_ID);
        ReflectionTestUtils.setField(archived, "status", ProjectStatus.ARCHIVED);
        sourceProjects(archived);
        var filter = new ReportFilterRequest();
        filter.setStatus(ProjectStatus.ACTIVE);
        assertThat(service.getDistribution(OWNER_ID, filter, ReportGroupBy.PROJECT).items()).isEmpty();
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any());
        verify(repository, org.mockito.Mockito.never()).findEntries(any(), any(), any(), any());
    }

    @Test
    void rejectsExcessiveDailyPointsBeforeAggregation() {
        assertThatThrownBy(() -> service.getWorkTrend(OWNER_ID, filter("2020-01-01", "2026-01-01"), ReportGranularity.DAY))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    private void sourceProjects(Project... values) {
        when(repository.findProjects(OWNER_ID)).thenReturn(List.of(values));
    }

    private void datedEntries(TimeEntry... values) {
        when(repository.findEntries(
                eq(OWNER_ID), any(), any(), any())).thenReturn(List.of(values));
    }

    private TimeEntry entry(UUID id, String start, long seconds) {
        var ref = mock(Project.class);
        when(ref.getId()).thenReturn(id);
        var row = mock(TimeEntry.class);
        when(row.getProject()).thenReturn(ref);
        when(row.getStartedAt()).thenReturn(Instant.parse(start));
        when(row.getDurationSeconds()).thenReturn(seconds);
        return row;
    }

    private Task task(TaskStatus status) {
        var ref = mock(Project.class);
        when(ref.getId()).thenReturn(PROJECT_ID);
        var row = mock(Task.class);
        when(row.getProject()).thenReturn(ref);
        when(row.getStatus()).thenReturn(status);
        return row;
    }

    private ReportFilterRequest filter(String from, String to) {
        var filter = new ReportFilterRequest();
        filter.setFrom(LocalDate.parse(from));
        filter.setTo(LocalDate.parse(to));
        return filter;
    }

    private Project project(String name, UUID id) {
        var owner = User.builder().id(OWNER_ID).build();
        var client = new Client(owner, "Acme");
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
        var project = new Project(owner, client, name);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }
}
