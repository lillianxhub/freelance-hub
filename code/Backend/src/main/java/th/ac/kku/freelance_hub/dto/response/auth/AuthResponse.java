package th.ac.kku.freelance_hub.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
/**
 * DTO for authentication response with JWT token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type;
    private Long expiresIn;
    private UserResponse user;

    /**
     * Create response with Bearer token
     */
    public static AuthResponse of(String token, Long expiresIn, UserResponse user) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
