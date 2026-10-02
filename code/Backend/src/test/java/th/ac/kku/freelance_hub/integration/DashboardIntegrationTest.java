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
import th.ac.kku.freelance_hub.dto.request.DashboardActivityPeriod;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardResponse;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.DashboardService;

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
        assertThat(result.dailyWork()).hasSize(7);
        assertThat(result.dailyWork().get(6).trackedSeconds()).isEqualTo(3600);
        assertThat(result.activeProjects()).hasSize(1);
        assertThat(result.activeProjects().get(0).name()).isEqualTo("Website");
        assertThat(result.activeProjects().get(0).taskProgressPercent()).isEqualByComparingTo("50.00");
        assertThat(result.openTasks()).extracting(task -> task.name()).containsExactly("Build");
    }

    @Test
    void returnsEmptyOverviewWhenOwnerHasNoProjectsOrEntries() {
        User owner = user("dashboard-empty@example.com");

        DashboardResponse result = dashboard.getDashboard(owner.getId());

        assertThat(result.summary().weekTrackedSeconds()).isZero();
        assertThat(result.summary().weekTrendPercent()).isNull();
        assertThat(result.summary().targetUsagePercent()).isNull();
        assertThat(result.summary().completedTaskPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dailyWork()).hasSize(7);
        assertThat(result.activeProjects()).isEmpty();
        assertThat(result.openTasks()).isEmpty();
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
        assertThat(week.points()).hasSize(7);
        assertThat(week.points().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(week.points().get(0).trackedSeconds()).isZero();
        assertThat(week.points().get(6).trackedSeconds()).isEqualTo(3600);

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
