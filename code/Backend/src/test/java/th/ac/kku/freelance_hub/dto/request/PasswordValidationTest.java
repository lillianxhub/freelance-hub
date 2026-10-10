package th.ac.kku.freelance_hub.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.stream.Stream;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.request.auth.ChangePasswordRequest;

class PasswordValidationTest {
    static Stream<Arguments> passwords() {
        return Stream.of(
                Arguments.of("password", true),
                Arguments.of("a".repeat(72), true),
                Arguments.of("a".repeat(73), false),
                Arguments.of("ก".repeat(24), true),
                Arguments.of("ก".repeat(25), false),
                Arguments.of("😀".repeat(18), true),
                Arguments.of("😀".repeat(19), false),
                Arguments.of("short", false),
                Arguments.of("", false),
                Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("passwords")
    void validatesRegistrationAndPasswordChange(String password, boolean valid) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var registration = RegisterRequest.builder()
                    .email("owner@example.com").displayName("Owner").password(password).build();
            var change = ChangePasswordRequest.builder()
                    .oldPassword("old-password").newPassword(password).build();
            assertThat(validator.validate(registration).isEmpty()).isEqualTo(valid);
            assertThat(validator.validate(change).isEmpty()).isEqualTo(valid);
        }
    }

    @Test
    void byteLimitProvidesThaiValidationMessage() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var request = ChangePasswordRequest.builder()
                    .oldPassword("old-password").newPassword("ก".repeat(25)).build();
            assertThat(factory.getValidator().validate(request))
                    .extracting(violation -> violation.getMessage())
                    .containsExactly("รหัสผ่านใหม่ต้องไม่เกิน 72 ไบต์ UTF-8");
        }
    }
}
