package th.ac.kku.freelance_hub.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;

/** Converts between Client entities and API request/response DTOs. */
@Component
public class ClientMapper {

    public Client toEntity(CreateClientRequest request, User owner) {
        Objects.requireNonNull(request, "request is required");

        Client client = new Client(owner, request.getName());
        client.updateDetails(
            request.getName(),
            request.getCompanyName(),
            request.getEmail(),
            request.getPhone(),
            request.getAddress(),
            request.getTaxId(),
            request.getNotes()
        );
        return client;
    }

    /** Applies supplied PATCH fields; null leaves the stored value unchanged. */
    public void updateEntity(UpdateClientRequest request, Client client) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(client, "client is required");

        client.updateDetails(
            keepIfNull(request.getName(), client.getName()),
            keepIfNull(request.getCompanyName(), client.getCompanyName()),
            keepIfNull(request.getEmail(), client.getEmail()),
            keepIfNull(request.getPhone(), client.getPhone()),
            keepIfNull(request.getAddress(), client.getAddress()),
            keepIfNull(request.getTaxId(), client.getTaxId()),
            keepIfNull(request.getNotes(), client.getNotes())
        );
    }

    public ClientResponse toResponse(Client client) {
        Objects.requireNonNull(client, "client is required");

        return ClientResponse.builder()
            .id(client.getId())
            .name(client.getName())
            .companyName(client.getCompanyName())
            .email(client.getEmail())
            .phone(client.getPhone())
            .address(client.getAddress())
            .taxId(client.getTaxId())
            .notes(client.getNotes())
            .status(client.getStatus())
            .createdAt(client.getCreatedAt())
            .updatedAt(client.getUpdatedAt())
            .version(client.getVersion())
            .build();
    }

    private static String keepIfNull(String requested, String current) {
        return requested == null ? current : requested;
    }
}
