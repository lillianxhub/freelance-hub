package th.ac.kku.freelance_hub.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.kku.freelance_hub.exception.ReportRequestException;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.dto.response.report.ReportDistributionResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkPatternResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkTrendResponse;
import th.ac.kku.freelance_hub.repository.ReportQueryRepository;
import th.ac.kku.freelance_hub.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    private final ReportQueryRepository queries;
    private final Clock clock;

    public ReportServiceImpl(ReportQueryRepository queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(
            UUID ownerId,
            ReportFilterRequest filter
    ) {
        Context context = context(ownerId, filter);
        List<Client> allClients = queries.findVisibleClients(ownerId);
        List<Project> allProjects = queries.findVisibleProjects(ownerId);
        validateSelection(filter, allClients, allProjects);

        List<Project> selected = selectProjects(allProjects, filter);
        ReportQueryRepository.Totals current = totals(ownerId, filter, context);
        BigDecimal trend = null;
        if (filter.getFrom() != null) {
            long days = ChronoUnit.DAYS.between(filter.getFrom(), filter.getTo()) + 1;
            Instant previousFrom = filter.getFrom().minusDays(days)
                    .atStartOfDay(BANGKOK).toInstant();
            ReportQueryRepository.Totals previous = reportTotals(
                    ownerId, previousFrom, context.from(), filter
            );
            trend = percentChange(current.trackedSeconds(), previous.trackedSeconds());
        }

        Map<UUID, Long> byProject = projectSeconds(ownerId, filter, context);
        Map<UUID, Long> byClient = selected.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getClient().getId(),
                        Collectors.summingLong(p ->
                                byProject.getOrDefault(p.getId(), 0L))
                ));

        long totalClients = filter.getClientId() != null
                && filter.getStatus() == null
                ? 1
                : filter.getClientId() != null || filter.getProjectId() != null
                        || filter.getStatus() != null
                    ? selected.stream()
                            .map(p -> p.getClient().getId())
                            .distinct().count()
                    : allClients.size();

        ReportSummaryResponse.Summary summary =
                new ReportSummaryResponse.Summary(
                        current.trackedSeconds(),
                        trend,
                        current.entryCount(),
                        byProject.values().stream().filter(v -> v > 0).count(),
                        selected.size(),
                        byClient.values().stream().filter(v -> v > 0).count(),
                        totalClients
                );

        ReportSummaryResponse.Filters options =
                new ReportSummaryResponse.Filters(
                        allClients.stream()
                                .map(c -> new ReportSummaryResponse.ClientOption(
                                        c.getId(), c.getName()
                                ))
                                .toList(),
                        allProjects.stream()
                                .map(p -> new ReportSummaryResponse.ProjectOption(
                                        p.getId(),
                                        p.getName(),
                                        p.getClient().getId(),
                                        p.getStatus()
                                ))
                                .toList()
                );

        return new ReportSummaryResponse(clock.instant(), options, summary);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportWorkTrendResponse getWorkTrend(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportGranularity granularity
    ) {
        Context context = context(ownerId, filter);
        validateSelection(ownerId, filter);
        Objects.requireNonNull(granularity, "granularity is required");
        if (filter.getFrom() == null) {
            throw new ReportRequestException(
                    HttpStatus.BAD_REQUEST, "กรุณาระบุช่วงวันที่สำหรับกราฟแนวโน้ม"
            );
        }

        Map<LocalDate, Long> totals = new HashMap<>();
        for (ReportQueryRepository.DailyTime day : reportTimeByDay(
                ownerId, context.from(), context.toExclusive(), filter
        )) {
            totals.merge(
                    periodStart(day.date(), granularity),
                    day.trackedSeconds(),
                    Long::sum
            );
        }

        List<ReportWorkTrendResponse.Point> points = new ArrayList<>();
        LocalDate cursor = periodStart(filter.getFrom(), granularity);
        LocalDate last = periodStart(filter.getTo(), granularity);

        while (!cursor.isAfter(last)) {
            points.add(new ReportWorkTrendResponse.Point(
                    cursor,
                    totals.getOrDefault(cursor, 0L)
            ));
            cursor = switch (granularity) {
                case DAY -> cursor.plusDays(1);
                case WEEK -> cursor.plusWeeks(1);
                case MONTH -> cursor.plusMonths(1);
            };
        }

        return new ReportWorkTrendResponse(granularity, points);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDistributionResponse getDistribution(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportGroupBy groupBy
    ) {
        Context context = context(ownerId, filter);
        validateSelection(ownerId, filter);
        Objects.requireNonNull(groupBy, "groupBy is required");

        List<Project> projects = selectProjects(
                queries.findVisibleProjects(ownerId), filter
        );
        Map<UUID, Long> byProject = projectSeconds(ownerId, filter, context);
        Map<UUID, Long> totals = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();

        for (Project project : projects) {
            UUID id = groupBy == ReportGroupBy.CLIENT
                    ? project.getClient().getId()
                    : project.getId();
            String name = groupBy == ReportGroupBy.CLIENT
                    ? project.getClient().getName()
                    : project.getName();

            names.put(id, name);
            totals.merge(
                    id,
                    byProject.getOrDefault(project.getId(), 0L),
                    Long::sum
            );
        }

        long grandTotal = totals.values().stream()
                .mapToLong(Long::longValue).sum();

        List<ReportDistributionResponse.Item> items = totals.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new ReportDistributionResponse.Item(
                        entry.getKey(),
                        names.get(entry.getKey()),
                        entry.getValue(),
                        percentage(entry.getValue(), grandTotal)
                ))
                .sorted(Comparator
                        .comparingLong(
                                ReportDistributionResponse.Item::trackedSeconds
                        )
                        .reversed()
                        .thenComparing(ReportDistributionResponse.Item::name))
                .toList();

        return new ReportDistributionResponse(groupBy, items);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportWorkPatternResponse getWorkPattern(
            UUID ownerId,
            ReportFilterRequest filter
    ) {
        Context context = context(ownerId, filter);
        validateSelection(ownerId, filter);

        long[] weekday = new long[7];
        long[] hourly = new long[24];

        for (ReportQueryRepository.DailyTime day : reportTimeByDay(
                ownerId, context.from(), context.toExclusive(), filter
        )) {
            weekday[day.date().getDayOfWeek().getValue() - 1]
                    += day.trackedSeconds();
        }

        for (ReportQueryRepository.HourlyTime hour : reportTimeByHour(
                ownerId, context.from(), context.toExclusive(), filter
        )) {
            hourly[hour.hour()] += hour.trackedSeconds();
        }

        List<ReportWorkPatternResponse.DayTotal> days = new ArrayList<>();
        List<ReportWorkPatternResponse.HourTotal> hours = new ArrayList<>();

        DayOfWeek bestDay = null;
        Integer bestHour = null;
        long bestDaySeconds = 0;
        long bestHourSeconds = 0;

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(i + 1);
            days.add(new ReportWorkPatternResponse.DayTotal(day, weekday[i]));
            if (weekday[i] > bestDaySeconds) {
                bestDaySeconds = weekday[i];
                bestDay = day;
            }
        }

        for (int i = 0; i < 24; i++) {
            hours.add(new ReportWorkPatternResponse.HourTotal(i, hourly[i]));
            if (hourly[i] > bestHourSeconds) {
                bestHourSeconds = hourly[i];
                bestHour = i;
            }
        }

        BigDecimal trend = null;
        if (filter.getFrom() != null) {
            long periodDays = ChronoUnit.DAYS.between(
                    filter.getFrom(), filter.getTo()
            ) + 1;
            Instant previousFrom = filter.getFrom().minusDays(periodDays)
                    .atStartOfDay(BANGKOK).toInstant();

            long currentSeconds = totals(ownerId, filter, context)
                    .trackedSeconds();
            long previousSeconds = reportTotals(
                    ownerId, previousFrom, context.from(), filter
            ).trackedSeconds();
            trend = percentChange(currentSeconds, previousSeconds);
        }

        return new ReportWorkPatternResponse(
                bestDay,
                bestHour,
                trend,
                days,
                hours
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportProjectResponse> getProjects(
            UUID ownerId,
            ReportFilterRequest filter,
            ReportProjectsRequest request
    ) {
        Context context = context(ownerId, filter);
        validateSelection(ownerId, filter);

        Map<UUID, Long> byProject = projectSeconds(ownerId, filter, context);
        Map<UUID, ReportQueryRepository.TaskProgress> progress =
                queries.taskProgress(ownerId).stream()
                        .collect(Collectors.toMap(
                                ReportQueryRepository.TaskProgress::projectId,
                                item -> item
                        ));

        List<ReportProjectResponse> rows = selectProjects(
                queries.findVisibleProjects(ownerId), filter
        ).stream()
                .map(project -> projectRow(
                        project,
                        byProject.getOrDefault(project.getId(), 0L),
                        progress.get(project.getId())
                ))
                .sorted(projectComparator(request))
                .toList();

        int fromIndex = Math.min(
                (int) Math.min(
                        (long) (request.getPage() - 1) * request.getLimit(),
                        rows.size()
                ),
                rows.size()
        );
        int toIndex = Math.min(fromIndex + request.getLimit(), rows.size());

        return new PageImpl<>(
                rows.subList(fromIndex, toIndex),
                PageRequest.of(request.getPage() - 1, request.getLimit()),
                rows.size()
        );
    }

    private ReportProjectResponse projectRow(
            Project project,
            long trackedSeconds,
            ReportQueryRepository.TaskProgress progress
    ) {
        Long targetSeconds = project.getTargetMinutes() == null
                ? null
                : project.getTargetMinutes().longValue() * 60;

        BigDecimal usagePercent = targetSeconds == null
                || targetSeconds == 0
                ? null
                : percentage(trackedSeconds, targetSeconds);

        long totalTasks = progress == null ? 0 : progress.totalTasks();
        long completedTasks = progress == null ? 0 : progress.completedTasks();

        return new ReportProjectResponse(
                project.getId(),
                project.getName(),
                project.getClient().getId(),
                project.getClient().getName(),
                project.getColor(),
                targetSeconds,
                trackedSeconds,
                usagePercent,
                percentage(completedTasks, totalTasks),
                project.getStatus()
        );
    }

    private Comparator<ReportProjectResponse> projectComparator(
            ReportProjectsRequest request
    ) {
        Comparator<ReportProjectResponse> comparator =
                switch (request.getSortBy()) {
                    case "clientName" -> Comparator.comparing(
                            ReportProjectResponse::clientName,
                            String.CASE_INSENSITIVE_ORDER
                    );
                    case "trackedSeconds" -> Comparator.comparingLong(
                            ReportProjectResponse::trackedSeconds
                    );
                    case "usagePercent" -> Comparator.comparing(
                            ReportProjectResponse::usagePercent,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    );
                    default -> Comparator.comparing(
                            ReportProjectResponse::projectName,
                            String.CASE_INSENSITIVE_ORDER
                    );
                };

        if ("desc".equalsIgnoreCase(request.getDirection())) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(ReportProjectResponse::projectId);
    }

    private Context context(UUID ownerId, ReportFilterRequest filter) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");

        LocalDate from = filter.getFrom();
        LocalDate to = filter.getTo();

        if ((from == null) != (to == null)) {
            throw new ReportRequestException(
                    HttpStatus.BAD_REQUEST, "กรุณาระบุวันที่เริ่มและวันที่สิ้นสุดให้ครบ"
            );
        }

        if (from == null) {
            return new Context(null, null);
        }

        if (from.isAfter(to)) {
            throw new ReportRequestException(
                    HttpStatus.BAD_REQUEST,
                    "วันที่เริ่มต้องไม่เกินวันที่สิ้นสุด"
            );
        }

        return new Context(
                from.atStartOfDay(BANGKOK).toInstant(),
                to.plusDays(1).atStartOfDay(BANGKOK).toInstant()
        );
    }

    private void validateSelection(UUID ownerId, ReportFilterRequest filter) {
        if (filter.getClientId() == null && filter.getProjectId() == null) {
            return;
        }

        validateSelection(
                filter,
                filter.getClientId() == null
                        ? List.of()
                        : queries.findVisibleClients(ownerId),
                queries.findVisibleProjects(ownerId)
        );
    }

    private void validateSelection(
            ReportFilterRequest filter,
            List<Client> clients,
            List<Project> projects
    ) {
        UUID clientId = filter.getClientId();
        UUID projectId = filter.getProjectId();

        if (clientId != null && clients.stream()
                .noneMatch(c -> c.getId().equals(clientId))) {
            throw new ReportRequestException(
                    HttpStatus.NOT_FOUND, "ไม่พบลูกค้า"
            );
        }

        if (projectId != null && projects.stream()
                .noneMatch(p -> p.getId().equals(projectId)
                        && (clientId == null
                        || p.getClient().getId().equals(clientId)))) {
            throw new ReportRequestException(
                    HttpStatus.NOT_FOUND, "ไม่พบโปรเจกต์"
            );
        }
    }

    private List<Project> selectProjects(
            List<Project> projects,
            ReportFilterRequest filter
    ) {
        return projects.stream()
                .filter(p -> filter.getClientId() == null
                        || p.getClient().getId()
                                .equals(filter.getClientId()))
                .filter(p -> filter.getProjectId() == null
                        || p.getId().equals(filter.getProjectId()))
                .filter(p -> filter.getStatus() == null
                        || p.getStatus() == filter.getStatus())
                .toList();
    }

    private Map<UUID, Long> projectSeconds(
            UUID ownerId,
            ReportFilterRequest filter,
            Context context
    ) {
        List<ReportQueryRepository.ProjectTime> rows = filter.getStatus() == null
                ? queries.timeByProject(ownerId, context.from(), context.toExclusive(),
                        filter.getClientId(), filter.getProjectId())
                : queries.timeByProject(ownerId, context.from(), context.toExclusive(),
                        filter.getClientId(), filter.getProjectId(), filter.getStatus());
        return rows.stream().collect(Collectors.toMap(
                ReportQueryRepository.ProjectTime::projectId,
                ReportQueryRepository.ProjectTime::trackedSeconds
        ));
    }

    private ReportQueryRepository.Totals totals(
            UUID ownerId,
            ReportFilterRequest filter,
            Context context
    ) {
        return reportTotals(ownerId, context.from(), context.toExclusive(), filter);
    }

    private ReportQueryRepository.Totals reportTotals(
            UUID ownerId, Instant from, Instant to, ReportFilterRequest filter) {
        if (filter.getStatus() == null) {
            return queries.totals(ownerId, from, to,
                    filter.getClientId(), filter.getProjectId());
        }
        return queries.totals(ownerId, from, to,
                filter.getClientId(), filter.getProjectId(), filter.getStatus());
    }

    private List<ReportQueryRepository.DailyTime> reportTimeByDay(
            UUID ownerId, Instant from, Instant to, ReportFilterRequest filter) {
        if (filter.getStatus() == null) {
            return queries.timeByDay(ownerId, from, to,
                    filter.getClientId(), filter.getProjectId());
        }
        return queries.timeByDay(ownerId, from, to,
                filter.getClientId(), filter.getProjectId(), filter.getStatus());
    }

    private List<ReportQueryRepository.HourlyTime> reportTimeByHour(
            UUID ownerId, Instant from, Instant to, ReportFilterRequest filter) {
        if (filter.getStatus() == null) {
            return queries.timeByHour(ownerId, from, to,
                    filter.getClientId(), filter.getProjectId());
        }
        return queries.timeByHour(ownerId, from, to,
                filter.getClientId(), filter.getProjectId(), filter.getStatus());
    }

    private LocalDate periodStart(
            LocalDate date,
            ReportGranularity granularity
    ) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private BigDecimal percentage(long part, long whole) {
        if (whole == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(previous),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private record Context(Instant from, Instant toExclusive) {}
}
