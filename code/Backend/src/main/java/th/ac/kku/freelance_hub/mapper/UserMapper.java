package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.domain.entity.Address;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
import th.ac.kku.freelance_hub.dto.request.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.UserResponse;

/**
 * Mapper for User and UserProfile entities
 */
@Component
public class UserMapper {

    private final AddressMapper addressMapper = new AddressMapper();

    /**
     * Convert User entity to UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt());

        // Add profile information if exists
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            builder.displayName(profile.getDisplayName())
                    .firstName(profile.getFirstName())
                    .lastName(profile.getLastName())
                    .phone(profile.getPhone())
                    .avatarUrl(profile.getProfileImageUrl())
                    .timezone(profile.getTimezone())
                    .dateFormat(profile.getDateFormat());
            if (profile.getAddress() != null) {
                builder.address(profile.getAddress().getAddress())
                        .subdistrict(profile.getAddress().getSubdistrict())
                        .district(profile.getAddress().getDistrict())
                        .province(profile.getAddress().getProvince())
                        .postalCode(profile.getAddress().getPostalCode());
            }
        }

        return builder.build();
    }

    /**
     * Create UserProfile from RegisterRequest
     */
    public UserProfile toProfile(RegisterRequest request) {
        return UserProfile.builder()
                .displayName(request.getDisplayName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .dateFormat("YYYY-MM-DD")
                .build();
    }

    /** Applies non-null profile and flat address fields from a PATCH request. */
    public void updateProfile(UpdateUserProfileRequest request, UserProfile profile) {
        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone().trim());
        }
        if (request.getDateFormat() != null) {
            profile.setDateFormat(request.getDateFormat().trim());
        }
        if (request.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(request.getProfileImageUrl().trim());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }

        if (addressMapper.hasAnyValue(
                request.getAddress(),
                request.getSubdistrict(),
                request.getDistrict(),
                request.getProvince(),
                request.getPostalCode())) {
            Address current = profile.getAddress();
            Address replacement = addressMapper.toEntity(
                    request.getAddress(),
                    request.getSubdistrict(),
                    request.getDistrict(),
                    request.getProvince(),
                    request.getPostalCode());
            if (replacement == null) {
                profile.setAddress(null);
            } else if (current == null) {
                profile.setAddress(replacement);
            } else {
                addressMapper.update(
                        current,
                        request.getAddress(),
                        request.getSubdistrict(),
                        request.getDistrict(),
                        request.getProvince(),
                        request.getPostalCode());
            }
        }
    }
}
