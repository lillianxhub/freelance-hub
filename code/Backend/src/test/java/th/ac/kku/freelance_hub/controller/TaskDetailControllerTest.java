package th.ac.kku.freelance_hub.controller;

import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.task.ChangeTaskStatusRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
@ExtendWith(MockitoExtension.class)
class TaskDetailControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

    @Mock private TaskService taskService;
    @Mock private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskDetailController(taskService, userService))
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory()))
                .setValidator(validator)
                .build();
    }

    @Test
    void getByIdUsesCurrentOwnerAndReturnsCommonResponse() throws Exception {
        stubCurrentUser();
        when(taskService.getById(OWNER_ID, TASK_ID))
                .thenReturn(TaskResponse.builder()
                        .id(TASK_ID)
                        .projectId(PROJECT_ID)
                        .name("Design")
                        .description("Task detail")
                        .status(TaskStatus.OPEN)
                        .sortOrder(0)
                        .build());

        mockMvc.perform(get("/api/tasks/{taskId}", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงข้อมูลงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Design"))
                .andExpect(jsonPath("$.data.description").value("Task detail"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.sortOrder").value(0))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).getById(OWNER_ID, TASK_ID);
    }

    @Test
    void getByIdReturns404WhenTaskIsUnavailableToCurrentOwner() throws Exception {
        stubCurrentUser();
        when(taskService.getById(OWNER_ID, TASK_ID))
                .thenThrow(new TaskNotFoundException(TASK_ID));

        mockMvc.perform(get("/api/tasks/{taskId}", TASK_ID))
                .andExpect(status().isNotFound());

        verify(taskService).getById(OWNER_ID, TASK_ID);
    }

    @Test
    void updateUsesPutAndReturnsCommonResponse() throws Exception {
        stubCurrentUser();
        when(taskService.update(eq(OWNER_ID), eq(TASK_ID), any(UpdateTaskRequest.class)))
                .thenReturn(TaskResponse.builder()
                        .id(TASK_ID)
                        .projectId(PROJECT_ID)
                        .name("Renamed")
                        .description("Updated detail")
                        .status(TaskStatus.OPEN)
                        .build());

        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"description\":\"Updated detail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("แก้ไขงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Renamed"))
                .andExpect(jsonPath("$.data.description").value("Updated detail"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).update(eq(OWNER_ID), eq(TASK_ID), argThat(request ->
                "Renamed".equals(request.getName())
                        && "Updated detail".equals(request.getDescription())));
    }

    @Test
    void updateRejectsBlankOrTooLongNamesBeforeCallingService() throws Exception {
        for (String name : List.of("", "x".repeat(181))) {
            mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(taskService, userService);
    }

    @Test
    void updateReturns404ForUnavailableTask() throws Exception {
        stubCurrentUser();
        when(taskService.update(eq(OWNER_ID), eq(TASK_ID), any(UpdateTaskRequest.class)))
                .thenThrow(new TaskNotFoundException(TASK_ID));

        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns409WhenProjectStateForbidsEditing() throws Exception {
        stubCurrentUser();
        when(taskService.update(eq(OWNER_ID), eq(TASK_ID), any(UpdateTaskRequest.class)))
                .thenThrow(new IllegalStateException("Cannot change tasks"));

        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isConflict());
    }

    private void stubCurrentUser() {
        when(userService.currentUserId())
                .thenReturn(OWNER_ID);
    }

    @Test
    void changeStatusUsesPatchAndReturnsCommonResponse() throws Exception {
        stubCurrentUser();
        when(taskService.changeStatus(eq(OWNER_ID), eq(TASK_ID), any(ChangeTaskStatusRequest.class)))
                .thenReturn(TaskResponse.builder()
                        .id(TASK_ID)
                        .projectId(PROJECT_ID)
                        .status(TaskStatus.IN_PROGRESS)
                        .build());

        mockMvc.perform(patch("/api/tasks/{taskId}/status", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("เปลี่ยนสถานะงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).changeStatus(eq(OWNER_ID), eq(TASK_ID),
                argThat(request -> request.getStatus() == TaskStatus.IN_PROGRESS));
    }

    @Test
    void changeStatusRejectsMissingOrUnknownStatus() throws Exception {
        for (String body : List.of("{}", "{\"status\":null}", "{\"status\":\"ACTIVE\"}")) {
            mockMvc.perform(patch("/api/tasks/{taskId}/status", TASK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(taskService, userService);
    }

    @Test
    void changeStatusReturns404ForUnavailableTask() throws Exception {
        stubCurrentUser();
        when(taskService.changeStatus(eq(OWNER_ID), eq(TASK_ID), any(ChangeTaskStatusRequest.class)))
                .thenThrow(new TaskNotFoundException(TASK_ID));

        mockMvc.perform(patch("/api/tasks/{taskId}/status", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatusReturns409ForForbiddenTransition() throws Exception {
        stubCurrentUser();
        when(taskService.changeStatus(eq(OWNER_ID), eq(TASK_ID), any(ChangeTaskStatusRequest.class)))
                .thenThrow(new IllegalStateException("only an open task can be started"));

        mockMvc.perform(patch("/api/tasks/{taskId}/status", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturnsCommonResponseAndUsesCurrentOwner() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ลบงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).delete(OWNER_ID, TASK_ID);
    }

    @Test
    void deleteReturns404ForUnavailableTask() throws Exception {
        stubCurrentUser();
        doThrow(new TaskNotFoundException(TASK_ID))
                .when(taskService).delete(OWNER_ID, TASK_ID);

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns409WhenProjectForbidsEditing() throws Exception {
        stubCurrentUser();
        doThrow(new IllegalStateException("Cannot change tasks"))
                .when(taskService).delete(OWNER_ID, TASK_ID);

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID))
                .andExpect(status().isConflict());
    }
}