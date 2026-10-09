package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.ReportService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportIntegrationTest {
    @Autowired private UserRepository users;
    @Autowired private ClientRepository clients;
    @Autowired private ProjectRepository projects;
    @Autowired private TimeEntryRepository entries;
    @Autowired private ReportService reports;

    @Test
    void summaryAndProjectsExcludeOtherOwnersEntries() {
        User owner = user("report-owner@example.com");
        User other = user("report-other@example.com");
        Project project = project(owner, "Website", 120);
        Project otherProject = project(other, "Private", 120);

        entry(owner, project, "2026-10-01T03:00:00Z", 3600);
        entry(other, otherProject, "2026-10-01T03:00:00Z", 7200);

        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-01"
        );

        var summary = reports.getSummary(owner.getId(), filter);
        var page = reports.getProjects(
                owner.getId(), filter, new ReportProjectsRequest()
        );

        assertThat(summary.summary().totalTrackedSeconds())
                .isEqualTo(3600);
        assertThat(summary.summary().timeEntryCount())
                .isEqualTo(1);
        assertThat(summary.summary().projectsWithTime())
                .isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).projectName())
                .isEqualTo("Website");
        assertThat(page.getContent().get(0).usagePercent())
                .isEqualByComparingTo("50.00");
    }

    @Test
    void filterByClientExcludesOtherClient() {
        User owner = user("report-filter@example.com");
        Project first = project(owner, "Website", 120);
        Project second = project(owner, "Mobile", 120);

        entry(owner, first, "2026-10-01T03:00:00Z", 3600);
        entry(owner, second, "2026-10-01T04:00:00Z", 1800);

        ReportFilterRequest filter = filter(
                "2026-10-01", "2026-10-01"
        );
        filter.setClientId(first.getClient().getId());

        var summary = reports.getSummary(owner.getId(), filter);
        var page = reports.getProjects(
                owner.getId(), filter, new ReportProjectsRequest()
        );

        assertThat(summary.summary().totalTrackedSeconds())
                .isEqualTo(3600);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).projectId())
                .isEqualTo(first.getId());
    }

    @Test
    void trendAndWorkPatternGroupEntriesInBangkokTime() {
        User owner = user("report-timezone@example.com");
        Project project = project(owner, "Timezone", 120);
        entry(owner, project, "2026-09-30T18:00:00Z", 1800);

        ReportFilterRequest filter = filter("2026-10-01", "2026-10-02");
        var trend = reports.getWorkTrend(owner.getId(), filter, ReportGranularity.DAY);
        var pattern = reports.getWorkPattern(owner.getId(), filter);

        assertThat(trend.points())
                .extracting(point -> point.trackedSeconds())
                .containsExactly(1800L, 0L);
        assertThat(pattern.mostActiveHour()).isEqualTo(1);
    }

    @Test
    void emptyDateFilterSummarizesAllRecordedTime() {
        User owner = user("report-all-time@example.com");
        Project project = project(owner, "All time", 120);
        entry(owner, project, "2025-10-01T03:00:00Z", 1800);
        entry(owner, project, "2026-10-01T03:00:00Z", 3600);

        ReportFilterRequest filter = new ReportFilterRequest();
        var summary = reports.getSummary(owner.getId(), filter);
        var distribution = reports.getDistribution(
                owner.getId(), filter,
                ReportGroupBy.CLIENT
        );
        var page = reports.getProjects(
                owner.getId(), filter, new ReportProjectsRequest()
        );

        assertThat(summary.summary().totalTrackedSeconds()).isEqualTo(5400);
        assertThat(summary.summary().trackedTimeTrendPercent()).isNull();
        assertThat(distribution.items().get(0).trackedSeconds()).isEqualTo(5400);
        assertThat(page.getContent().get(0).trackedSeconds()).isEqualTo(5400);
    }

    private User user(String email) {
        return users.saveAndFlush(
                User.builder()
                        .email(email)
                        .passwordHash("test-hash")
                        .build()
        );
    }

    private Project project(User owner, String name, int targetMinutes) {
        Client client = clients.saveAndFlush(
                new Client(owner, name + " Client")
        );
        Project project = new Project(owner, client, name);
        project.updateDetails(
                name, null, null, null, null, targetMinutes
        );
        project.changeStatus(ProjectStatus.ACTIVE);
        return projects.saveAndFlush(project);
    }

    private void entry(
            User owner,
            Project project,
            String startedAt,
            long durationSeconds
    ) {
        entries.saveAndFlush(
                TimeEntry.createManualWithDurationSeconds(
                        owner,
                        project,
                        null,
                        "Report test",
                        Instant.parse(startedAt),
                        durationSeconds
                )
        );
    }

    private ReportFilterRequest filter(String from, String to) {
        ReportFilterRequest filter = new ReportFilterRequest();
        filter.setFrom(LocalDate.parse(from));
        filter.setTo(LocalDate.parse(to));
        return filter;
    }
}
