package th.ac.kku.freelance_hub.domain.progress;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectProgressThresholdsTest {

    @Test
    void reportsReachedThresholds() {
        assertThat(ProjectProgressThresholds.reached(79, 100)).isEmpty();
        assertThat(ProjectProgressThresholds.reached(80, 100)).containsExactly(80);
        assertThat(ProjectProgressThresholds.reached(99, 100)).containsExactly(80);
        assertThat(ProjectProgressThresholds.reached(100, 100)).containsExactly(80, 100);
    }

    @Test
    void roundsUpTheEightyPercentBoundary() {
        assertThat(ProjectProgressThresholds.reached(5, 7)).isEmpty();
        assertThat(ProjectProgressThresholds.reached(6, 7)).containsExactly(80);
    }

    @Test
    void returnsNoThresholdWhenTargetIsNotSet() {
        assertThat(ProjectProgressThresholds.reached(100, null)).isEmpty();
    }

    @Test
    void reportsThresholdsCrossedByOneChange() {
        assertThat(ProjectProgressThresholds.newlyReached(79, 100, 80, 100))
                .containsExactly(80);

        // บันทึกเวลาครั้งเดียวข้ามทั้งสองเกณฑ์
        assertThat(ProjectProgressThresholds.newlyReached(79, 100, 100, 100))
                .containsExactly(80, 100);
    }

    @Test
    void doesNotReportThresholdsAlreadyReached() {
        assertThat(ProjectProgressThresholds.newlyReached(80, 100, 90, 100))
                .isEmpty();
        assertThat(ProjectProgressThresholds.newlyReached(100, 100, 120, 100))
                .isEmpty();
    }
}