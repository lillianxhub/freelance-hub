package th.ac.kku.freelance_hub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.EntryType;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.service.UserService;

@ExtendWith(MockitoExtension.class)
class TimeEntryControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final Instant FROM =
            Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO =
            Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private TimeEntryService timeEntryService;

    @Mock
    private TimeEntryQueryService timeEntryQueryService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TimeEntryController(
                                timeEntryService,
                                timeEntryQueryService,
                                userService
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsManualEntryForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.createManual(
                eq(OWNER_ID),
                any(ManualTimeEntryRequest.class)
        )).thenReturn(response("Manual work"));

        mockMvc.perform(post("/api/time-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "taskId": "%s",
                                  "description": "Manual work",
                                  "startedAt": "2026-09-26T08:00:00Z",
                                  "durationMinutes": 30
                                }
                                """.formatted(PROJECT_ID, TASK_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/time-entries/" + ENTRY_ID
                ))
                .andExpect(jsonPath("$.id").value(ENTRY_ID.toString()));

        verify(timeEntryService).createManual(
                eq(OWNER_ID),
                any(ManualTimeEntryRequest.class)
        );
    }

    @Test
    void rejectsInvalidManualEntryBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/time-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(timeEntryService, never()).createManual(any(), any());
    }

    @Test
    void listsEntriesUsingBoundFiltersAndCurrentUser() throws Exception {
        stubCurrentUser();
        PageRequest pageable = PageRequest.of(1, 5);
        when(timeEntryQueryService.list(
                eq(OWNER_ID),
                any(TimeEntryFilterRequest.class)
        )).thenReturn(new PageImpl<>(
                List.of(response("Listed work")),
                pageable,
                6
        ));

        mockMvc.perform(get("/api/time-entries")
                        .param("projectId", PROJECT_ID.toString())
                        .param("taskId", TASK_ID.toString())
                        .param("entryType", "MANUAL")
                        .param("from", FROM.toString())
                        .param("to", TO.toString())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description")
                        .value("Listed work"));

        verify(timeEntryQueryService).list(
                eq(OWNER_ID),
                org.mockito.ArgumentMatchers.argThat(filter ->
                        PROJECT_ID.equals(filter.getProjectId())
                                && TASK_ID.equals(filter.getTaskId())
                                && filter.getEntryType() == EntryType.MANUAL
                                && FROM.equals(filter.getFrom())
                                && TO.equals(filter.getTo())
                                && filter.getPage() == 1
                                && filter.getSize() == 5
                )
        );
    }

    @Test
    void summarizesEntriesForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryQueryService.summarize(
                eq(OWNER_ID),
                any(TimeEntryFilterRequest.class)
        )).thenReturn(TimeEntrySummaryResponse.builder()
                .from(FROM)
                .to(TO)
                .entryCount(3)
                .totalMinutes(90)
                .build());

        mockMvc.perform(get("/api/time-entries/summary")
                        .param("from", FROM.toString())
                        .param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(3))
                .andExpect(jsonPath("$.totalMinutes").value(90));

        verify(timeEntryQueryService).summarize(
                eq(OWNER_ID),
                any(TimeEntryFilterRequest.class)
        );
    }

    @Test
    void updatesEntryForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.update(
                eq(OWNER_ID),
                eq(ENTRY_ID),
                any(UpdateTimeEntryRequest.class)
        )).thenReturn(response("Updated work"));

        mockMvc.perform(patch("/api/time-entries/{id}", ENTRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description")
                        .value("Updated work"));

        verify(timeEntryService).update(
                eq(OWNER_ID),
                eq(ENTRY_ID),
                any(UpdateTimeEntryRequest.class)
        );
    }

    @Test
    void rejectsEmptyUpdateBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/time-entries/{id}", ENTRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(timeEntryService, never()).update(any(), any(), any());
    }

    @Test
    void deletesEntryForCurrentUser() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete("/api/time-entries/{id}", ENTRY_ID))
                .andExpect(status().isNoContent());

        verify(timeEntryService).delete(OWNER_ID, ENTRY_ID);
    }

    private void stubCurrentUser() {
        when(userService.getCurrentUserEntity()).thenReturn(
                User.builder().id(OWNER_ID).build()
        );
    }

    private static TimeEntryResponse response(String description) {
        return TimeEntryResponse.builder()
                .id(ENTRY_ID)
                .projectId(PROJECT_ID)
                .taskId(TASK_ID)
                .description(description)
                .entryType(EntryType.MANUAL)
                .startedAt(FROM)
                .endedAt(TO)
                .durationMinutes(30)
                .build();
    }
}
