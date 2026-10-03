package th.ac.kku.freelance_hub.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.TaskRepository;
import th.ac.kku.freelance_hub.repository.TimeEntryRepository;
import th.ac.kku.freelance_hub.service.DashboardService;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.dto.request.dashboard.DashboardActivityPeriod;
import th.ac.kku.freelance_hub.dto.response.dashboard.ActiveProjectResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DailyWorkResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityPointResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.OpenTaskResponse;
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId THAILAND = ZoneId.of("Asia/Bangkok");

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryService timeEntryService;
    private final Clock clock;

    public DashboardServiceImpl(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            TimeEntryRepository timeEntryRepository,
            TimeEntryService timeEntryService,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryService = timeEntryService;
        this.clock = clock;
    }

    @Override
    public DashboardResponse getDashboard(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId is required");

        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, THAILAND);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        );
        LocalDate previousWeekStart = weekStart.minusWeeks(1);

        long weekSeconds = timeEntryService.sumCompletedSeconds(
                ownerId, weekStart, tomorrow
        );
        long previousWeekSeconds = timeEntryService.sumCompletedSeconds(
                ownerId, previousWeekStart, weekStart
        );

        List<DailyWorkResponse> dailyWork =
                timeEntryService.sumDailySeconds(
                        ownerId, today.minusDays(6), tomorrow
                ).stream()
                .map(day -> new DailyWorkResponse(
                        day.day(), day.totalSeconds()
                ))
                .toList();

        List<Project> projects =
                projectRepository.findVisibleByOwnerId(ownerId);
        List<Project> activeProjects = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .toList();

        List<UUID> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

        // Repository เดิมมี method นี้อยู่แล้ว
        List<Task> tasks = projectIds.isEmpty()
                ? List.of()
                : taskRepository.findActiveByProjectIds(
                        ownerId, projectIds
                );

        Map<UUID, List<Task>> tasksByProject = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getProject().getId()
                ));

        Map<UUID, Long> secondsByProject =
                timeEntryRepository.sumCompletedSecondsByProject(ownerId)
                        .stream()
                        .collect(Collectors.toMap(
                                TimeEntryRepository.ProjectTotalView::getProjectId,
                                TimeEntryRepository.ProjectTotalView::getTotalSeconds
                        ));

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        // นับเฉพาะโปรเจกต์ที่ตั้งเป้าหมาย เพื่อให้ตัวตั้งและตัวหาร
        // ของเปอร์เซ็นต์อ้างถึงโปรเจกต์ชุดเดียวกัน
        List<Project> projectsWithTarget = activeProjects.stream()
                .filter(project -> project.getTargetMinutes() != null)
                .toList();

        long targetSeconds = projectsWithTarget.stream()
                .mapToLong(project -> project.getTargetMinutes() * 60L)
                .sum();

        long trackedAgainstTargets = projectsWithTarget.stream()
                .mapToLong(project ->
                        secondsByProject.getOrDefault(project.getId(), 0L)
                )
                .sum();

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                weekSeconds,
                percentChange(weekSeconds, previousWeekSeconds),
                activeProjects.size(),
                trackedAgainstTargets,
                targetSeconds,
                targetSeconds == 0
                        ? null
                        : percent(trackedAgainstTargets, targetSeconds),
                completedTasks,
                totalTasks,
                percent(completedTasks, totalTasks)
        );

        List<ActiveProjectResponse> visibleProjects =
                activeProjects.stream()
                .map(project -> toActiveProject(
                        project,
                        tasksByProject.getOrDefault(
                                project.getId(), List.of()
                        )
                ))
                .sorted(Comparator
                        .comparing(
                                ActiveProjectResponse::taskProgressPercent
                        )
                        .reversed()
                        .thenComparing(ActiveProjectResponse::name))
                .limit(5)
                .toList();

        List<OpenTaskResponse> openTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .sorted(Comparator
                        .comparing(
                                Task::getUpdatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                        .thenComparing(Task::getId))
                .limit(5)
                .map(task -> new OpenTaskResponse(
                        task.getId(),
                        task.getProject().getId(),
                        task.getName(),
                        task.getProject().getName(),
                        task.getStatus()
                ))
                .toList();

        return new DashboardResponse(
                now, summary, dailyWork, visibleProjects, openTasks
        );
    }

    @Override
    public DashboardActivityResponse getActivity(
            UUID ownerId,
            DashboardActivityPeriod period
    ) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(period, "period is required");

        LocalDate today = LocalDate.ofInstant(clock.instant(), THAILAND);
        LocalDate from = switch (period) {
            case WEEK -> today.minusDays(6);
            case MONTH -> today.withDayOfMonth(1);
            case YEAR -> today.withDayOfYear(1);
        };
        LocalDate to = switch (period) {
            case WEEK -> today.plusDays(1);
            case MONTH -> from.plusMonths(1);
            case YEAR -> from.plusYears(1);
        };

        List<TimeEntryService.DailySeconds> daily =
                timeEntryService.sumDailySeconds(ownerId, from, to);

        if (period != DashboardActivityPeriod.YEAR) {
            return new DashboardActivityResponse(
                    period,
                    daily.stream()
                            .map(day -> new DashboardActivityPointResponse(
                                    day.day(), day.totalSeconds()
                            ))
                            .toList()
            );
        }

        Map<LocalDate, Long> monthly = new LinkedHashMap<>();
        for (LocalDate month = from; month.isBefore(to); month = month.plusMonths(1)) {
            monthly.put(month, 0L);
        }
        for (TimeEntryService.DailySeconds day : daily) {
            monthly.merge(day.day().withDayOfMonth(1), day.totalSeconds(), Long::sum);
        }
        return new DashboardActivityResponse(
                period,
                monthly.entrySet().stream()
                        .map(month -> new DashboardActivityPointResponse(
                                month.getKey(), month.getValue()
                        ))
                        .toList()
        );
    }

    private static ActiveProjectResponse toActiveProject(
            Project project,
            List<Task> tasks
    ) {
        long completed = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        return new ActiveProjectResponse(
                project.getId(),
                project.getName(),
                project.getClient().getName(),
                project.getColor(),
                project.getStatus(),
                completed,
                tasks.size(),
                percent(completed, tasks.size())
        );
    }

    private static BigDecimal percent(long amount, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentChange(
            long current,
            long previous
    ) {
        if (previous == 0) {
            return null;
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
    }
}
