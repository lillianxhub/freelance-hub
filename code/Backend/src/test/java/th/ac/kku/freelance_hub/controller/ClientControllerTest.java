package th.ac.kku.freelance_hub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.service.ClientService;
import th.ac.kku.freelance_hub.service.UserService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock ClientService clientService;
    @Mock UserService userService;

    private MockMvc mockMvc;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientController(clientService, userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
        clientId = UUID.randomUUID();
    }

    @Test
    void createUsesCurrentUserAndReturnsCreated() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        when(clientService.create(eq(OWNER_ID), any(CreateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("Acme").build());

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"ownerId\":999}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/clients/" + clientId))
            .andExpect(jsonPath("$.name").value("Acme"));

        verify(clientService).create(eq(OWNER_ID), any(CreateClientRequest.class));
    }

    @Test
    void createRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).create(any(), any());
    }

    @Test
    void listUsesCurrentUserAndDefaultFilters() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        doReturn(new PageImpl<>(List.of(ClientResponse.builder().name("Acme").build()),
            PageRequest.of(0, 20), 1))
            .when(clientService).list(eq(OWNER_ID), any(ClientFilterRequest.class));

        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Acme"));

        verify(clientService).list(eq(OWNER_ID), org.mockito.ArgumentMatchers.argThat(filter ->
            filter.getPage() == 0 && filter.getSize() == 20
                && filter.getStatus() == null && filter.getSortBy().equals("name")));
    }

    @Test
    void listBindsFiltersAndPagination() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        doReturn(new PageImpl<ClientResponse>(List.of(), PageRequest.of(2, 5), 0))
            .when(clientService).list(eq(OWNER_ID), any(ClientFilterRequest.class));

        mockMvc.perform(get("/api/clients")
                .param("status", "ARCHIVED")
                .param("search", "Acme")
                .param("page", "2")
                .param("size", "5")
                .param("sortBy", "createdAt")
                .param("direction", "DESC"))
            .andExpect(status().isOk());

        verify(clientService).list(eq(OWNER_ID), org.mockito.ArgumentMatchers.argThat(filter ->
            filter.getStatus() == ClientStatus.ARCHIVED
                && filter.getSearch().equals("Acme")
                && filter.getPage() == 2 && filter.getSize() == 5
                && filter.getSortBy().equals("createdAt")
                && filter.getDirection().isDescending()));
    }

    @Test
    void listRejectsInvalidPageSizeBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/clients").param("size", "101"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).list(any(), any());
    }

    @Test
    void getByIdUsesCurrentUserAndReturnsClient() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        when(clientService.getById(OWNER_ID, clientId))
            .thenReturn(ClientResponse.builder().id(clientId).name("Acme").build());

        mockMvc.perform(get("/api/clients/{id}", clientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clientId.toString()))
            .andExpect(jsonPath("$.name").value("Acme"));

        verify(clientService).getById(OWNER_ID, clientId);
    }

    @Test
    void getByIdReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        when(clientService.getById(OWNER_ID, clientId)).thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(get("/api/clients/{id}", clientId))
            .andExpect(status().isNotFound());

        verify(clientService).getById(OWNER_ID, clientId);
    }

    @Test
    void updateUsesCurrentUserAndReturnsUpdatedClient() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        when(clientService.update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("New Name").build());

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\",\"ownerId\":999}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"));

        verify(clientService).update(eq(OWNER_ID), eq(clientId),
            org.mockito.ArgumentMatchers.argThat(request ->
                request.getName().equals("New Name")));
    }

    @Test
    void updateRejectsBlankNameBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \"}"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).update(any(), any(), any());
    }

    @Test
    void updateReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        when(clientService.update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class)))
            .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\"}"))
            .andExpect(status().isNotFound());

        verify(clientService).update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class));
    }

    @Test
    void archiveUsesCurrentUserAndReturnsNoContent() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());

        mockMvc.perform(delete("/api/clients/{id}", clientId))
            .andExpect(status().isNoContent());

        verify(clientService).archive(OWNER_ID, clientId);
    }

    @Test
    void archiveReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(OWNER_ID).build());
        doThrow(new ClientNotFoundException(clientId)).when(clientService).archive(OWNER_ID, clientId);

        mockMvc.perform(delete("/api/clients/{id}", clientId))
            .andExpect(status().isNotFound());

        verify(clientService).archive(OWNER_ID, clientId);
    }
}
