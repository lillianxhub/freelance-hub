package th.ac.kku.freelance_hub.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Address;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;

/** Converts between Client entities and API request/response DTOs. */
@Component
public class ClientMapper {

    private final AddressMapper addressMapper = new AddressMapper();

    public Client toEntity(CreateClientRequest request, User owner) {
        Objects.requireNonNull(request, "request is required");

        Client client = new Client(owner, request.getName());
        client.updateDetailsWithAddress(
            request.getName(),
            request.getCompanyName(),
            request.getEmail(),
            request.getPhone(),
            addressMapper.toEntity(
                request.getAddress(),
                request.getSubdistrict(),
                request.getDistrict(),
                request.getProvince(),
                request.getPostalCode()),
            request.getTaxId(),
            request.getNotes()
        );
        return client;
    }

    /** Applies supplied PATCH fields; null leaves the stored value unchanged. */
    public void updateEntity(UpdateClientRequest request, Client client) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(client, "client is required");

        Address address = updateAddress(request, client.getAddress());

        client.updateDetailsWithAddress(
            keepIfNull(request.getName(), client.getName()),
            keepIfNull(request.getCompanyName(), client.getCompanyName()),
            keepIfNull(request.getEmail(), client.getEmail()),
            keepIfNull(request.getPhone(), client.getPhone()),
            address,
            keepIfNull(request.getTaxId(), client.getTaxId()),
            keepIfNull(request.getNotes(), client.getNotes())
        );
    }

    public ClientResponse toResponse(Client client) {
        Objects.requireNonNull(client, "client is required");

        ClientResponse.ClientResponseBuilder builder = ClientResponse.builder()
            .id(client.getId())
            .name(client.getName())
            .companyName(client.getCompanyName())
            .email(client.getEmail())
            .phone(client.getPhone())
            .taxId(client.getTaxId())
            .notes(client.getNotes())
            .status(client.getStatus())
            .createdAt(client.getCreatedAt())
            .updatedAt(client.getUpdatedAt())
            .version(client.getVersion());
        if (client.getAddress() != null) {
            builder.address(client.getAddress().getAddress())
                .subdistrict(client.getAddress().getSubdistrict())
                .district(client.getAddress().getDistrict())
                .province(client.getAddress().getProvince())
                .postalCode(client.getAddress().getPostalCode());
        }
        return builder.build();
    }

    private Address updateAddress(UpdateClientRequest request, Address current) {
        if (!addressMapper.hasAnyValue(
                request.getAddress(),
                request.getSubdistrict(),
                request.getDistrict(),
                request.getProvince(),
                request.getPostalCode())) {
            return current;
        }

        Address replacement = addressMapper.toEntity(
            request.getAddress(),
            request.getSubdistrict(),
            request.getDistrict(),
            request.getProvince(),
            request.getPostalCode());
        if (replacement == null) {
            return null;
        }
        if (current == null) {
            return replacement;
        }
        addressMapper.update(
            current,
            request.getAddress(),
            request.getSubdistrict(),
            request.getDistrict(),
            request.getProvince(),
            request.getPostalCode());
        return current;
    }

    private static String keepIfNull(String requested, String current) {
        return requested == null ? current : requested;
    }
}
