package th.ac.kku.freelance_hub.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void ownerScopedLookupAndPagesNeverReturnAnotherUsersClients() {
        User owner = saveUser("client-owner@example.com");
        User otherOwner = saveUser("other-client-owner@example.com");
        Client alpha = clientRepository.saveAndFlush(new Client(owner, "Alpha"));
        Client beta = clientRepository.saveAndFlush(new Client(owner, "Beta"));
        Client other = clientRepository.saveAndFlush(new Client(otherOwner, "Aardvark"));

        assertThat(clientRepository.findByIdAndOwnerId(alpha.getId(), owner.getId()))
            .contains(alpha);
        assertThat(clientRepository.findByIdAndOwnerId(other.getId(), owner.getId()))
            .isEmpty();
        assertThat(clientRepository.existsByIdAndOwnerId(beta.getId(), owner.getId()))
            .isTrue();
        assertThat(clientRepository.existsByIdAndOwnerId(other.getId(), owner.getId()))
            .isFalse();

        PageRequest firstPage = PageRequest.of(0, 1, Sort.by("name"));
        var pageZero = clientRepository.findAllByOwnerId(owner.getId(), firstPage);
        var pageOne = clientRepository.findAllByOwnerId(owner.getId(), firstPage.next());

        assertThat(pageZero.getTotalElements()).isEqualTo(2);
        assertThat(pageZero.getTotalPages()).isEqualTo(2);
        assertThat(pageZero.getContent()).extracting(Client::getId)
            .containsExactly(alpha.getId());
        assertThat(pageOne.getContent()).extracting(Client::getId)
            .containsExactly(beta.getId());
    }

    @Test
    void statusFilterKeepsArchivedClientsWithinTheirOwner() {
        User owner = saveUser("status-owner@example.com");
        User otherOwner = saveUser("other-status-owner@example.com");
        Client active = clientRepository.saveAndFlush(new Client(owner, "Active Client"));
        Client archived = new Client(owner, "Archived Client");
        archived.archive();
        archived = clientRepository.saveAndFlush(archived);
        Client otherArchived = new Client(otherOwner, "Other Archived Client");
        otherArchived.archive();
        clientRepository.saveAndFlush(otherArchived);

        PageRequest page = PageRequest.of(0, 10);
        assertThat(clientRepository.findAllByOwnerIdAndStatus(
            owner.getId(), ClientStatus.ARCHIVED, page
        ).getContent()).extracting(Client::getId).containsExactly(archived.getId());
        assertThat(clientRepository.findAllByOwnerIdAndStatus(
            owner.getId(), ClientStatus.ACTIVE, page
        ).getContent()).extracting(Client::getId).containsExactly(active.getId());
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
            .email(email)
            .passwordHash("test-hash")
            .build());
    }
}
