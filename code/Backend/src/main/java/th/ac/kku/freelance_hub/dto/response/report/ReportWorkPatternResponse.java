package th.ac.kku.freelance_hub.dto.response.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

public record ReportWorkPatternResponse(
        DayOfWeek mostProductiveDay,
        Integer mostActiveHour,
        BigDecimal trackedTimeTrendPercent,
        List<DayTotal> timeByWeekday,
        List<HourTotal> timeByHour
) {
    public record DayTotal(
            DayOfWeek day,
            long trackedSeconds
    ) {}

    public record HourTotal(
            int hour,
            long trackedSeconds
    ) {}
}