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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.mapper.ReportMapper;
import th.ac.kku.freelance_hub.repository.ReportQueryRepository;
import th.ac.kku.freelance_hub.domain.entity.Task;
import th.ac.kku.freelance_hub.domain.entity.TimeEntry;
import org.springframework.data.domain.PageRequest;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.request.report.ReportFilterRequest;
import th.ac.kku.freelance_hub.dto.request.report.ReportGranularity;
import th.ac.kku.freelance_hub.dto.request.report.ReportGroupBy;
import th.ac.kku.freelance_hub.dto.request.report.ReportProjectsRequest;
import th.ac.kku.freelance_hub.dto.response.report.ReportDistributionResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportProjectResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportSummaryResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkPatternResponse;
import th.ac.kku.freelance_hub.dto.response.report.ReportWorkTrendResponse;
import th.ac.kku.freelance_hub.service.ReportService;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;

@Service
public class ReportServiceImpl implements ReportService {
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    private final ReportQueryRepository repository;
    private final Clock clock;
    private final ReportMapper mapper;

    public ReportServiceImpl(ReportQueryRepository repository, Clock clock, ReportMapper mapper) {
        this.repository = repository;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(
            UUID ownerId,
            ReportFilterRequest filter
    ) {
        Context context = context(ownerId, filter);
        validateSelection(ownerId, filter);
        List<ClientInfo> allClients = repository.findClients(ownerId).stream()
                .map(client -> new ClientInfo(client.getId(), client.getName()))
                .sorted(Comparator.comparing(ClientInfo::name).thenComparing(ClientInfo::id)).toList();
        List<ProjectInfo> allProjects = visibleProjects(ownerId).stream()
                .sorted(Comparator.comparing(ProjectInfo::name).thenComparing(ProjectInfo::id)).toList();

        List<ProjectInfo> selected = selectProjects(allProjects, filter);
        List<EntryInfo> entries = entries(ownerId, context, selected);
        Totals current = totals(entries);
        BigDecimal trend = previousTrend(ownerId, filter, context, current.trackedSeconds(), selected);

        Map<UUID, Long> byProject = projectSeconds(entries);
        Map<UUID, Long> byClient = selected.stream()
                .collect(Collectors.groupingBy(
                        p -> p.clientId(),
                        Collectors.summingLong(p ->
                                byProject.getOrDefault(p.id(), 0L))
                ));

        long totalClients = filter.getClientId() != null && filter.getStatus() == null
                ? 1
                : filter.getProjectId() != null || filter.getClientId() != null || filter.getStatus() != null
                    ? selected.stream()
                            .map(p -> p.clientId())
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
                                        c.id(), c.name()
                                ))
                                .toList(),
                        allProjects.stream()
                                .map(p -> new ReportSummaryResponse.ProjectOption(
                                        p.id(),
                                        p.name(),
                                        p.clientId(),
                                        p.status()
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
            throw new IllegalArgumentException("กรุณาระบุช่วงวันที่สำหรับกราฟแนวโน้ม"
            );
        }

        long buckets = switch (granularity) {
            case DAY -> ChronoUnit.DAYS.between(filter.getFrom(), filter.getTo()) + 1;
            case WEEK -> ChronoUnit.WEEKS.between(periodStart(filter.getFrom(), granularity), periodStart(filter.getTo(), granularity)) + 1;
            case MONTH -> ChronoUnit.MONTHS.between(periodStart(filter.getFrom(), granularity), periodStart(filter.getTo(), granularity)) + 1;
        };
        if (buckets > 366) throw new IllegalArgumentException("กรุณาลดช่วงวันที่หรือเลือกกลุ่มสัปดาห์/เดือน (สูงสุด 366 จุด)");

        Map<LocalDate, Long> totals = new HashMap<>();
        List<ProjectInfo> selected = selectProjects(visibleProjects(ownerId), filter);
        for (EntryInfo entry : entries(ownerId, context, selected)) {
            totals.merge(
                    periodStart(entry.startedAt().atZone(BANGKOK).toLocalDate(), granularity),
                    entry.durationSeconds(),
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

        List<ProjectInfo> projects = selectProjects(visibleProjects(ownerId), filter);
        Map<UUID, Long> byProject = projectSeconds(entries(ownerId, context, projects));
        Map<UUID, Long> totals = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();

        for (ProjectInfo project : projects) {
            UUID id = groupBy == ReportGroupBy.CLIENT
                    ? project.clientId()
                    : project.id();
            String name = groupBy == ReportGroupBy.CLIENT
                    ? project.clientName()
                    : project.name();

            names.put(id, name);
            totals.merge(
                    id,
                    byProject.getOrDefault(project.id(), 0L),
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

        List<ProjectInfo> selected = selectProjects(visibleProjects(ownerId), filter);
        List<EntryInfo> entries = entries(ownerId, context, selected);
        for (EntryInfo entry : entries) {
            var start = entry.startedAt().atZone(BANGKOK);
            weekday[start.getDayOfWeek().getValue() - 1] += entry.durationSeconds();
            hourly[start.getHour()] += entry.durationSeconds();
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

        BigDecimal trend = filter.getFrom() == null ? null
                : previousTrend(ownerId, filter, context,
                        totals(entries).trackedSeconds(), selected);

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

        Objects.requireNonNull(request, "request is required");
        if (request.getPage() < 1 || request.getLimit() < 1 || request.getLimit() > 100) {
            throw new IllegalArgumentException("page ต้องไม่น้อยกว่า 1 และ limit ต้องอยู่ระหว่าง 1 ถึง 100");
        }
        Comparator<ReportProjectResponse> order = projectOrder(request);
        List<ProjectInfo> projects = selectProjects(visibleProjects(ownerId), filter);
        Map<UUID, Long> seconds = projectSeconds(entries(ownerId, context, projects));
        Map<UUID, long[]> taskCounts = new HashMap<>();
        List<UUID> projectIds = projects.stream().map(ProjectInfo::id).toList();
        List<Task> taskRows = projectIds.isEmpty() ? List.of()
                : repository.findTasks(ownerId, projectIds);
        for (var task : taskRows) {
            long[] counts = taskCounts.computeIfAbsent(task.getProject().getId(), id -> new long[2]);
            counts[0]++;
            if (task.getStatus() == TaskStatus.COMPLETED) counts[1]++;
        }
        List<ReportProjectResponse> rows = projects.stream().map(project -> {
            long[] counts = taskCounts.getOrDefault(project.id(), new long[2]);
            Long targetSeconds = project.targetMinutes() == null
                    ? null : project.targetMinutes().longValue() * 60;
            long trackedSeconds = seconds.getOrDefault(project.id(), 0L);
            BigDecimal usagePercent = targetSeconds == null || targetSeconds == 0
                    ? null : percentage(trackedSeconds, targetSeconds);
            BigDecimal taskProgressPercent = percentage(counts[1], counts[0]);
            return mapper.toProjectResponse(new ReportMapper.ProjectMetrics(
                    project.id(), project.name(), project.clientId(), project.clientName(),
                    project.color(), targetSeconds, trackedSeconds,
                    usagePercent, taskProgressPercent, project.status()));
        }).sorted(order).toList();
        var pageable = PageRequest.of(request.getPage() - 1, request.getLimit());
        long offset = pageable.getOffset();
        if (offset >= rows.size()) return new PageImpl<>(List.of(), pageable, rows.size());
        int start = Math.toIntExact(offset);
        int end = (int) Math.min(offset + request.getLimit(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    private Comparator<ReportProjectResponse> projectOrder(ReportProjectsRequest request) {
        boolean descending;
        if ("asc".equalsIgnoreCase(request.getDirection())) descending = false;
        else if ("desc".equalsIgnoreCase(request.getDirection())) descending = true;
        else throw new IllegalArgumentException("direction ต้องเป็น asc หรือ desc");
        Comparator<ReportProjectResponse> order = switch (request.getSortBy()) {
            case "projectName" -> Comparator.comparing(row -> row.projectName().toLowerCase(java.util.Locale.ROOT));
            case "clientName" -> Comparator.comparing(row -> row.clientName().toLowerCase(java.util.Locale.ROOT));
            case "trackedSeconds" -> Comparator.comparingLong(ReportProjectResponse::trackedSeconds);
            case "usagePercent" -> Comparator.comparing(ReportProjectResponse::usagePercent,
                    Comparator.nullsLast(descending ? Comparator.<BigDecimal>reverseOrder() : Comparator.<BigDecimal>naturalOrder()));
            default -> throw new IllegalArgumentException("sortBy ไม่ถูกต้อง");
        };
        if (descending && !"usagePercent".equals(request.getSortBy())) order = order.reversed();
        return order.thenComparing(ReportProjectResponse::projectId);
    }

    private Context context(UUID ownerId, ReportFilterRequest filter) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");

        LocalDate from = filter.getFrom();
        LocalDate to = filter.getTo();

        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("กรุณาระบุวันที่เริ่มและวันที่สิ้นสุดให้ครบ"
            );
        }

        if (from == null) {
            return new Context(null, null);
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("วันที่เริ่มต้องไม่เกินวันที่สิ้นสุด"
            );
        }

        return new Context(
                from.atStartOfDay(BANGKOK).toInstant(),
                to.plusDays(1).atStartOfDay(BANGKOK).toInstant()
        );
    }

    private void validateSelection(UUID ownerId, ReportFilterRequest filter) {
        UUID clientId = filter.getClientId();
        UUID projectId = filter.getProjectId();
        if (clientId != null && !repository.clientExists(clientId, ownerId)) {
            throw new ClientNotFoundException(clientId);
        }
        if (projectId != null) {
            var project = repository.findProject(projectId, ownerId)
                    .orElseThrow(() -> new ProjectNotFoundException(projectId));
            if (clientId != null && !clientId.equals(project.getClient().getId())) {
                throw new ProjectNotFoundException(projectId);
            }
        }
    }

    private List<ProjectInfo> selectProjects(
            List<ProjectInfo> projects,
            ReportFilterRequest filter
    ) {
        return projects.stream()
                .filter(p -> filter.getClientId() == null
                        || p.clientId()
                                .equals(filter.getClientId()))
                .filter(p -> filter.getProjectId() == null
                        || p.id().equals(filter.getProjectId()))
                .filter(p -> filter.getStatus() == null || p.status() == filter.getStatus())
                .toList();
    }

    private Map<UUID, Long> projectSeconds(List<EntryInfo> entries) {
        return entries.stream().collect(Collectors.groupingBy(
                EntryInfo::projectId, Collectors.summingLong(EntryInfo::durationSeconds)));
    }

    private List<EntryInfo> entries(UUID ownerId, Context context, List<ProjectInfo> selected) {
        List<UUID> ids = selected.stream().map(ProjectInfo::id).toList();
        if (ids.isEmpty()) return List.of();
        List<TimeEntry> rows = context.from() == null
                ? repository.findEntries(ownerId, ids)
                : repository.findEntries(
                        ownerId, ids, context.from(), context.toExclusive());
        return rows.stream().map(row -> new EntryInfo(row.getProject().getId(),
                row.getStartedAt(), row.getDurationSeconds())).toList();
    }

    private Totals totals(List<EntryInfo> entries) {
        return new Totals(entries.stream().mapToLong(EntryInfo::durationSeconds).sum(), entries.size());
    }

    private BigDecimal previousTrend(UUID ownerId, ReportFilterRequest filter,
            Context context, long currentSeconds, List<ProjectInfo> selected) {
        if (filter.getFrom() == null) return null;
        long days = ChronoUnit.DAYS.between(filter.getFrom(), filter.getTo()) + 1;
        Instant previousFrom = filter.getFrom().minusDays(days)
                .atStartOfDay(BANGKOK).toInstant();
        long previousSeconds = totals(entries(ownerId, new Context(previousFrom, context.from()), selected))
                .trackedSeconds();
        return percentChange(currentSeconds, previousSeconds);
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
            return null;
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(previous),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private List<ProjectInfo> visibleProjects(UUID ownerId) {
        return repository.findProjects(ownerId).stream()
                .map(p -> new ProjectInfo(p.getId(), p.getName(), p.getClient().getId(),
                        p.getClient().getName(), p.getStatus(), p.getColor(), p.getTargetMinutes())).toList();
    }

    private record ClientInfo(UUID id, String name) {}
    private record ProjectInfo(UUID id, String name, UUID clientId, String clientName,
            ProjectStatus status, String color, Integer targetMinutes) {}
    private record EntryInfo(UUID projectId, Instant startedAt, long durationSeconds) {}
    private record Context(Instant from, Instant toExclusive) {}
    private record Totals(long trackedSeconds, long entryCount) {}
}
