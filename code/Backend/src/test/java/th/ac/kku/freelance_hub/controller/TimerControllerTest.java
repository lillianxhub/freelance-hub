package th.ac.kku.freelance_hub.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.exception.TimeTrackingExceptionHandler;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.service.TimerService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.timeentry.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
@ExtendWith(MockitoExtension.class)
class TimerControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final Instant STARTED_AT =
            Instant.parse("2026-09-26T08:00:00Z");

    @Mock
    private TimerService timeEntryService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TimerController(timeEntryService, userService)
                )
                .setControllerAdvice(new TimeTrackingExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void startsTimerForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.startTimer(
                eq(OWNER_ID),
                any(StartTimerRequest.class)
        )).thenReturn(runningTimer());

        mockMvc.perform(post("/api/timer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "description": "Current work"
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/time-entries/" + ENTRY_ID
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("เริ่มจับเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(timeEntryService).startTimer(
                eq(OWNER_ID),
                any(StartTimerRequest.class)
        );
    }

    @Test
    void rejectsStartWithoutProjectBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/timer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(timeEntryService, never()).startTimer(any(), any());
    }

    @Test
    void returnsConflictWhenTimerIsAlreadyRunning() throws Exception {
        stubCurrentUser();
        when(timeEntryService.startTimer(
                eq(OWNER_ID),
                any(StartTimerRequest.class)
        )).thenThrow(new TimerAlreadyRunningException());

        mockMvc.perform(post("/api/timer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + PROJECT_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("มีตัวจับเวลาที่กำลังทำงานอยู่แล้ว"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("TIMER_ALREADY_RUNNING"));
    }

    @Test
    void returnsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.getCurrentTimer(OWNER_ID))
                .thenReturn(Optional.of(runningTimer()));

        mockMvc.perform(get("/api/timer/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "ดึงข้อมูลตัวจับเวลาที่กำลังทำงานเรียบร้อยแล้ว"
                ))
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.data.timeEntry.id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data.timeEntry.project.id")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.timeEntry.project.name")
                        .value("Timer project"))
                .andExpect(jsonPath("$.data.timeEntry.task").doesNotExist())
                .andExpect(jsonPath("$.data.timeEntry.startedAt")
                        .value(STARTED_AT.toString()))
                .andExpect(jsonPath("$.data.timeEntry.description")
                        .value("Current work"))
                .andExpect(jsonPath("$.data.timeEntry.projectId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.timeEntry.entryType")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.timeEntry.running")
                        .doesNotExist());

        verify(timeEntryService).getCurrentTimer(OWNER_ID);
    }

    @Test
    void returnsNotFoundWhenNoTimerIsRunning() throws Exception {
        stubCurrentUser();
        when(timeEntryService.getCurrentTimer(OWNER_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/timer/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ไม่มีตัวจับเวลาที่กำลังทำงาน"))
                .andExpect(jsonPath("$.data.running").value(false))
                .andExpect(jsonPath("$.data.timeEntry").doesNotExist());
    }

    @Test
    void stopsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();
        TimeEntryResponse stoppedTimer = runningTimer();
        stoppedTimer.setEndedAt(STARTED_AT.plusSeconds(120));
        stoppedTimer.setDurationSeconds(120L);
        stoppedTimer.setRunning(false);
        when(timeEntryService.stopTimer(OWNER_ID))
                .thenReturn(stoppedTimer);

        mockMvc.perform(post("/api/timer/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("หยุดจับเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data.startedAt")
                        .value(STARTED_AT.toString()))
                .andExpect(jsonPath("$.data.endedAt")
                        .value(STARTED_AT.plusSeconds(120).toString()))
                .andExpect(jsonPath("$.data.durationSeconds").value(120))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(timeEntryService).stopTimer(OWNER_ID);
    }

    @Test
    void cancelsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete("/api/timer/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ยกเลิกการจับเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(timeEntryService).cancelTimer(OWNER_ID);
    }

    private void stubCurrentUser() {
        when(userService.getCurrentUserEntity()).thenReturn(
                User.builder().id(OWNER_ID).build()
        );
    }

    private static TimeEntryResponse runningTimer() {
        return TimeEntryResponse.builder()
                .id(ENTRY_ID)
                .projectId(PROJECT_ID)
                .projectName("Timer project")
                .description("Current work")
                .entryType(EntryType.TIMER)
                .startedAt(STARTED_AT)
                .running(true)
                .build();
    }
}
