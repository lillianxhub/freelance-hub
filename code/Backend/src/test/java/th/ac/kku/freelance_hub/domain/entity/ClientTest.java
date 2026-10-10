package th.ac.kku.freelance_hub.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.exception.InvalidArgumentException;

class ClientTest {
    private final User owner = User.builder().email("owner@example.com").build();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void requiresNameForCreationAndUpdate(String name) {
        assertThatThrownBy(() -> new Client(owner, name)).isInstanceOf(InvalidArgumentException.class);
        Client client = new Client(owner, "Client");
        assertThatThrownBy(() -> client.updateDetails(name, null, null, null, null, null, null))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(client.getName()).isEqualTo("Client");
    }

    @Test
    void detailsNormalizeAndClearOptionalFields() {
        Client client = new Client(owner, " Client ");
        client.updateDetailsWithAddress(" Updated ", " Company ", " email@example.com ", " 0812345678 ",
                " Street ", " Sub ", " District ", " Province ", " 40000 ", " 123 ", " Notes ");
        assertThat(client.getName()).isEqualTo("Updated");
        assertThat(client.getCompanyName()).isEqualTo("Company");
        assertThat(client.getAddress()).isEqualTo("Street");
        assertThat(client.getPostalCode()).isEqualTo("40000");
        client.updateDetailsWithAddress("Updated", " ", null, "", null, null, null, null, null, null, null);
        assertThat(client.getCompanyName()).isNull();
        assertThat(client.getPhone()).isNull();
        assertThat(client.getAddressDetails()).isNull();
        assertThat(client.getTaxId()).isNull();
    }

    @Test
    void archiveCanBeReversedWithoutClearingSoftDeletion() {
        Client client = new Client(owner, "Client");
        client.setActive(false);
        assertThat(client.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        client.setActive(true);
        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        client.softDelete();
        assertThat(client.getDeletedAt()).isNotNull();
    }

    @Test
    void userProfileLinksBackToItsOwner() {
        UserProfile profile = new UserProfile();
        owner.setProfile(profile);
        assertThat(profile.getUser()).isSameAs(owner);
        owner.setProfile(null);
        assertThat(owner.getProfile()).isNull();
    }
}
