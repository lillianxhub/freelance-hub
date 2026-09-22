package th.ac.kku.freelance_hub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.service.ClientService;
import th.ac.kku.freelance_hub.service.UserService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

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
        when(userService.getCurrentUserEntity()).thenReturn(User.builder().id(7L).build());
        when(clientService.create(eq(7L), any(CreateClientRequest.class)))
            .thenReturn(ClientResponse.builder().id(clientId).name("Acme").build());

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"ownerId\":999}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/clients/" + clientId))
            .andExpect(jsonPath("$.name").value("Acme"));

        verify(clientService).create(eq(7L), any(CreateClientRequest.class));
    }

    @Test
    void createRejectsMissingNameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(clientService, never()).create(any(), any());
    }
}
