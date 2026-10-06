package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.mapper.ClientMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.ClientServiceImpl;
import th.ac.kku.freelance_hub.dto.request.client.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.client.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.client.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.client.ClientResponse;
@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock ClientRepository clientRepository;
    @Mock UserRepository userRepository;

    private ClientServiceImpl service;
    private User owner;
    private Client client;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(clientRepository, userRepository, new ClientMapper());
        owner = User.builder().id(OWNER_ID).build();
        client = new Client(owner, "Existing Client");
        clientId = UUID.randomUUID();
        ReflectionTestUtils.setField(client, "id", clientId);
    }

    @Test
    void createAssignsOwnerFromAuthenticatedId() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(OWNER_ID, CreateClientRequest.builder().name("New Client").build());

        verify(clientRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.getOwner() == owner && saved.getName().equals("New Client")));
    }

    @Test
    void getByIdLooksUpOnlyOwnedClient() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));

        assertThat(service.getById(OWNER_ID, clientId).getName()).isEqualTo("Existing Client");
        verify(clientRepository).findByIdAndOwnerId(clientId, OWNER_ID);
    }

    @Test
    void anotherOwnersClientIsReportedAsNotFound() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OWNER_ID, clientId,
            UpdateClientRequest.builder().name("Changed").build()))
            .isInstanceOf(ClientNotFoundException.class);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateKeepsUnspecifiedFields() {
        client.updateDetails("Existing Client", "Acme", null, null, null, null, null);
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        service.update(OWNER_ID, clientId, UpdateClientRequest.builder().name("New Name").build());

        assertThat(client.getName()).isEqualTo("New Name");
        assertThat(client.getCompanyName()).isEqualTo("Acme");
    }

    @Test
    void replaceClearsUnspecifiedOptionalFields() {
        client.updateDetails("Existing Client", "Acme", "old@example.com", null, null, null, null);
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        service.replace(OWNER_ID, clientId, CreateClientRequest.builder().name("New Name").build());

        assertThat(client.getName()).isEqualTo("New Name");
        assertThat(client.getCompanyName()).isNull();
        assertThat(client.getEmail()).isNull();
        verify(clientRepository).save(client);
    }

    @Test
    void replaceCannotChangeAnotherOwnersClient() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(OWNER_ID, clientId,
            CreateClientRequest.builder().name("New Name").build()))
            .isInstanceOf(ClientNotFoundException.class);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void softDeletePreservesClientStatus() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));

        service.softDelete(OWNER_ID, clientId);

        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.getIsActive()).isTrue();
        assertThat(client.getDeletedAt()).isNotNull();
        verify(clientRepository).save(client);
    }

    @Test
    void softDeleteAlsoPreservesInactiveStatus() {
        client.setActive(false);
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));

        service.softDelete(OWNER_ID, clientId);

        assertThat(client.getIsActive()).isFalse();
        assertThat(client.getDeletedAt()).isNotNull();
        verify(clientRepository).save(client);
    }

    @Test
    void changeStatusDoesNotSoftDeleteClient() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        var archived = service.changeStatus(OWNER_ID, clientId, false);
        assertThat(archived.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        assertThat(client.getIsActive()).isFalse();
        assertThat(client.getDeletedAt()).isNull();

        var active = service.changeStatus(OWNER_ID, clientId, true);
        assertThat(active.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.getIsActive()).isTrue();
        assertThat(client.getDeletedAt()).isNull();
    }

    @Test
    void deletedClientCannotBeReadOrChanged() {
        client.softDelete();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.getById(OWNER_ID, clientId))
            .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> service.changeStatus(OWNER_ID, clientId, false))
            .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> service.softDelete(OWNER_ID, clientId))
            .isInstanceOf(ClientNotFoundException.class);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void changeStatusCannotModifyAnotherOwnersClient() {
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(OWNER_ID, clientId, false))
            .isInstanceOf(ClientNotFoundException.class);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void listUsesSpecificationAndPagination() {
        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(client)));

        var result = service.list(OWNER_ID, ClientFilterRequest.builder().page(1).size(5).build());

        assertThat(result.getContent()).hasSize(1);
        verify(clientRepository).findAll(any(Specification.class),
            org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 5));
    }

    @Test
    void listUsesLimitInsteadOfLegacySizeWhenBothAreProvided() {
        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(client)));

        service.list(OWNER_ID, ClientFilterRequest.builder().page(1).size(5).limit(7).build());

        verify(clientRepository).findAll(any(Specification.class),
            org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 7));
    }

    @Test
    void listLoadsTrackedSecondsOnceForOnlyTheCurrentPageAndDefaultsMissingTotalsToZero() {
        Client second = new Client(owner, "Second Client");
        UUID secondId = UUID.randomUUID();
        ReflectionTestUtils.setField(second, "id", secondId);
        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(client, second), PageRequest.of(0, 2), 3));
        when(clientRepository.sumTrackedSecondsByClientIds(OWNER_ID, List.of(clientId, secondId)))
                .thenReturn(List.of(new ClientRepository.ClientTrackedSeconds() {
                    public UUID getClientId() { return clientId; }
                    public Long getTotalSeconds() { return 5401L; }
                }));

        var page = service.list(OWNER_ID, ClientFilterRequest.builder().limit(2).build());

        assertThat(page.getContent()).extracting(ClientResponse::getTotalTrackedSeconds)
                .containsExactly(5401L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        verify(clientRepository).sumTrackedSecondsByClientIds(OWNER_ID, List.of(clientId, secondId));
    }

    @Test
    void emptyClientPageDoesNotQueryTrackedSeconds() {
        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(service.list(OWNER_ID, ClientFilterRequest.builder().build()).getContent()).isEmpty();

        verify(clientRepository, never()).sumTrackedSecondsByClientIds(eq(OWNER_ID), anyList());
    }

    @Test
    void invalidSortFieldIsRejected() {
        assertThatThrownBy(() -> service.list(OWNER_ID,
            ClientFilterRequest.builder().sortBy("owner.id").build()))
            .isInstanceOf(IllegalArgumentException.class);
        verify(clientRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
