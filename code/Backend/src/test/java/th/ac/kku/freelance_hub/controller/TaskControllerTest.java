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
import static org.hamcrest.Matchers.nullValue;

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
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.TaskNotFoundException;
import th.ac.kku.freelance_hub.service.TaskService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.task.CreateTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.ReorderTaskRequest;
import th.ac.kku.freelance_hub.dto.request.task.UpdateTaskRequest;
import th.ac.kku.freelance_hub.dto.response.task.TaskResponse;
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
                        "/api/tasks/" + TASK_ID
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("สร้างงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.projectId")
                        .value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Design"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

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
    void listDefaultsToActiveAndReturnsCommonResponseWithPagination() throws Exception {
        stubCurrentUser();
        when(taskService.list(
                eq(OWNER_ID), eq(PROJECT_ID), eq(true), any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(response("Design", TaskStatus.OPEN)),
                PageRequest.of(0, 5),
                11
        ));

        mockMvc.perform(get(BASE, PROJECT_ID)
                        .param("page", "1")
                        .param("limit", "5")
                        .param("sort", "sortOrder,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงรายการงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Design"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(5))
                .andExpect(jsonPath("$.meta.total").value(11))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).list(
                eq(OWNER_ID),
                eq(PROJECT_ID),
                eq(true),
                org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 5
                                && pageable.getSort()
                                        .getOrderFor("sortOrder")
                                        .isDescending()
                )
        );
    }

    @Test
    void listAcceptsInactiveFilterAndReturnsEmptyArray() throws Exception {
        stubCurrentUser();
        when(taskService.list(
                eq(OWNER_ID), eq(PROJECT_ID), eq(false), any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.<TaskResponse>of(), PageRequest.of(0, 20), 0
        ));

        mockMvc.perform(get(BASE, PROJECT_ID).param("is_active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงรายการงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).list(
                eq(OWNER_ID), eq(PROJECT_ID), eq(false), any(Pageable.class)
        );
    }

    @Test
    void listAcceptsExplicitActiveFilter() throws Exception {
        stubCurrentUser();
        when(taskService.list(
                eq(OWNER_ID), eq(PROJECT_ID), eq(true), any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.<TaskResponse>of(), PageRequest.of(1, 5), 0
        ));

        mockMvc.perform(get(BASE, PROJECT_ID)
                        .param("is_active", "true")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(taskService).list(
                eq(OWNER_ID), eq(PROJECT_ID), eq(true),
                org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5)
        );
    }

    @Test
    void legacyNestedGetRouteIsDisabled() throws Exception {
        mockMvc.perform(get(BASE + "/{taskId}", PROJECT_ID, TASK_ID))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNull(result.getHandler()));

        verify(taskService, never()).getById(any(), any(), any());
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
    void legacyStartRouteIsDisabledButCompleteRemainsAvailable() throws Exception {
        stubCurrentUser();
        when(taskService.complete(OWNER_ID, PROJECT_ID, TASK_ID))
                .thenReturn(response("Design", TaskStatus.COMPLETED));

        mockMvc.perform(patch(
                        BASE + "/{taskId}/start", PROJECT_ID, TASK_ID
                ))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNull(result.getHandler()));

        mockMvc.perform(patch(
                        BASE + "/{taskId}/complete", PROJECT_ID, TASK_ID
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(taskService, never()).start(any(), any(), any());
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

    @Test
    void projectReorderReturnsCommonResponseAndPassesTaskAndPosition() throws Exception {
        stubCurrentUser();
        when(taskService.reorder(eq(OWNER_ID), eq(PROJECT_ID), eq(TASK_ID),
                any(ReorderTaskRequest.class)))
                .thenReturn(TaskResponse.builder().id(TASK_ID).projectId(PROJECT_ID)
                        .sortOrder(0).build());

        mockMvc.perform(patch(BASE + "/reorder", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + TASK_ID + "\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("เรียงลำดับงานย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.sortOrder").value(0))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(taskService).reorder(eq(OWNER_ID), eq(PROJECT_ID), eq(TASK_ID),
                org.mockito.ArgumentMatchers.<ReorderTaskRequest>argThat(
                        request -> Integer.valueOf(0).equals(request.getSortOrder())));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "{\"sortOrder\":0}",
            "{\"taskId\":\"00000000-0000-0000-0000-000000000001\"}",
            "{\"taskId\":\"00000000-0000-0000-0000-000000000001\",\"sortOrder\":-1}"
    })
    void projectReorderRejectsMissingFieldsAndNegativePosition(String json) throws Exception {
        mockMvc.perform(patch(BASE + "/reorder", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).reorder(any(), any(), any(), any());
    }

    @Test
    void projectReorderReturns404ForTaskOutsideProject() throws Exception {
        stubCurrentUser();
        when(taskService.reorder(eq(OWNER_ID), eq(PROJECT_ID), eq(TASK_ID),
                any(ReorderTaskRequest.class)))
                .thenThrow(new TaskNotFoundException(TASK_ID));

        mockMvc.perform(patch(BASE + "/reorder", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + TASK_ID + "\",\"sortOrder\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectReorderReturns400ForPositionOutsideList() throws Exception {
        stubCurrentUser();
        when(taskService.reorder(eq(OWNER_ID), eq(PROJECT_ID), eq(TASK_ID),
                any(ReorderTaskRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid sort order"));

        mockMvc.perform(patch(BASE + "/reorder", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + TASK_ID + "\",\"sortOrder\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void projectReorderReturns409WhenProjectStateForbidsEditing() throws Exception {
        stubCurrentUser();
        when(taskService.reorder(eq(OWNER_ID), eq(PROJECT_ID), eq(TASK_ID),
                any(ReorderTaskRequest.class)))
                .thenThrow(new IllegalStateException("Project cannot edit tasks"));

        mockMvc.perform(patch(BASE + "/reorder", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + TASK_ID + "\",\"sortOrder\":0}"))
                .andExpect(status().isConflict());
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
