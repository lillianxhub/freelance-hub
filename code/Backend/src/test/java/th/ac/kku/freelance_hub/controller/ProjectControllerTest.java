package th.ac.kku.freelance_hub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import th.ac.kku.freelance_hub.dto.request.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.CreateProjectRequest;
import th.ac.kku.freelance_hub.dto.response.ProjectResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.service.ProjectService;
import th.ac.kku.freelance_hub.service.UserService;

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
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    void createUsesCurrentUserAndReturns201() throws Exception {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());
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
                .andExpect(jsonPath("$.name").value("Website"));

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
                .andExpect(status().isBadRequest());

        verify(projectService, never()).create(any(), any());
    }

    @Test
    void listPassesFiltersAndPaginationToService() throws Exception {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());
        when(projectService.list(
                eq(OWNER_ID),
                eq("web"),
                eq(ProjectStatus.ACTIVE),
                eq(CLIENT_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
            List.<ProjectResponse>of(),
            PageRequest.of(1, 5),
            0
        ));

        mockMvc.perform(get("/api/projects")
                        .param("search", "web")
                        .param("status", "ACTIVE")
                        .param("clientId", CLIENT_ID.toString())
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk());

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
                )
        );
    }

    @Test
    void getByIdReturns404WhenProjectIsNotFound() throws Exception {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());
        when(projectService.getById(OWNER_ID, PROJECT_ID))
                .thenThrow(new ProjectNotFoundException(PROJECT_ID));

        mockMvc.perform(get("/api/projects/{id}", PROJECT_ID))
                .andExpect(status().isNotFound());

        verify(projectService).getById(OWNER_ID, PROJECT_ID);
    }

    @Test
    void invalidStatusChangeReturns409() throws Exception {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());
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
                .andExpect(status().isConflict());
    }

    @Test
    void archiveReturns204() throws Exception {
        when(userService.getCurrentUserEntity())
                .thenReturn(User.builder().id(OWNER_ID).build());

        mockMvc.perform(delete("/api/projects/{id}", PROJECT_ID))
                .andExpect(status().isNoContent());

        verify(projectService).archive(OWNER_ID, PROJECT_ID);
    }

}