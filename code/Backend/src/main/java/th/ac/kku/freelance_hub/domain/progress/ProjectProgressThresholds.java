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
                    "trackedMinutes must not be negative"
            );
        }

        if (targetMinutes == null) {
            return List.of();
        }

        if (targetMinutes <= 0) {
            throw new IllegalArgumentException(
                    "targetMinutes must be greater than zero"
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