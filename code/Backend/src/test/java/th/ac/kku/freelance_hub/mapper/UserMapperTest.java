package th.ac.kku.freelance_hub.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

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
            .address("99 ถนนมิตรภาพ")
            .subdistrict("ในเมือง")
            .district("เมืองขอนแก่น")
            .province("ขอนแก่น")
            .postalCode("40000")
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
            .address("เดิม")
            .subdistrict("เดิม")
            .district("เดิม")
            .province("เดิม")
            .postalCode("10000")
            .build();

        mapper.updateProfile(UpdateUserProfileRequest.builder()
            .province("ขอนแก่น")
            .postalCode("40000")
            .build(), profile);

        assertThat(profile.getAddress()).isEqualTo("เดิม");
        assertThat(profile.getProvince()).isEqualTo("ขอนแก่น");
        assertThat(profile.getPostalCode()).isEqualTo("40000");
    }
}
