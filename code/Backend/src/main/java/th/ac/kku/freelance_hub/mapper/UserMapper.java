package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.entity.UserProfile;
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
                    .address(profile.getAddress())
                    .city(profile.getCity())
                    .country(profile.getCountry())
                    .postalCode(profile.getPostalCode())
                    .avatarUrl(profile.getProfileImageUrl())
                    .timezone(profile.getTimezone())
                    .dateFormat(profile.getDateFormat());
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
}
