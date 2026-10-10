package th.ac.kku.freelance_hub.domain.progress;

import java.util.List;

public final class ProjectProgressThresholds {

    private ProjectProgressThresholds() {
    }

    public static List<Integer> reached(
            long trackedMinutes,
            Integer targetMinutes
    ) {
        if (trackedMinutes < 0) {
            throw new IllegalArgumentException(
                    "เวลาที่บันทึกต้องไม่ติดลบ"
            );
        }

        if (targetMinutes == null) {
            return List.of();
        }

        if (targetMinutes <= 0) {
            throw new IllegalArgumentException(
                    "เวลาเป้าหมายต้องมากกว่าศูนย์"
            );
        }

        if (trackedMinutes >= targetMinutes) {
            return List.of(80, 100);
        }

        long eightyPercentMinutes = (4L * targetMinutes + 4L) / 5L;
        return trackedMinutes >= eightyPercentMinutes
                ? List.of(80)
                : List.of();
    }

    public static List<Integer> newlyReached(
        long previousTrackedMinutes,
        Integer previousTargetMinutes,
        long currentTrackedMinutes,
        Integer currentTargetMinutes
    ) {
        List<Integer> previouslyReached =
                reached(previousTrackedMinutes, previousTargetMinutes);

        return reached(currentTrackedMinutes, currentTargetMinutes).stream()
                .filter(threshold -> !previouslyReached.contains(threshold))
                .toList();
    }
}