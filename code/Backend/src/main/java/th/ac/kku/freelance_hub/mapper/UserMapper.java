package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;
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

    /**
     * Convert User entity to UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt());

        // Add profile information if exists
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            builder.displayName(profile.getDisplayName())
                    .firstName(profile.getFirstName())
                    .lastName(profile.getLastName())
                    .phone(profile.getPhone())
                    .address(profile.getAddress())
                    .subdistrict(profile.getSubdistrict())
                    .district(profile.getDistrict())
                    .province(profile.getProvince())
                    .postalCode(profile.getPostalCode())
                    .dateFormat(profile.getDateFormat())
                    .bio(profile.getBio());
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
        if (request.getDateFormat() != null) {
            profile.setDateFormat(request.getDateFormat().trim());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }

        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress().trim());
        }
        if (request.getSubdistrict() != null) {
            profile.setSubdistrict(request.getSubdistrict().trim());
        }
        if (request.getDistrict() != null) {
            profile.setDistrict(request.getDistrict().trim());
        }
        if (request.getProvince() != null) {
            profile.setProvince(request.getProvince().trim());
        }
        if (request.getPostalCode() != null) {
            profile.setPostalCode(request.getPostalCode().trim());
        }
    }
}
