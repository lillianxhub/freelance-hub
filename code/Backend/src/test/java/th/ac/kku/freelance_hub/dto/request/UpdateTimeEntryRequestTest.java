package th.ac.kku.freelance_hub.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
@DisplayName("UpdateTimeEntryRequest validation tests")
class UpdateTimeEntryRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("accepts a complete replacement using end time")
    void acceptsCompleteReplacementUsingEndTime() {
        Instant startedAt = Instant.parse("2026-09-27T09:00:00Z");
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .projectId(UUID.randomUUID())
                .startedAt(startedAt)
                .endedAt(startedAt.plusSeconds(90))
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("accepts a complete replacement using duration seconds")
    void acceptsCompleteReplacementUsingDurationSeconds() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .projectId(UUID.randomUUID())
                .startedAt(Instant.parse("2026-09-27T09:00:00Z"))
                .durationSeconds(90L)
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("rejects both end time and duration seconds")
    void rejectsBothEndTimeAndDurationSeconds() {
        Instant startedAt = Instant.parse("2026-09-27T09:00:00Z");
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .projectId(UUID.randomUUID())
                .startedAt(startedAt)
                .endedAt(startedAt.plusSeconds(90))
                .durationSeconds(90L)
                .build();

        Set<ConstraintViolation<UpdateTimeEntryRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง");
    }

    @Test
    @DisplayName("requires project, start time, and a time input")
    void rejectsIncompleteReplacement() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder().build();

        Set<ConstraintViolation<UpdateTimeEntryRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "กรุณาระบุโปรเจกต์",
                        "กรุณาระบุเวลาเริ่มต้น",
                        "กรุณาระบุเวลาสิ้นสุดหรือระยะเวลาอย่างใดอย่างหนึ่ง"
                );
    }
}
