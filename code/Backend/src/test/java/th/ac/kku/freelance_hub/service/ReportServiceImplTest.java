package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.repository.ReportQueryRepository;
import th.ac.kku.freelance_hub.service.impl.ReportServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock private ReportQueryRepository queries;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                queries,
                Clock.fixed(
                        Instant.parse("2026-10-05T00:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void trendFillsDaysWithoutEntriesWithZero() {
        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-03"
        );

        when(queries.timeByDay(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(List.of(
                new ReportQueryRepository.DailyTime(
                        LocalDate.of(2026, 10, 2), 1800
                )
        ));

        var result = service.getWorkTrend(
                OWNER_ID, filter, ReportGranularity.DAY
        );

        assertThat(result.points())
                .extracting(point -> point.trackedSeconds())
                .containsExactly(0L, 1800L, 0L);
    }

    @Test
    void distributionCombinesProjectsOfSameClient() {
        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-05"
        );
        Project first = project("Website", UUID.randomUUID());
        Project second = project("Mobile", UUID.randomUUID());

        when(queries.findVisibleProjects(OWNER_ID))
                .thenReturn(List.of(first, second));
        when(queries.timeByProject(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(List.of(
                new ReportQueryRepository.ProjectTime(
                        first.getId(), 3600
                ),
                new ReportQueryRepository.ProjectTime(
                        second.getId(), 1800
                )
        ));

        var result = service.getDistribution(
                OWNER_ID, filter, ReportGroupBy.CLIENT
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(CLIENT_ID);
        assertThat(result.items().get(0).trackedSeconds())
                .isEqualTo(5400);
        assertThat(result.items().get(0).percent())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void projectReportUsesTrackedTimeAndCompletedTaskCount() {
        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-05"
        );
        Project project = project("Website", PROJECT_ID);
        project.updateDetails(
                "Website", null, null, null, null, 120
        );

        when(queries.findVisibleProjects(OWNER_ID))
                .thenReturn(List.of(project));
        when(queries.timeByProject(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(List.of(
                new ReportQueryRepository.ProjectTime(
                        PROJECT_ID, 3600
                )
        ));
        when(queries.taskProgress(OWNER_ID)).thenReturn(List.of(
                new ReportQueryRepository.TaskProgress(
                        PROJECT_ID, 4, 3
                )
        ));

        var result = service.getProjects(
                OWNER_ID, filter, new ReportProjectsRequest()
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        var row = result.getContent().get(0);
        assertThat(row.targetSeconds()).isEqualTo(7200);
        assertThat(row.trackedSeconds()).isEqualTo(3600);
        assertThat(row.usagePercent())
                .isEqualByComparingTo("50.00");
        assertThat(row.taskProgressPercent())
                .isEqualByComparingTo("75.00");
    }

    @Test
    void workPatternHasNoTopDayOrHourWithoutEntries() {
        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-03"
        );

        when(queries.timeByDay(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(List.of());
        when(queries.timeByHour(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(List.of());
        when(queries.totals(
                eq(OWNER_ID), any(), any(), isNull(), isNull()
        )).thenReturn(new ReportQueryRepository.Totals(0, 0));

        var result = service.getWorkPattern(OWNER_ID, filter);

        assertThat(result.mostProductiveDay()).isNull();
        assertThat(result.mostActiveHour()).isNull();
        assertThat(result.timeByWeekday()).hasSize(7);
        assertThat(result.timeByHour()).hasSize(24);
        assertThat(result.trackedTimeTrendPercent())
                .isEqualByComparingTo("0");
    }

    private ReportFilterRequest filter(String from, String to) {
        ReportFilterRequest filter = new ReportFilterRequest();
        filter.setFrom(LocalDate.parse(from));
        filter.setTo(LocalDate.parse(to));
        return filter;
    }

    private Project project(String name, UUID projectId) {
        User owner = User.builder().id(OWNER_ID).build();
        Client client = new Client(owner, "Acme");
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);

        Project project = new Project(owner, client, name);
        ReflectionTestUtils.setField(project, "id", projectId);
        return project;
    }
}
