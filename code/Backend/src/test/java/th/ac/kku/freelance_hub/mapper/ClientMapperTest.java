package th.ac.kku.freelance_hub.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientMapperTest {

    private final ClientMapper mapper = new ClientMapper();

    @Test
    void createsClientWithOwnerAndActiveStatus() {
        User owner = User.builder().id(7L).build();
        CreateClientRequest request = CreateClientRequest.builder()
            .name("  Alice  ")
            .companyName("  Acme  ")
            .email("  alice@example.com  ")
            .phone("0812345678")
            .taxId("  1234567890123  ")
            .build();

        Client client = mapper.toEntity(request, owner);

        assertThat(client.getOwner()).isSameAs(owner);
        assertThat(client.getName()).isEqualTo("Alice");
        assertThat(client.getCompanyName()).isEqualTo("Acme");
        assertThat(client.getEmail()).isEqualTo("alice@example.com");
        assertThat(client.getPhone()).isEqualTo("0812345678");
        assertThat(client.getTaxId()).isEqualTo("1234567890123");
        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }

    @Test
    void updatesOnlySuppliedFields() {
        User owner = User.builder().id(7L).build();
        Client client = new Client(owner, "Alice");
        client.updateDetails("Alice", "Acme", "alice@example.com", "0812345678", null, null, null);
        UpdateClientRequest request = UpdateClientRequest.builder()
            .name("  Alicia  ")
            .build();

        mapper.updateEntity(request, client);

        assertThat(client.getName()).isEqualTo("Alicia");
        assertThat(client.getCompanyName()).isEqualTo("Acme");
        assertThat(client.getEmail()).isEqualTo("alice@example.com");
        assertThat(client.getPhone()).isEqualTo("0812345678");
        assertThat(client.getOwner()).isSameAs(owner);
    }

    @Test
    void clearsOptionalFieldWhenEmptyStringIsSupplied() {
        Client client = new Client(User.builder().id(7L).build(), "Alice");
        client.updateDetails("Alice", "Acme", "alice@example.com", null, null, null, null);
        UpdateClientRequest request = UpdateClientRequest.builder()
            .companyName("")
            .build();

        mapper.updateEntity(request, client);

        assertThat(client.getCompanyName()).isNull();
        assertThat(client.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsBlankNameOnUpdate() {
        Client client = new Client(User.builder().id(7L).build(), "Alice");

        assertThatThrownBy(() -> mapper.updateEntity(
            UpdateClientRequest.builder().name("   ").build(), client
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name is required");
    }

    @Test
    void mapsEntityToResponseWithoutOwner() {
        Client client = new Client(User.builder().id(7L).build(), "Alice");
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-22T05:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-22T06:00:00Z");
        ReflectionTestUtils.setField(client, "id", id);
        ReflectionTestUtils.setField(client, "createdAt", createdAt);
        ReflectionTestUtils.setField(client, "updatedAt", updatedAt);
        ReflectionTestUtils.setField(client, "version", 2L);

        ClientResponse response = mapper.toResponse(client);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.getVersion()).isEqualTo(2L);
    }
}
