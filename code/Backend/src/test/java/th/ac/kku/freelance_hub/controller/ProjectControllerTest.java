package th.ac.kku.freelance_hub.controller;

import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.UserService;

import static org.hamcrest.Matchers.nullValue;
import th.ac.kku.freelance_hub.dto.request.project.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.project.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.request.project.UpdateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.project.ProjectListItemResponse;
import th.ac.kku.freelance_hub.dto.response.project.ProjectResponse;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProjectController(projectService, userService)
                )
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory()))
                .setValidator(validator)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    void createUsesCurrentUserAndReturns201() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.create(eq(OWNER_ID), any(CreateProjectRequest.class)))
                .thenReturn(ProjectResponse.builder()
                        .id(PROJECT_ID)
                        .name("Website")
                        .build());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID
                                + "\",\"name\":\"Website\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/projects/" + PROJECT_ID
                ))
                .andExpect(jsonPath("$.data.name").value("Website"));

        verify(projectService).create(
                eq(OWNER_ID),
                org.mockito.ArgumentMatchers.argThat(request ->
                        CLIENT_ID.equals(request.getClientId())
                                && "Website".equals(request.getName())
                )
        );
    }

    @Test
    void createRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(projectService, never()).create(any(), any());
    }

    @Test
    void listPassesFiltersAndPaginationToService() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.list(
                eq(OWNER_ID),
                eq("web"),
                eq(ProjectStatus.ACTIVE),
                eq(CLIENT_ID),
                any(Pageable.class),
                eq(false), eq(false)
        )).thenReturn(new PageImpl<>(
            List.of(ProjectListItemResponse.builder()
                    .id(PROJECT_ID)
                    .timeTracking(ProjectListItemResponse.TimeTracking.builder()
                            .trackedSeconds(3600)
                            .trackedHours(new BigDecimal("1.00"))
                            .usagePercent(new BigDecimal("50.00"))
                            .build())
                    .build()),
            PageRequest.of(1, 5),
            6
        ));

        mockMvc.perform(get("/api/projects")
                        .param("search", "web")
                        .param("status", "ACTIVE")
                        .param("clientId", CLIENT_ID.toString())
                        .param("page", "2")
                        .param("limit", "5")
                        .param("sortBy", "project_name")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].timeTracking.trackedSeconds").value(3600))
                .andExpect(jsonPath("$.data[0].timeTracking.trackedHours").value(1.0))
                .andExpect(jsonPath("$.data[0].timeTracking.usagePercent").value(50.0))
                .andExpect(jsonPath("$.data[0].tasks").doesNotExist());

        verify(projectService).list(
                eq(OWNER_ID),
                eq("web"),
                eq(ProjectStatus.ACTIVE),
                eq(CLIENT_ID),
                org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5
                                && pageable.getSort()
                                        .getOrderFor("name")
                                        .isDescending()
                ),
                eq(false), eq(false)
        );
    }

    @Test
    void listIncludesTasksWhenRequested() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.list(
                eq(OWNER_ID), eq(null), eq(null), eq(null),
                any(Pageable.class), eq(true), eq(false)
        )).thenReturn(new PageImpl<>(List.of(
                ProjectListItemResponse.builder()
                        .id(PROJECT_ID)
                        .tasks(List.of(TaskResponse.builder().name("Design").build()))
                        .build()
        )));

        mockMvc.perform(get("/api/projects").param("include", "tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tasks[0].name").value("Design"));

        verify(projectService).list(
                eq(OWNER_ID), eq(null), eq(null), eq(null),
                any(Pageable.class), eq(true), eq(false)
        );
    }

    @Test
    void listAcceptsAllStatuses() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.list(
                eq(OWNER_ID), eq(null), eq(null), eq(null),
                any(Pageable.class), eq(false), eq(true)
        )).thenReturn(new PageImpl<>(List.of(
                ProjectListItemResponse.builder()
                        .id(PROJECT_ID)
                        .status(ProjectStatus.ARCHIVED)
                        .build()
        )));

        mockMvc.perform(get("/api/projects")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("sortBy", "project_name")
                        .param("direction", "ASC")
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ARCHIVED"));
    }

    @Test
    void getByIdReturns404WhenProjectIsNotFound() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.getById(OWNER_ID, PROJECT_ID))
                .thenThrow(new ProjectNotFoundException(PROJECT_ID));

        mockMvc.perform(get("/api/projects/{id}", PROJECT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_FOUND"));

        verify(projectService).getById(OWNER_ID, PROJECT_ID);
    }

    @Test
    void invalidStatusChangeReturns409() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.changeStatus(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                any(ChangeProjectStatusRequest.class)
        )).thenThrow(new IllegalStateException(
                "cannot change project status"
        ));

        mockMvc.perform(patch("/api/projects/{id}/status", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    void deleteCallsArchiveAndReturnsCommonResponse() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);

        mockMvc.perform(delete("/api/projects/{id}", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("ลบโปรเจกต์สำเร็จ"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(projectService).archive(OWNER_ID, PROJECT_ID);
    }

        @Test
    void updateUsesPutAndReturnsCommonResponse() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);

        when(projectService.update(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                any(UpdateProjectRequest.class)
        )).thenReturn(ProjectResponse.builder()
                .id(PROJECT_ID)
                .clientId(CLIENT_ID)
                .name("Updated Website")
                .targetMinutes(180)
                .build());

        mockMvc.perform(put("/api/projects/{id}", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "name": "Updated Website",
                                  "targetMinutes": 180
                                }
                                """.formatted(CLIENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("แก้ไขโปรเจกต์สำเร็จ"))
                .andExpect(jsonPath("$.data.id")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.name")
                        .value("Updated Website"))
                .andExpect(jsonPath("$.data.targetMinutes").value(180))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(projectService).update(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.argThat(request ->
                        CLIENT_ID.equals(request.getClientId())
                                && "Updated Website".equals(request.getName())
                                && Integer.valueOf(180)
                                        .equals(request.getTargetMinutes())
                )
        );
    }

    @Test
    void updateRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(put("/api/projects/{id}", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s"
                                }
                                """.formatted(CLIENT_ID)))
                .andExpect(status().isBadRequest());

        verify(projectService, never()).update(
                any(),
                any(),
                any()
        );
    }

        @Test
    void changeStatusUsesPatchAndReturnsCommonResponse() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);

        when(projectService.changeStatus(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                any(ChangeProjectStatusRequest.class)
        )).thenReturn(ProjectResponse.builder()
                .id(PROJECT_ID)
                .status(ProjectStatus.ARCHIVED)
                .build());

        mockMvc.perform(patch("/api/projects/{id}/status", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("เปลี่ยนสถานะโปรเจกต์สำเร็จ"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(projectService).changeStatus(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.getStatus() == ProjectStatus.ARCHIVED
                )
        );
    }


    @Test
    void getProjectDetailReturnsNestedDataAndCommonResponse() throws Exception {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
        when(projectService.getById(OWNER_ID, PROJECT_ID))
                .thenReturn(ProjectListItemResponse.builder()
                        .id(PROJECT_ID)
                        .name("Website")
                        .status(ProjectStatus.ACTIVE)
                        .targetHours(new BigDecimal("36.00"))
                        .client(ProjectListItemResponse.ClientSummary.builder()
                                .id(CLIENT_ID).name("Acme").build())
                        .taskProgress(ProjectListItemResponse.TaskProgress.builder()
                                .totalTasks(2).completedTasks(1)
                                .percent(new BigDecimal("50.00")).build())
                        .timeTracking(ProjectListItemResponse.TimeTracking.builder()
                                .trackedSeconds(36000)
                                .trackedHours(new BigDecimal("10.00"))
                                .usagePercent(new BigDecimal("27.78"))
                                .build())
                        .build());

        mockMvc.perform(get("/api/projects/{id}", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงรายละเอียดโปรเจกต์สำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Website"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.targetHours").value(36.0))
                .andExpect(jsonPath("$.data.client.id").value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$.data.client.name").value("Acme"))
                .andExpect(jsonPath("$.data.taskProgress.totalTasks").value(2))
                .andExpect(jsonPath("$.data.taskProgress.completedTasks").value(1))
                .andExpect(jsonPath("$.data.taskProgress.percent").value(50.0))
                .andExpect(jsonPath("$.data.timeTracking.trackedSeconds").value(36000))
                .andExpect(jsonPath("$.data.timeTracking.trackedHours").value(10.0))
                .andExpect(jsonPath("$.data.timeTracking.usagePercent").value(27.78))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(projectService).getById(OWNER_ID, PROJECT_ID);
    }
}
