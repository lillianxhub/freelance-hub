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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import th.ac.kku.freelance_hub.exception.TimeTrackingExceptionHandler;
import th.ac.kku.freelance_hub.exception.TimeEntryNotFoundException;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.timeentry.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntrySummaryResponse;
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
    private static final Instant CREATED_AT =
            Instant.parse("2026-08-31T23:59:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-10-01T00:01:00Z");

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
                .setControllerAdvice(new TimeTrackingExceptionHandler())
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
                                  "durationSeconds": 1800
                                }
                                """.formatted(PROJECT_ID, TASK_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/time-entries/" + ENTRY_ID
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("เพิ่มรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data.project.id")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.project.name")
                        .value("Project name"))
                .andExpect(jsonPath("$.data.task.id")
                        .value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.task.title")
                        .value("Task name"))
                .andExpect(jsonPath("$.data.startedAt")
                        .value(FROM.toString()))
                .andExpect(jsonPath("$.data.endedAt")
                        .value(TO.toString()))
                .andExpect(jsonPath("$.data.durationSeconds").value(1800))
                .andExpect(jsonPath("$.data.description")
                        .value("Manual work"))
                .andExpect(jsonPath("$.data.createdAt")
                        .value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.data.updatedAt")
                        .value(UPDATED_AT.toString()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.projectId").exists());

        verify(timeEntryService, never()).createManual(any(), any());
    }

    @Test
    void listsEntriesUsingBoundFiltersAndCurrentUser() throws Exception {
        stubCurrentUser();
        PageRequest pageable = PageRequest.of(0, 5);
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
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ดึงข้อมูลรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data[0].id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data[0].project.id")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data[0].project.name")
                        .value("Project name"))
                .andExpect(jsonPath("$.data[0].task.id")
                        .value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data[0].task.title")
                        .value("Task name"))
                .andExpect(jsonPath("$.data[0].startedAt")
                        .value(FROM.toString()))
                .andExpect(jsonPath("$.data[0].endedAt")
                        .value(TO.toString()))
                .andExpect(jsonPath("$.data[0].durationSeconds")
                        .value(1800))
                .andExpect(jsonPath("$.data[0].description")
                        .doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(5))
                .andExpect(jsonPath("$.meta.total").value(6))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(timeEntryQueryService).list(
                eq(OWNER_ID),
                org.mockito.ArgumentMatchers.argThat(filter ->
                        PROJECT_ID.equals(filter.getProjectId())
                                && TASK_ID.equals(filter.getTaskId())
                                && filter.getEntryType() == EntryType.MANUAL
                                && FROM.equals(filter.getFrom())
                                && TO.equals(filter.getTo())
                                && filter.getPage() == 1
                                && filter.getLimit() == 5
                )
        );
    }

    @Test
    void getsEntryDetailsForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryQueryService.getById(OWNER_ID, ENTRY_ID))
                .thenReturn(response("Detailed work"));

        mockMvc.perform(get("/api/time-entries/{id}", ENTRY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ดึงข้อมูลรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.id")
                        .value(ENTRY_ID.toString()))
                .andExpect(jsonPath("$.data.project.id")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.project.name")
                        .value("Project name"))
                .andExpect(jsonPath("$.data.task.id")
                        .value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.task.title")
                        .value("Task name"))
                .andExpect(jsonPath("$.data.startedAt")
                        .value(FROM.toString()))
                .andExpect(jsonPath("$.data.endedAt")
                        .value(TO.toString()))
                .andExpect(jsonPath("$.data.durationSeconds")
                        .value(1800))
                .andExpect(jsonPath("$.data.description")
                        .value("Detailed work"))
                .andExpect(jsonPath("$.data.createdAt")
                        .value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.data.updatedAt")
                        .value(UPDATED_AT.toString()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(timeEntryQueryService).getById(OWNER_ID, ENTRY_ID);
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
                .totalSeconds(5400)
                .build());

        mockMvc.perform(get("/api/time-entries/summary")
                        .param("from", FROM.toString())
                        .param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("สรุปรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.entryCount").value(3))
                .andExpect(jsonPath("$.data.totalSeconds").value(5400))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(timeEntryQueryService).summarize(
                eq(OWNER_ID),
                any(TimeEntryFilterRequest.class)
        );
    }

    @Test
    void returnsSharedErrorWhenEntryIsNotFound() throws Exception {
        stubCurrentUser();
        when(timeEntryQueryService.getById(OWNER_ID, ENTRY_ID))
                .thenThrow(new TimeEntryNotFoundException(ENTRY_ID));

        mockMvc.perform(get("/api/time-entries/{id}", ENTRY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ไม่พบรายการเวลา"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("TIME_ENTRY_NOT_FOUND"));
    }

    @Test
    void updatesEntryForCurrentUser() throws Exception {
        stubCurrentUser();
        when(timeEntryService.update(
                eq(OWNER_ID),
                eq(ENTRY_ID),
                any(UpdateTimeEntryRequest.class)
        )).thenReturn(response("Updated work"));

        mockMvc.perform(put("/api/time-entries/{id}", ENTRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "taskId": null,
                                  "description": "Updated work",
                                  "startedAt": "2026-09-01T00:00:00Z",
                                  "durationSeconds": 1800
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("แก้ไขรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data.description")
                        .value("Updated work"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(timeEntryService).update(
                eq(OWNER_ID),
                eq(ENTRY_ID),
                any(UpdateTimeEntryRequest.class)
        );
    }

    @Test
    void rejectsEmptyUpdateBeforeCallingService() throws Exception {
        mockMvc.perform(put("/api/time-entries/{id}", ENTRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(timeEntryService, never()).update(any(), any(), any());
    }

    @Test
    void deletesEntryForCurrentUser() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete("/api/time-entries/{id}", ENTRY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ลบรายการเวลาเรียบร้อยแล้ว"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

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
                .projectName("Project name")
                .taskId(TASK_ID)
                .taskName("Task name")
                .description(description)
                .entryType(EntryType.MANUAL)
                .startedAt(FROM)
                .endedAt(TO)
                .durationSeconds(1800L)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }
}
