package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.domain.valueobject.Address;
import th.ac.kku.freelance_hub.dto.request.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.UserResponse;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void mapsNormalizedAddressAsFlatUserResponseFields() {
        UserProfile profile = UserProfile.builder()
            .displayName("Test User")
            .addressDetails(new Address("99 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000"))
            .taxId("0105559999999")
            .build();
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .profile(profile)
            .build();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.getAddress()).isEqualTo("99 ถนนมิตรภาพ");
        assertThat(response.getSubdistrict()).isEqualTo("ในเมือง");
        assertThat(response.getDistrict()).isEqualTo("เมืองขอนแก่น");
        assertThat(response.getProvince()).isEqualTo("ขอนแก่น");
        assertThat(response.getPostalCode()).isEqualTo("40000");
        assertThat(response.getTaxId()).isEqualTo("0105559999999");
    }

    @Test
    void updatesProfileAddressWithoutAcceptingAnOwnerId() {
        UserProfile profile = UserProfile.builder()
            .displayName("Test User")
            .addressDetails(new Address("เดิม", "เดิม", "เดิม", "เดิม", "10000"))
            .taxId("เดิม")
            .build();

        mapper.updateProfile(UpdateUserProfileRequest.builder()
            .province("ขอนแก่น")
            .postalCode("40000")
            .taxId("0105559999999")
            .build(), profile);

        assertThat(profile.getAddress()).isEqualTo("เดิม");
        assertThat(profile.getProvince()).isEqualTo("ขอนแก่น");
        assertThat(profile.getPostalCode()).isEqualTo("40000");
        assertThat(profile.getTaxId()).isEqualTo("0105559999999");
    }
}
