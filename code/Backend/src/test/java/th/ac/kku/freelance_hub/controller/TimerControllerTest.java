package th.ac.kku.freelance_hub.controller;

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
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.RunningTimerNotFoundException;
import th.ac.kku.freelance_hub.exception.TimerAlreadyRunningException;
import th.ac.kku.freelance_hub.service.TimerService;
import th.ac.kku.freelance_hub.service.UserService;

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
                .setControllerAdvice(new GlobalExceptionHandler())
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
                .andExpect(jsonPath("$.running").value(true));

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
                .andExpect(status().isConflict());
    }

    @Test
    void returnsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.getCurrentTimer(OWNER_ID))
                .thenReturn(runningTimer());

        mockMvc.perform(get("/api/timer/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.running").value(true));

        verify(timeEntryService).getCurrentTimer(OWNER_ID);
    }

    @Test
    void returnsNotFoundWhenNoTimerIsRunning() throws Exception {
        stubCurrentUser();
        when(timeEntryService.getCurrentTimer(OWNER_ID))
                .thenThrow(new RunningTimerNotFoundException());

        mockMvc.perform(get("/api/timer/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    void stopsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();
        TimeEntryResponse stoppedTimer = runningTimer();
        stoppedTimer.setEndedAt(STARTED_AT.plusSeconds(120));
        stoppedTimer.setDurationMinutes(2);
        stoppedTimer.setRunning(false);
        when(timeEntryService.stopTimer(OWNER_ID))
                .thenReturn(stoppedTimer);

        mockMvc.perform(post("/api/timer/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.durationMinutes").value(2));

        verify(timeEntryService).stopTimer(OWNER_ID);
    }

    @Test
    void cancelsCurrentTimerForCurrentUser() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete("/api/timer/current"))
                .andExpect(status().isNoContent());

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
                .description("Current work")
                .entryType(EntryType.TIMER)
                .startedAt(STARTED_AT)
                .running(true)
                .build();
    }
}
