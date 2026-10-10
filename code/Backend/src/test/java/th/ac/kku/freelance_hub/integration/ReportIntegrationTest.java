package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.repository.TaskRepository;
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
    @Autowired private TaskRepository tasks;

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

    @Test
    void statusFilterAppliesToSummaryDistributionAndProjectPage() {
        User owner = user("report-status@example.com");
        Project active = project(owner, "Active", 120);
        Project archived = project(owner, "Archived", 120);
        active.changeStatus(ProjectStatus.ACTIVE);
        archived.changeStatus(ProjectStatus.ARCHIVED);
        projects.saveAndFlush(active);
        projects.saveAndFlush(archived);
        entry(owner, active, "2026-10-01T03:00:00Z", 1800);
        // Create historical entries while the project is editable, then archive it.
        archived.changeStatus(ProjectStatus.ACTIVE);
        entry(owner, archived, "2026-10-01T03:00:00Z", 7200);
        archived.changeStatus(ProjectStatus.ARCHIVED);
        projects.saveAndFlush(archived);
        var filter = new ReportFilterRequest();
        filter.setStatus(ProjectStatus.ACTIVE);
        assertThat(reports.getSummary(owner.getId(), filter).summary().totalTrackedSeconds()).isEqualTo(1800);
        assertThat(reports.getDistribution(owner.getId(), filter, ReportGroupBy.PROJECT).items())
                .extracting(item -> item.id()).containsExactly(active.getId());
        assertThat(reports.getProjects(owner.getId(), filter, new ReportProjectsRequest()).getContent())
                .extracting(item -> item.projectId()).containsExactly(active.getId());
        var trend = reports.getWorkTrend(owner.getId(), datedStatus(filter), ReportGranularity.DAY);
        assertThat(trend.points().get(0).trackedSeconds()).isEqualTo(1800);
        assertThat(reports.getWorkPattern(owner.getId(), filter).timeByHour())
                .extracting(item -> item.trackedSeconds()).contains(1800L);
    }

    @Test
    void servicePaginationSortsBeforeSelectingPageAndKeepsTotal() {
        User owner = user("report-pages@example.com");
        Project low = project(owner, "Low", 120);
        Project high = project(owner, "High", 120);
        Project medium = project(owner, "Medium", 120);
        entry(owner, low, "2026-10-01T03:00:00Z", 600);
        entry(owner, high, "2026-10-01T03:00:00Z", 3600);
        entry(owner, medium, "2026-10-01T03:00:00Z", 1800);
        var request = new ReportProjectsRequest();
        request.setLimit(1);
        request.setPage(2);
        request.setSortBy("trackedSeconds");
        request.setDirection("desc");
        var page = reports.getProjects(owner.getId(), new ReportFilterRequest(), request);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).extracting(row -> row.projectId()).containsExactly(medium.getId());
        request.setPage(4);
        var empty = reports.getProjects(owner.getId(), new ReportFilterRequest(), request);
        assertThat(empty.getContent()).isEmpty();
        assertThat(empty.getTotalElements()).isEqualTo(3);
    }

    @Test
    void taskJoinDoesNotMultiplyTrackedTimeAndNullTargetsSortLast() {
        User owner = user("report-join@example.com");
        Project targeted = project(owner, "Targeted", 120);
        Project untargeted = project(owner, "Untargeted", 120);
        untargeted.updateDetails("Untargeted", null, null, null, null, null);
        projects.saveAndFlush(untargeted);
        var first = new Task(targeted, "One", 0);
        first.complete(Instant.parse("2026-10-01T04:00:00Z"));
        tasks.saveAndFlush(first);
        tasks.saveAndFlush(new Task(targeted, "Two", 1));
        entry(owner, targeted, "2026-10-01T03:00:00Z", 1800);
        entry(owner, targeted, "2026-10-01T04:00:00Z", 1800);
        var request = new ReportProjectsRequest();
        request.setSortBy("usagePercent");
        request.setDirection("desc");
        var rows = reports.getProjects(owner.getId(), new ReportFilterRequest(), request).getContent();
        assertThat(rows).extracting(row -> row.projectId()).containsExactly(targeted.getId(), untargeted.getId());
        assertThat(rows.get(0).trackedSeconds()).isEqualTo(3600);
        assertThat(rows.get(0).taskProgressPercent()).isEqualByComparingTo("50.00");
        assertThat(rows.get(1).usagePercent()).isNull();
    }

    @Test
    void foreignProjectFilterIsRejectedWithoutExposingPrivateData() {
        User owner = user("report-selection@example.com");
        User other = user("report-selection-other@example.com");
        Project privateProject = project(other, "Private", 120);
        var filter = new ReportFilterRequest();
        filter.setProjectId(privateProject.getId());
        assertThatThrownBy(() ->
                reports.getProjects(owner.getId(), filter, new ReportProjectsRequest()))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void zeroPreviousTotalHasNoPercentageBaseline() {
        User owner = user("report-zero-baseline@example.com");
        Project project = project(owner, "New work", 120);
        entry(owner, project, "2026-10-01T03:00:00Z", 1800);
        assertThat(reports.getSummary(owner.getId(), datedStatus(new ReportFilterRequest()))
                .summary().trackedTimeTrendPercent()).isNull();
    }

    @Test
    void roundedUsageSortingMatchesDisplayedPercentages() {
        User owner = user("report-rounding@example.com");
        Project high = project(owner, "High", 3);
        Project low = project(owner, "Low", 3);
        entry(owner, high, "2026-10-01T03:00:00Z", 61);
        entry(owner, low, "2026-10-01T04:00:00Z", 60);
        var request = new ReportProjectsRequest();
        request.setSortBy("usagePercent");
        request.setLimit(1);
        var first = reports.getProjects(owner.getId(), new ReportFilterRequest(), request);
        assertThat(first.getContent().get(0).projectId()).isEqualTo(low.getId());
        assertThat(first.getContent().get(0).usagePercent()).isEqualByComparingTo("33.33");
        request.setPage(2);
        var second = reports.getProjects(owner.getId(), new ReportFilterRequest(), request);
        assertThat(second.getContent().get(0).projectId()).isEqualTo(high.getId());
        assertThat(second.getContent().get(0).usagePercent()).isEqualByComparingTo("33.89");
    }

    @Autowired private org.springframework.web.context.WebApplicationContext webContext;

    @Test
    void reportEndpointsRequireAuthentication() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        for (String endpoint : List.of("summary", "projects", "distribution", "work-trend", "work-pattern")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/reports/" + endpoint))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        }
    }

    @Test
    void authenticatedReportUsesSharedNotFoundHandler() throws Exception {
        User owner = user("report-http-owner@example.com");
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/reports/summary")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(owner.getEmail()))
                .param("projectId", java.util.UUID.randomUUID().toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void workPatternAttributesWholeEntryToItsBangkokStartHour() {
        User owner = user("report-start-hour@example.com");
        Project project = project(owner, "Across midnight", 120);
        entry(owner, project, "2026-10-01T16:30:00Z", 7200);
        var result = reports.getWorkPattern(owner.getId(), new ReportFilterRequest());
        assertThat(result.mostActiveHour()).isEqualTo(23);
        assertThat(result.timeByHour().get(23).trackedSeconds()).isEqualTo(7200);
        assertThat(result.timeByHour().get(0).trackedSeconds()).isZero();
    }

    private ReportFilterRequest datedStatus(ReportFilterRequest filter) {
        filter.setFrom(LocalDate.of(2026, 10, 1));
        filter.setTo(LocalDate.of(2026, 10, 1));
        return filter;
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
