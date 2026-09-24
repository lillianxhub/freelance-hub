package th.ac.kku.freelance_hub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String jwtSecret = "your-very-secure-secret-key-for-testing-must-be-at-least-256-bits-long-freelance-hub";
    private final Long jwtExpiration = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", jwtExpiration);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        // Given
        String email = "test@example.com";

        // When
        String token = jwtTokenProvider.generateToken(email);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract email from valid token")
    void shouldExtractEmailFromValidToken() {
        // Given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(email);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("Should validate valid token successfully")
    void shouldValidateValidTokenSuccessfully() {
        // Given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(email);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for malformed token")
    void shouldReturnFalseForMalformedToken() {
        // Given
        String malformedToken = "not-a-jwt-token-at-all";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty token")
    void shouldReturnFalseForEmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return correct expiration time")
    void shouldReturnCorrectExpirationTime() {
        // When
        Long expirationTime = jwtTokenProvider.getExpirationTime();

        // Then
        assertThat(expirationTime).isEqualTo(jwtExpiration);
    }

    @Test
    @DisplayName("Should generate different tokens for different emails")
    void shouldGenerateDifferentTokensForDifferentEmails() {
        // Given
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // When
        String token1 = jwtTokenProvider.generateToken(email1);
        String token2 = jwtTokenProvider.generateToken(email2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtTokenProvider.getEmailFromToken(token1)).isEqualTo(email1);
        assertThat(jwtTokenProvider.getEmailFromToken(token2)).isEqualTo(email2);
    }

    @Test
    @DisplayName("Should generate different tokens for same email at different times")
    void shouldGenerateDifferentTokensForSameEmailAtDifferentTimes() throws InterruptedException {
        // Given
        String email = "test@example.com";

        // When
        String token1 = jwtTokenProvider.generateToken(email);
        Thread.sleep(1000); // Wait 1 second to ensure different issuedAt timestamp
        String token2 = jwtTokenProvider.generateToken(email);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtTokenProvider.getEmailFromToken(token1)).isEqualTo(email);
        assertThat(jwtTokenProvider.getEmailFromToken(token2)).isEqualTo(email);
    }
}
