package th.ac.kku.freelance_hub.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;

class AddressTest {
    @Test
    void blankAddressIsNullAndNonblankFieldsAreTrimmed() {
        assertThat(Address.ofNullable(" ", null, "\t", "", null)).isNull();
        Address value = Address.ofNullable(" Street ", " Sub ", " District ", " Province ", " 40000 ");
        assertThat(value).isEqualTo(new Address("Street", "Sub", "District", "Province", "40000"));
        assertThat(value.hashCode()).isEqualTo(new Address("Street", "Sub", "District", "Province", "40000").hashCode());
        assertThat(value).isNotEqualTo(new Address("Other", "Sub", "District", "Province", "40000"));
        assertThat(Address.ofNullable(null, null, null, null, "40000")).isNotNull();
    }

    @Test
    void profileReplacesAndClearsEmbeddedAddress() {
        UserProfile profile = new UserProfile();
        assertThat(profile.getAddress()).isNull();
        profile.updateAddress(new Address("Street", "Sub", "District", "Province", "40000"));
        assertThat(profile.getSubdistrict()).isEqualTo("Sub");
        assertThat(profile.getDistrict()).isEqualTo("District");
        assertThat(profile.getProvince()).isEqualTo("Province");
        assertThat(profile.getPostalCode()).isEqualTo("40000");
        profile.updateAddress(null);
        assertThat(profile.getAddress()).isNull();
        assertThat(profile.getPostalCode()).isNull();
    }
}
