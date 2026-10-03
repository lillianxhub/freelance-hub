package th.ac.kku.freelance_hub.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.client.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.client.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.client.ClientResponse;
/** Converts between Client entities and API request/response DTOs. */
@Component
public class ClientMapper {
    public Client toEntity(CreateClientRequest request, User owner) {
        Objects.requireNonNull(request, "request is required");
        Client client = new Client(owner, request.getName());
        client.updateDetailsWithAddress(request.getName(), request.getCompanyName(), request.getEmail(),
            request.getPhone(), request.getAddress(), request.getSubdistrict(), request.getDistrict(),
            request.getProvince(), request.getPostalCode(), request.getTaxId(), request.getNotes());
        return client;
    }

    /** Replaces all editable fields; omitted optional values are cleared. */
    public void replaceEntity(CreateClientRequest request, Client client) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(client, "client is required");
        client.updateDetailsWithAddress(request.getName(), request.getCompanyName(), request.getEmail(),
            request.getPhone(), request.getAddress(), request.getSubdistrict(), request.getDistrict(),
            request.getProvince(), request.getPostalCode(), request.getTaxId(), request.getNotes());
    }

    public void updateEntity(UpdateClientRequest request, Client client) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(client, "client is required");
        client.updateDetailsWithAddress(
            keepIfNull(request.getName(), client.getName()),
            keepIfNull(request.getCompanyName(), client.getCompanyName()),
            keepIfNull(request.getEmail(), client.getEmail()),
            keepIfNull(request.getPhone(), client.getPhone()),
            keepIfNull(request.getAddress(), client.getAddress()),
            keepIfNull(request.getSubdistrict(), client.getSubdistrict()),
            keepIfNull(request.getDistrict(), client.getDistrict()),
            keepIfNull(request.getProvince(), client.getProvince()),
            keepIfNull(request.getPostalCode(), client.getPostalCode()),
            keepIfNull(request.getTaxId(), client.getTaxId()),
            keepIfNull(request.getNotes(), client.getNotes()));
    }

    public ClientResponse toResponse(Client client) {
        Objects.requireNonNull(client, "client is required");
        return ClientResponse.builder()
            .id(client.getId()).name(client.getName()).companyName(client.getCompanyName())
            .email(client.getEmail()).phone(client.getPhone()).address(client.getAddress())
            .subdistrict(client.getSubdistrict()).district(client.getDistrict()).province(client.getProvince())
            .postalCode(client.getPostalCode()).taxId(client.getTaxId()).notes(client.getNotes())
            .status(client.getStatus()).isActive(client.getIsActive())
            .createdAt(client.getCreatedAt()).updatedAt(client.getUpdatedAt())
            .version(client.getVersion()).build();
    }

    private static String keepIfNull(String requested, String current) {
        return requested == null ? current : requested;
    }
}
