package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import th.ac.kku.freelance_hub.domain.entity.Address;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.dto.request.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.UserResponse;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void mapsNormalizedAddressAsFlatUserResponseFields() {
        UserProfile profile = UserProfile.builder()
            .displayName("Test User")
            .address(new Address("99 ถนนมิตรภาพ", "ในเมือง", "เมืองขอนแก่น", "ขอนแก่น", "40000"))
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
    }

    @Test
    void updatesProfileAddressWithoutAcceptingAnOwnerId() {
        UserProfile profile = UserProfile.builder()
            .displayName("Test User")
            .address(new Address("เดิม", "เดิม", "เดิม", "เดิม", "10000"))
            .build();

        mapper.updateProfile(UpdateUserProfileRequest.builder()
            .province("ขอนแก่น")
            .postalCode("40000")
            .build(), profile);

        assertThat(profile.getAddress().getAddress()).isEqualTo("เดิม");
        assertThat(profile.getAddress().getProvince()).isEqualTo("ขอนแก่น");
        assertThat(profile.getAddress().getPostalCode()).isEqualTo("40000");
    }
}
