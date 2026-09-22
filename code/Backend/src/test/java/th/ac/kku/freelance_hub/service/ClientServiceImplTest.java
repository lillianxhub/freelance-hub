package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.mapper.ClientMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.ClientServiceImpl;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock ClientRepository clientRepository;
    @Mock UserRepository userRepository;

    private ClientServiceImpl service;
    private User owner;
    private Client client;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(clientRepository, userRepository, new ClientMapper());
        owner = User.builder().id(7L).build();
        client = new Client(owner, "Existing Client");
        clientId = UUID.randomUUID();
    }

    @Test
    void createAssignsOwnerFromAuthenticatedId() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(7L, CreateClientRequest.builder().name("New Client").build());

        verify(clientRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.getOwner() == owner && saved.getName().equals("New Client")));
    }

    @Test
    void getByIdLooksUpOnlyOwnedClient() {
        when(clientRepository.findByIdAndOwnerId(clientId, 7L)).thenReturn(Optional.of(client));

        assertThat(service.getById(7L, clientId).getName()).isEqualTo("Existing Client");
        verify(clientRepository).findByIdAndOwnerId(clientId, 7L);
    }

    @Test
    void anotherOwnersClientIsReportedAsNotFound() {
        when(clientRepository.findByIdAndOwnerId(clientId, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, clientId,
            UpdateClientRequest.builder().name("Changed").build()))
            .isInstanceOf(ClientNotFoundException.class);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateKeepsUnspecifiedFields() {
        client.updateDetails("Existing Client", "Acme", null, null, null, null, null);
        when(clientRepository.findByIdAndOwnerId(clientId, 7L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        service.update(7L, clientId, UpdateClientRequest.builder().name("New Name").build());

        assertThat(client.getName()).isEqualTo("New Name");
        assertThat(client.getCompanyName()).isEqualTo("Acme");
    }

    @Test
    void archivePreservesClientAndChangesStatus() {
        when(clientRepository.findByIdAndOwnerId(clientId, 7L)).thenReturn(Optional.of(client));

        service.archive(7L, clientId);

        assertThat(client.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        verify(clientRepository).save(client);
    }

    @Test
    void listUsesSpecificationAndPagination() {
        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(client)));

        var result = service.list(7L, ClientFilterRequest.builder().page(1).size(5).build());

        assertThat(result.getContent()).hasSize(1);
        verify(clientRepository).findAll(any(Specification.class),
            org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                pageable.getPageNumber() == 1 && pageable.getPageSize() == 5));
    }

    @Test
    void invalidSortFieldIsRejected() {
        assertThatThrownBy(() -> service.list(7L,
            ClientFilterRequest.builder().sortBy("owner.id").build()))
            .isInstanceOf(IllegalArgumentException.class);
        verify(clientRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
