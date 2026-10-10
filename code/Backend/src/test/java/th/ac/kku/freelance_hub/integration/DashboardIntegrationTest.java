package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.DashboardService;
import th.ac.kku.freelance_hub.dto.request.dashboard.DashboardActivityPeriod;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardResponse;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(DashboardIntegrationTest.FixedClockConfiguration.class)
class DashboardIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-30T05:00:00Z");

    @Autowired private UserRepository users;
    @Autowired private ClientRepository clients;
    @Autowired private ProjectRepository projects;
    @Autowired private TaskRepository tasks;
    @Autowired private TimeEntryRepository entries;
    @Autowired private DashboardService dashboard;
    @Autowired private org.springframework.web.context.WebApplicationContext webContext;

    @Test
    void invalidActivityPeriodUsesSharedRequestErrorHandler() throws Exception {
        User owner = user("dashboard-invalid-period@example.com");
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(owner.getEmail()))
                .param("period", "UNKNOWN"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.status").value(400))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.details.field").value("period"));
    }

    @Test
    void summarizesCurrentWeekProjectsAndTasksForOwner() {
        User owner = user("dashboard-owner@example.com");
        User other = user("dashboard-other@example.com");
        Project project = project(owner, "Website", 120);
        Project otherProject = project(other, "Private", 120);
        Task done = tasks.saveAndFlush(new Task(project, "Design", 1));
        done.changeStatus(TaskStatus.COMPLETED, NOW);
        tasks.saveAndFlush(done);
        tasks.saveAndFlush(new Task(project, "Build", 2));

        entry(owner, project, null, "Unassigned work", "2026-09-30T02:00:00Z", 3600);
        entry(owner, project, done, "Design work", "2026-09-29T02:00:00Z", 1800);
        entry(owner, project, null, "Previous week", "2026-09-24T02:00:00Z", 2700);
        entry(other, otherProject, null, "Other account", "2026-09-30T02:00:00Z", 7200);

        DashboardResponse result = dashboard.getDashboard(owner.getId());

        assertThat(result.generatedAt()).isEqualTo(NOW);
        assertThat(result.summary().weekTrackedSeconds()).isEqualTo(5400);
        assertThat(result.summary().weekTrendPercent()).isEqualByComparingTo("100.00");
        assertThat(result.summary().activeProjectCount()).isEqualTo(1);
        assertThat(result.summary().activeProjectTrackedSeconds()).isEqualTo(8100);
        assertThat(result.summary().activeProjectTargetSeconds()).isEqualTo(7200);
        assertThat(result.summary().targetUsagePercent()).isEqualByComparingTo("112.50");
        assertThat(result.summary().completedTaskCount()).isEqualTo(1);
        assertThat(result.summary().totalTaskCount()).isEqualTo(2);
        assertThat(result.summary().completedTaskPercent()).isEqualByComparingTo("50.00");
        assertThat(result.dailyWork()).hasSize(3);
        assertThat(result.dailyWork().get(2).trackedSeconds()).isEqualTo(3600);
        assertThat(result.activeProjects()).hasSize(1);
        assertThat(result.activeProjects().get(0).name()).isEqualTo("Website");
        assertThat(result.activeProjects().get(0).taskProgressPercent()).isEqualByComparingTo("50.00");
        assertThat(result.openTasks()).extracting(task -> task.name()).containsExactly("Build");
        assertThat(result.recentTimeEntries()).hasSize(2);
        assertThat(result.recentTimeEntries().get(0).description()).isEqualTo("Unassigned work");
        assertThat(result.recentTimeEntries().get(0).durationSeconds()).isEqualTo(3600);
        assertThat(result.recentTimeEntries().get(1).taskName()).isEqualTo("Design");
    }

    @Test
    void returnsEmptyOverviewWhenOwnerHasNoProjectsOrEntries() {
        User owner = user("dashboard-empty@example.com");

        DashboardResponse result = dashboard.getDashboard(owner.getId());

        assertThat(result.summary().weekTrackedSeconds()).isZero();
        assertThat(result.summary().weekTrendPercent()).isNull();
        assertThat(result.summary().targetUsagePercent()).isNull();
        assertThat(result.summary().completedTaskPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dailyWork()).hasSize(3);
        assertThat(result.activeProjects()).isEmpty();
        assertThat(result.openTasks()).isEmpty();
        assertThat(result.recentTimeEntries()).isEmpty();
    }

    @Test
    void activityUsesBangkokDatesAndReturnsZeroFilledDaysAndMonths() {
        User owner = user("dashboard-chart-owner@example.com");
        User other = user("dashboard-chart-other@example.com");
        Project project = project(owner, "Chart", 120);
        Project otherProject = project(other, "Other chart", 120);
        entry(owner, project, null, "Today", "2026-09-30T02:00:00Z", 3600);
        entry(owner, project, null, "Month start", "2026-08-31T18:00:00Z", 1800);
        entry(owner, project, null, "January", "2026-01-01T02:00:00Z", 7200);
        entry(other, otherProject, null, "Private", "2026-09-30T02:00:00Z", 5400);

        DashboardActivityResponse week = dashboard.getActivity(owner.getId(), DashboardActivityPeriod.WEEK);
        assertThat(week.points()).hasSize(3);
        assertThat(week.points().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(week.points().get(0).trackedSeconds()).isZero();
        assertThat(week.points().get(2).trackedSeconds()).isEqualTo(3600);

        DashboardActivityResponse month = dashboard.getActivity(owner.getId(), DashboardActivityPeriod.MONTH);
        assertThat(month.points()).hasSize(30);
        assertThat(month.points().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(month.points().get(0).trackedSeconds()).isEqualTo(1800);
        assertThat(month.points().get(29).trackedSeconds()).isEqualTo(3600);

        DashboardActivityResponse year = dashboard.getActivity(owner.getId(), DashboardActivityPeriod.YEAR);
        assertThat(year.points()).hasSize(12);
        assertThat(year.points().get(0).trackedSeconds()).isEqualTo(7200);
        assertThat(year.points().get(1).trackedSeconds()).isZero();
        assertThat(year.points().get(8).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(year.points().get(8).trackedSeconds()).isEqualTo(5400);
    }

    @Test
    void weeklySummaryAndBothChartsUseTheSameBangkokCalendarWeek() {
        User owner = user("dashboard-week-boundary@example.com");
        Project project = project(owner, "Week boundary", 120);
        entry(owner, project, null, "Previous Sunday", "2026-09-27T16:58:00Z", 60);
        entry(owner, project, null, "Monday midnight", "2026-09-27T17:00:00Z", 60);
        entry(owner, project, null, "Wednesday midnight", "2026-09-29T17:00:00Z", 90);
        entry(owner, project, null, "Tomorrow", "2026-09-30T17:00:00Z", 300);

        DashboardResponse overview = dashboard.getDashboard(owner.getId());
        DashboardActivityResponse activity = dashboard.getActivity(owner.getId(), DashboardActivityPeriod.WEEK);

        assertThat(overview.summary().weekTrackedSeconds()).isEqualTo(150);
        assertThat(overview.dailyWork()).extracting(day -> day.date()).containsExactly(
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30));
        assertThat(overview.dailyWork()).extracting(day -> day.trackedSeconds()).containsExactly(60L, 0L, 90L);
        assertThat(activity.points()).extracting(point -> point.date()).containsExactly(
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30));
        assertThat(activity.points()).extracting(point -> point.trackedSeconds()).containsExactly(60L, 0L, 90L);
    }

    @Test
    void excludesDeletedClientsFromAllDashboardSectionsBeforeRecentEntryPagination() {
        User owner = user("dashboard-deleted-client@example.com");
        Project visible = project(owner, "Visible", 120);
        Project hidden = project(owner, "Hidden", 120);
        tasks.saveAndFlush(new Task(visible, "Visible task", 1));
        tasks.saveAndFlush(new Task(hidden, "Hidden task", 1));
        entry(owner, visible, null, "Visible history", "2026-09-28T01:00:00Z", 600);
        entry(owner, hidden, null, "Previous hidden", "2026-09-24T01:00:00Z", 3600);
        for (int hour = 1; hour <= 3; hour++) {
            entry(owner, hidden, null, "Hidden history", "2026-09-30T0" + hour + ":00:00Z", 3600);
        }
        hidden.getClient().softDelete();
        clients.saveAndFlush(hidden.getClient());

        DashboardResponse result = dashboard.getDashboard(owner.getId());
        assertThat(result.summary().weekTrackedSeconds()).isEqualTo(600);
        assertThat(result.summary().weekTrendPercent()).isNull();
        assertThat(result.summary().activeProjectCount()).isEqualTo(1);
        assertThat(result.summary().totalTaskCount()).isEqualTo(1);
        assertThat(result.activeProjects()).extracting(project -> project.name()).containsExactly("Visible");
        assertThat(result.openTasks()).hasSize(1);
        assertThat(result.recentTimeEntries()).extracting(entry -> entry.description()).containsExactly("Visible history");
        assertThat(result.dailyWork().stream().mapToLong(day -> day.trackedSeconds()).sum()).isEqualTo(600);
        for (DashboardActivityPeriod period : DashboardActivityPeriod.values()) {
            assertThat(dashboard.getActivity(owner.getId(), period).points().stream()
                    .mapToLong(point -> point.trackedSeconds()).sum()).isEqualTo(600);
        }
    }

    @Test
    void retainsDeletedTaskTimeButExcludesArchivedClientsAndProjects() {
        User owner = user("dashboard-history@example.com");
        Project active = project(owner, "Active history", 120);
        Task removed = tasks.saveAndFlush(new Task(active, "Removed task", 1));
        entry(owner, active, removed, "Active history", "2026-09-30T01:00:00Z", 600);
        removed.softDelete();
        tasks.saveAndFlush(removed);
        Project archived = project(owner, "Archived history", 120);
        Task archivedTask = tasks.saveAndFlush(new Task(archived, "Archived task", 1));
        entry(owner, archived, archivedTask, "Archived history", "2026-09-30T02:00:00Z", 1800);
        archivedTask.softDelete();
        tasks.saveAndFlush(archivedTask);
        archived.changeStatus(ProjectStatus.ARCHIVED);
        projects.saveAndFlush(archived);
        Project archivedClientProject = project(owner, "Inactive client history", 120);
        entry(owner, archivedClientProject, null, "Inactive client history", "2026-09-30T03:00:00Z", 3600);
        archivedClientProject.getClient().setActive(false);
        clients.saveAndFlush(archivedClientProject.getClient());

        DashboardResponse result = dashboard.getDashboard(owner.getId());
        assertThat(result.summary().weekTrackedSeconds()).isEqualTo(600);
        assertThat(result.summary().activeProjectCount()).isEqualTo(1);
        assertThat(result.summary().totalTaskCount()).isZero();
        assertThat(result.openTasks()).isEmpty();
        assertThat(result.recentTimeEntries()).extracting(entry -> entry.description())
                .containsExactly("Active history");
        assertThat(result.dailyWork().stream().mapToLong(day -> day.trackedSeconds()).sum()).isEqualTo(600);
        assertThat(result.summary().activeProjectTrackedSeconds()).isEqualTo(600);
        assertThat(result.summary().activeProjectTargetSeconds()).isEqualTo(7200);
        for (DashboardActivityPeriod period : DashboardActivityPeriod.values()) {
            assertThat(dashboard.getActivity(owner.getId(), period).points().stream()
                    .mapToLong(point -> point.trackedSeconds()).sum()).isEqualTo(600);
        }
    }

    @Test
    void returnsAllActiveProjectsWhenThereAreMoreThanFive() {
        User owner = user("dashboard-all-projects@example.com");
        for (int index = 1; index <= 7; index++) {
            project(owner, "Project " + index, 120);
        }
        DashboardResponse result = dashboard.getDashboard(owner.getId());
        assertThat(result.summary().activeProjectCount()).isEqualTo(7);
        assertThat(result.activeProjects()).hasSize(7);
    }

    private User user(String email) {
        return users.saveAndFlush(User.builder().email(email).passwordHash("test-hash").build());
    }

    private Project project(User owner, String name, int targetMinutes) {
        Client client = clients.saveAndFlush(new Client(owner, name + " Client"));
        Project project = new Project(owner, client, name);
        project.changeStatus(ProjectStatus.ACTIVE);
        project.updateDetails(name, null, null, null, null, targetMinutes);
        return projects.saveAndFlush(project);
    }

    private void entry(User owner, Project project, Task task, String description,
                       String startedAt, long durationSeconds) {
        entries.saveAndFlush(TimeEntry.createManualWithDurationSeconds(
                owner, project, task, description, Instant.parse(startedAt), durationSeconds));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock dashboardTestClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
