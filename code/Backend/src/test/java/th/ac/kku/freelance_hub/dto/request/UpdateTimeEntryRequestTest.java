package th.ac.kku.freelance_hub.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("accepts clearTask as an update by itself")
    void acceptsClearTaskByItself() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .clearTask(true)
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("accepts taskId as an update by itself")
    void acceptsTaskIdByItself() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .taskId(UUID.randomUUID())
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("rejects taskId together with clearTask")
    void rejectsTaskIdTogetherWithClearTask() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder()
                .taskId(UUID.randomUUID())
                .clearTask(true)
                .build();

        Set<ConstraintViolation<UpdateTimeEntryRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Task ID and clear task cannot be used together");
    }

    @Test
    @DisplayName("rejects an update with no supplied fields")
    void rejectsEmptyUpdate() {
        UpdateTimeEntryRequest request = UpdateTimeEntryRequest.builder().build();

        Set<ConstraintViolation<UpdateTimeEntryRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("At least one field must be provided");
    }
}
