package th.ac.kku.freelance_hub.service.impl;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.exception.UserNotFoundException;
import th.ac.kku.freelance_hub.mapper.ClientMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.ClientService;

@Service
public class ClientServiceImpl implements ClientService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "name", "companyName", "email", "createdAt", "updatedAt");

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    public ClientServiceImpl(
            ClientRepository clientRepository,
            UserRepository userRepository,
            ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.clientMapper = clientMapper;
    }

    @Override
    @Transactional
    public ClientResponse create(UUID ownerId, CreateClientRequest request) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId));
        Client client = clientMapper.toEntity(request, owner);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(UUID ownerId, UUID clientId) {
        return clientMapper.toResponse(findOwnedClient(ownerId, clientId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> list(UUID ownerId, ClientFilterRequest filter) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(filter, "filter is required");
        int pageSize = filter.getLimit() != null ? filter.getLimit() : filter.getSize();
        if (filter.getPage() < 0 || filter.getSize() < 1 || filter.getSize() > 100
                || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Invalid client page, size, or limit");
        }
        if (!SORT_FIELDS.contains(filter.getSortBy()) || filter.getDirection() == null) {
            throw new IllegalArgumentException("Invalid client sort field or direction");
        }

        Specification<Client> specification = (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("owner").get("id"), ownerId);
            predicate = cb.and(predicate, cb.isNull(root.get("deletedAt")));
            if (filter.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(
                    root.get("isActive"), filter.getStatus() == ClientStatus.ACTIVE));
            }
            String search = filter.getSearch();
            if (search != null && !search.isBlank()) {
                // Prefix matching avoids the leading wildcard that prevents ordinary index use.
                String pattern = escapeLike(search.trim()) + "%";
                Predicate match = cb.or(
                        cb.like(root.get("name"), pattern, '\\'),
                        cb.like(root.get("companyName"), pattern, '\\'),
                        cb.like(root.get("email"), pattern, '\\'),
                        cb.like(root.get("phone"), pattern, '\\'));
                match = cb.or(
                        match,
                        cb.like(root.get("address"), pattern, '\\'),
                        cb.like(root.get("subdistrict"), pattern, '\\'),
                        cb.like(root.get("district"), pattern, '\\'),
                        cb.like(root.get("province"), pattern, '\\'),
                        cb.like(root.get("postalCode"), pattern, '\\'));
                predicate = cb.and(predicate, match);
            }
            return predicate;
        };

        PageRequest pageable = PageRequest.of(
                filter.getPage(), pageSize,
                Sort.by(filter.getDirection(), filter.getSortBy()));
        return clientRepository.findAll(specification, pageable).map(clientMapper::toResponse);
    }

    @Override
    @Transactional
    public ClientResponse replace(UUID ownerId, UUID clientId, CreateClientRequest request) {
        Client client = findOwnedClient(ownerId, clientId);
        clientMapper.replaceEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse update(UUID ownerId, UUID clientId, UpdateClientRequest request) {
        Client client = findOwnedClient(ownerId, clientId);
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse changeStatus(UUID ownerId, UUID clientId, boolean isActive) {
        Client client = findOwnedClient(ownerId, clientId);
        client.setActive(isActive);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void softDelete(UUID ownerId, UUID clientId) {
        Client client = findOwnedClient(ownerId, clientId);
        client.softDelete();
        clientRepository.save(client);
    }

    private Client findOwnedClient(UUID ownerId, UUID clientId) {
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(clientId, "clientId is required");
        return clientRepository.findByIdAndOwnerId(clientId, ownerId)
                .filter(client -> client.getDeletedAt() == null)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
