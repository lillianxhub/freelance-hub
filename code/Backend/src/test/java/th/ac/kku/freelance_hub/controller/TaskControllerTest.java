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
import th.ac.kku.freelance_hub.domain.enums.TaskStatus;
import th.ac.kku.freelance_hub.dto.request.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.TaskResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final String BASE = "/api/projects/{projectId}/tasks";

    @Mock private TaskService taskService;
    @Mock private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(taskService, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    void createUsesCurrentUserAndReturns201() throws Exception {
        stubCurrentUser();
        when(taskService.create(
                eq(OWNER_ID), eq(PROJECT_ID), any(CreateTaskRequest.class)
        )).thenReturn(response("Design", TaskStatus.OPEN));

        mockMvc.perform(post(BASE, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Design\",\"sortOrder\":0}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/projects/" + PROJECT_ID + "/tasks/" + TASK_ID
                ))
                .andExpect(jsonPath("$.name").value("Design"));

        verify(taskService).create(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.<CreateTaskRequest>argThat(
                        request -> "Design".equals(request.getName())
                                && Integer.valueOf(0).equals(
                                        request.getSortOrder()
                                )
                )
        );
    }

    @Test
    void createRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(post(BASE, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).create(any(), any(), any());
    }

    @Test
    void listPassesPaginationToService() throws Exception {
        stubCurrentUser();
        when(taskService.list(
                eq(OWNER_ID), eq(PROJECT_ID), any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.<TaskResponse>of(),
                PageRequest.of(1, 5),
                0
        ));

        mockMvc.perform(get(BASE, PROJECT_ID)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "sortOrder,desc"))
                .andExpect(status().isOk());

        verify(taskService).list(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5
                                && pageable.getSort()
                                        .getOrderFor("sortOrder")
                                        .isDescending()
                )
        );
    }

    @Test
    void getByIdReturns404WhenTaskIsNotFound() throws Exception {
        stubCurrentUser();
        when(taskService.getById(OWNER_ID, PROJECT_ID, TASK_ID))
                .thenThrow(new TaskNotFoundException(TASK_ID));

        mockMvc.perform(get(BASE + "/{taskId}", PROJECT_ID, TASK_ID))
                .andExpect(status().isNotFound());

        verify(taskService).getById(OWNER_ID, PROJECT_ID, TASK_ID);
    }

    @Test
    void updatePassesRequestToService() throws Exception {
        stubCurrentUser();
        when(taskService.update(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                eq(TASK_ID),
                any(UpdateTaskRequest.class)
        )).thenReturn(response("Renamed", TaskStatus.OPEN));

        mockMvc.perform(patch(BASE + "/{taskId}", PROJECT_ID, TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));

        verify(taskService).update(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                eq(TASK_ID),
                org.mockito.ArgumentMatchers.<UpdateTaskRequest>argThat(
                        request -> "Renamed".equals(request.getName())
                )
        );
    }

    @Test
    void startAndCompletePassTaskToService() throws Exception {
        stubCurrentUser();
        when(taskService.start(OWNER_ID, PROJECT_ID, TASK_ID))
                .thenReturn(response("Design", TaskStatus.IN_PROGRESS));
        when(taskService.complete(OWNER_ID, PROJECT_ID, TASK_ID))
                .thenReturn(response("Design", TaskStatus.COMPLETED));

        mockMvc.perform(patch(
                        BASE + "/{taskId}/start", PROJECT_ID, TASK_ID
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch(
                        BASE + "/{taskId}/complete", PROJECT_ID, TASK_ID
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(taskService).start(OWNER_ID, PROJECT_ID, TASK_ID);
        verify(taskService).complete(OWNER_ID, PROJECT_ID, TASK_ID);
    }

    @Test
    void reorderPassesNewPositionToService() throws Exception {
        stubCurrentUser();
        when(taskService.reorder(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                eq(TASK_ID),
                any(ReorderTaskRequest.class)
        )).thenReturn(TaskResponse.builder()
                .id(TASK_ID)
                .projectId(PROJECT_ID)
                .sortOrder(1)
                .build());

        mockMvc.perform(patch(
                        BASE + "/{taskId}/reorder", PROJECT_ID, TASK_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(1));

        verify(taskService).reorder(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                eq(TASK_ID),
                org.mockito.ArgumentMatchers.<ReorderTaskRequest>argThat(
                        request -> Integer.valueOf(1).equals(
                                request.getSortOrder()
                        )
                )
        );
    }

    @Test
    void deleteReturns204() throws Exception {
        stubCurrentUser();

        mockMvc.perform(delete(BASE + "/{taskId}", PROJECT_ID, TASK_ID))
                .andExpect(status().isNoContent());

        verify(taskService).delete(OWNER_ID, PROJECT_ID, TASK_ID);
    }

    private void stubCurrentUser() {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());
    }

    private TaskResponse response(String name, TaskStatus taskStatus) {
        return TaskResponse.builder()
                .id(TASK_ID)
                .projectId(PROJECT_ID)
                .name(name)
                .status(taskStatus)
                .build();
    }
}