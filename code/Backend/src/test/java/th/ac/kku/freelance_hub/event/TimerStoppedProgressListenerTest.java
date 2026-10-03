package th.ac.kku.freelance_hub.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntrySummaryResponse;
@ExtendWith(MockitoExtension.class)
class TimerStoppedProgressListenerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TimeEntryQueryService timeEntryQueryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TimerStoppedProgressListener listener;

    @Test
    void publishesBothThresholdsWhenOneTimerCrossesEightyAndHundredPercent() {
        Project project = mock(Project.class);
        when(project.getTargetMinutes()).thenReturn(100);
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));
        when(timeEntryQueryService.summarize(
                eq(OWNER_ID),
                any(TimeEntryFilterRequest.class)
        )).thenReturn(
                TimeEntrySummaryResponse.builder()
                        .totalSeconds(110 * 60)
                        .build()
        );

        // ก่อนหยุด Timer มี 79 นาที; Timer นี้เพิ่ม 31 นาที เป็น 110 นาที
        listener.onTimerStopped(stoppedEvent(31));

        ArgumentCaptor<TimeEntryFilterRequest> filterCaptor =
                ArgumentCaptor.forClass(TimeEntryFilterRequest.class);
        verify(timeEntryQueryService).summarize(
                eq(OWNER_ID),
                filterCaptor.capture()
        );
        assertThat(filterCaptor.getValue().getProjectId())
                .isEqualTo(PROJECT_ID);

        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2))
                .publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues()).containsExactly(
                new ProjectProgressThresholdEvent(OWNER_ID, PROJECT_ID, 80),
                new ProjectProgressThresholdEvent(OWNER_ID, PROJECT_ID, 100)
        );
    }

    @Test
    void doesNothingWhenProjectHasNoTimeTarget() {
        Project project = mock(Project.class);
        when(project.getTargetMinutes()).thenReturn((Integer) null);
        
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        listener.onTimerStopped(stoppedEvent(10));

        verifyNoInteractions(timeEntryQueryService, eventPublisher);
    }

    private static TimerStoppedEvent stoppedEvent(int durationMinutes) {
        Instant startedAt = Instant.parse("2026-09-28T09:00:00Z");
        return new TimerStoppedEvent(
                UUID.randomUUID(),
                OWNER_ID,
                PROJECT_ID,
                null,
                durationMinutes * 60L,
                startedAt,
                startedAt.plusSeconds(durationMinutes * 60L)
        );
    }
}
