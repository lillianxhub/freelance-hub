package th.ac.kku.freelance_hub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.common.response.RequestTraceFilter;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.exception.ClientExceptionHandler;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.service.ClientService;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
import th.ac.kku.freelance_hub.dto.request.client.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.client.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.client.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.client.ClientResponse;
@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock ClientService clientService;
    @Mock CurrentUserProvider userService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientController(clientService, userService))
            .setControllerAdvice(new ClientExceptionHandler(new ApiErrorFactory()))
            .addFilters(new RequestTraceFilter())
            .setValidator(validator)
            .build();
        clientId = UUID.randomUUID();
    }

    @Test
    void createUsesCurrentUserAndReturnsCreated() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.create(eq(OWNER_ID), any(CreateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("Acme").build());

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"ownerId\":999}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/clients/" + clientId))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("สร้างลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data.id").value(clientId.toString()))
            .andExpect(jsonPath("$.data.name").value("Acme"))
            .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService).create(eq(OWNER_ID), any(CreateClientRequest.class));
    }

    @Test
    void createRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(traceableError(400, "VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error.fieldErrors.name").exists());

        verify(clientService, never()).create(any(), any());
    }

    @Test
    void listUsesCurrentUserAndDefaultFilters() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        doReturn(new PageImpl<>(List.of(ClientResponse.builder().name("Acme").build()),
            PageRequest.of(0, 20), 1))
            .when(clientService).list(eq(OWNER_ID), any(ClientFilterRequest.class));

        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("ดึงรายชื่อลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data[0].name").value("Acme"))
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.limit").value(20))
            .andExpect(jsonPath("$.meta.total").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService).list(eq(OWNER_ID), org.mockito.ArgumentMatchers.argThat(filter ->
            filter.getPage() == 1 && filter.getSize() == 20
                && filter.getStatus() == null && filter.getSortBy().equals("name")));
    }

    @Test
    void listBindsFiltersAndPagination() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        doReturn(new PageImpl<ClientResponse>(List.of(), PageRequest.of(2, 5), 0))
            .when(clientService).list(eq(OWNER_ID), any(ClientFilterRequest.class));

        mockMvc.perform(get("/api/clients")
                .param("status", "ARCHIVED")
                .param("search", "Acme")
                .param("page", "3")
                .param("size", "5")
                .param("sortBy", "createdAt")
                .param("direction", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.page").value(3))
            .andExpect(jsonPath("$.meta.limit").value(5))
            .andExpect(jsonPath("$.meta.total").value(0));

        verify(clientService).list(eq(OWNER_ID), org.mockito.ArgumentMatchers.argThat(filter ->
            filter.getStatus() == ClientStatus.ARCHIVED
                && filter.getSearch().equals("Acme")
                && filter.getPage() == 3 && filter.getSize() == 5
                && filter.getSortBy().equals("createdAt")
                && filter.getDirection().isDescending()));
    }

    @Test
    void listRejectsInvalidPageSizeBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/clients").param("page", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/clients").param("size", "101"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).list(any(), any());
    }

    @Test
    void listBindsLimitAlongsideLegacySize() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        doReturn(new PageImpl<ClientResponse>(List.of(), PageRequest.of(0, 7), 0))
            .when(clientService).list(eq(OWNER_ID), any(ClientFilterRequest.class));

        mockMvc.perform(get("/api/clients")
                .param("page", "1")
                .param("size", "5")
                .param("limit", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.limit").value(7));

        verify(clientService).list(eq(OWNER_ID), org.mockito.ArgumentMatchers.argThat(filter ->
            filter.getPage() == 1 && filter.getSize() == 5 && filter.getLimit() == 7));
    }

    @Test
    void listRejectsInvalidLimitBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/clients").param("limit", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/clients").param("limit", "101"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).list(any(), any());
    }

    @Test
    void getByIdUsesCurrentUserAndReturnsClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.getById(OWNER_ID, clientId))
            .thenReturn(ClientResponse.builder().id(clientId).name("Acme").build());

        mockMvc.perform(get("/api/clients/{id}", clientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("ดึงข้อมูลลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data.id").value(clientId.toString()))
            .andExpect(jsonPath("$.data.name").value("Acme"))
            .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService).getById(OWNER_ID, clientId);
    }

    @Test
    void getByIdReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.getById(OWNER_ID, clientId)).thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(get("/api/clients/{id}", clientId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Client not found with id: " + clientId))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
            .andExpect(traceableError(404, "CLIENT_NOT_FOUND"))
            .andExpect(jsonPath("$.error.fieldErrors").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService).getById(OWNER_ID, clientId);
    }

    @Test
    void replaceUsesCurrentUserAndReturnsWrappedClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.replace(eq(OWNER_ID), eq(clientId), any(CreateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("New Name").build());

        mockMvc.perform(put("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\",\"ownerId\":999}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("อัปเดตข้อมูลลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data.id").value(clientId.toString()))
            .andExpect(jsonPath("$.data.name").value("New Name"));

        verify(clientService).replace(eq(OWNER_ID), eq(clientId),
            org.mockito.ArgumentMatchers.argThat(request -> request.getName().equals("New Name")));
    }

    @Test
    void replaceRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(put("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).replace(any(), any(), any());
    }

    @Test
    void replaceReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.replace(eq(OWNER_ID), eq(clientId), any(CreateClientRequest.class)))
            .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(put("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\"}"))
            .andExpect(status().isNotFound());

        verify(clientService).replace(eq(OWNER_ID), eq(clientId), any(CreateClientRequest.class));
    }

    @Test
    void updateUsesCurrentUserAndReturnsUpdatedClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("New Name").build());

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\",\"ownerId\":999}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("อัปเดตข้อมูลลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data.id").value(clientId.toString()))
            .andExpect(jsonPath("$.data.name").value("New Name"))
            .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

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
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class)))
            .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(patch("/api/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\"}"))
            .andExpect(status().isNotFound());

        verify(clientService).update(eq(OWNER_ID), eq(clientId), any(UpdateClientRequest.class));
    }

    @Test
    void changeStatusUsesCurrentUserAndBooleanFlag() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.changeStatus(OWNER_ID, clientId, false))
            .thenReturn(ClientResponse.builder().id(clientId).status(ClientStatus.ARCHIVED)
                .isActive(false).build());

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("อัปเดตสถานะลูกค้าสำเร็จ"))
            .andExpect(jsonPath("$.data.id").value(clientId.toString()))
            .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.data.isActive").value(false));

        verify(clientService).changeStatus(OWNER_ID, clientId, false);
    }

    @Test
    void changeStatusRejectsMissingBooleanFlag() throws Exception {
        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).changeStatus(any(), any(), anyBoolean());
    }

    @Test
    void changeStatusReturnsNotFoundForAnotherOwnersClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.changeStatus(OWNER_ID, clientId, true))
            .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":true}"))
            .andExpect(status().isNotFound());

        verify(clientService).changeStatus(OWNER_ID, clientId, true);
    }

    @Test
    void softDeleteUsesCurrentUserAndReturnsNoContent() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);

        mockMvc.perform(delete("/api/clients/{id}", clientId))
            .andExpect(status().isNoContent());

        verify(clientService).softDelete(OWNER_ID, clientId);
    }

    @Test
    void softDeleteReturnsNotFoundForMissingOrOtherOwnersClient() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        doThrow(new ClientNotFoundException(clientId)).when(clientService).softDelete(OWNER_ID, clientId);

        mockMvc.perform(delete("/api/clients/{id}", clientId))
            .andExpect(status().isNotFound());

        verify(clientService).softDelete(OWNER_ID, clientId);
    }
    @Test
    void invalidClientIdUsesTraceableBadRequestWithParameterDetails() throws Exception {
        mockMvc.perform(get("/api/clients/not-a-uuid"))
            .andExpect(traceableError(400, "INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.error.details.field").value("id"))
            .andExpect(jsonPath("$.error.fieldErrors").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService, never()).getById(any(), any());
    }

    @Test
    void malformedJsonUsesTraceableBadRequest() throws Exception {
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":"))
            .andExpect(traceableError(400, "INVALID_REQUEST_BODY"))
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.error.fieldErrors").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService, never()).create(any(), any());
    }

    @Test
    void unsupportedIncludeUsesTraceableBadRequestBeforeCallingService() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);

        mockMvc.perform(get("/api/clients/{id}", clientId).param("include", "tasks"))
            .andExpect(traceableError(400, "INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService, never()).getById(any(), any());
        verify(clientService, never()).getById(any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void invalidPageAndSortUseFieldErrorsRatherThanDetails() throws Exception {
        mockMvc.perform(get("/api/clients").param("page", "0"))
            .andExpect(traceableError(400, "VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.fieldErrors.page").isNotEmpty())
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get("/api/clients").param("sortBy", "passwordHash"))
            .andExpect(traceableError(400, "VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.fieldErrors.sortBy").isNotEmpty())
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()));

        verify(clientService, never()).list(any(), any());
    }

    @Test
    void statusConflictUsesTraceableConflictResponse() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.changeStatus(OWNER_ID, clientId, false))
            .thenThrow(new IllegalStateException("Project status conflict"));

        mockMvc.perform(patch("/api/clients/{id}/status", clientId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\":false}"))
            .andExpect(traceableError(409, "INVALID_STATE"))
            .andExpect(jsonPath("$.message").value("Project status conflict"));
    }

    @Test
    void unexpectedFailureUsesTraceableErrorWithoutLeakingInternalMessage() throws Exception {
        when(userService.currentUserId()).thenReturn(OWNER_ID);
        when(clientService.getById(OWNER_ID, clientId))
            .thenThrow(new RuntimeException("Internal database diagnostic"));

        mockMvc.perform(get("/api/clients/{id}", clientId))
            .andExpect(traceableError(500, "INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("เกิดข้อผิดพลาดภายในระบบ"))
            .andExpect(jsonPath("$.error.details").value(org.hamcrest.Matchers.nullValue()));
    }

    /** Assert the shared contract using the real Client advice and request trace filter. */
    private ResultMatcher traceableError(int expectedStatus, String expectedCode) {
        return result -> {
            var response = result.getResponse();
            var body = objectMapper.readTree(response.getContentAsByteArray());
            var error = body.path("error");
            assertThat(response.getStatus()).isEqualTo(expectedStatus);
            assertThat(body.path("success").isBoolean()).isTrue();
            assertThat(body.path("success").booleanValue()).isFalse();
            assertThat(body.path("message").asText()).isNotBlank();
            assertThat(body.path("data").isNull()).isTrue();
            assertThat(body.path("meta").isNull()).isTrue();
            assertThat(error.path("code").asText()).isEqualTo(expectedCode);
            assertThat(error.path("status").intValue()).isEqualTo(expectedStatus);
            String timestamp = error.path("timestamp").asText();
            assertThat(timestamp).endsWith("Z");
            Instant.parse(timestamp);
            String traceId = response.getHeader(RequestTraceFilter.HEADER);
            assertThat(traceId).isNotBlank();
            UUID.fromString(traceId);
            assertThat(error.path("traceId").asText()).isEqualTo(traceId);
        };
    }
}
