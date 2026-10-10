package th.ac.kku.freelance_hub.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResultTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void successWithoutPaginationKeepsAllEnvelopeFields() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                ApiResult.success("สำเร็จ", "data")));

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("message").asText()).isEqualTo("สำเร็จ");
        assertThat(json.path("data").asText()).isEqualTo("data");
        assertThat(json.path("meta").isNull()).isTrue();
        assertThat(json.path("error").isNull()).isTrue();
    }

    @Test
    void paginatedSuccessIncludesPaginationMetadata() throws Exception {
        PaginationMeta meta = new PaginationMeta(1, 20, 125, 7);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                ApiResult.success("สำเร็จ", "data", meta)));

        assertThat(json.path("meta").path("page").asInt()).isEqualTo(1);
        assertThat(json.path("meta").path("limit").asInt()).isEqualTo(20);
        assertThat(json.path("meta").path("total").asLong()).isEqualTo(125);
        assertThat(json.path("meta").path("totalPages").asInt()).isEqualTo(7);
    }

    @Test
    void errorUsesTheSameEnvelope() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                new ApiErrorFactory().body(new th.ac.kku.freelance_hub.exception.handling.ErrorDescriptor(
                        HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "ไม่ถูกต้อง", java.util.Map.of("field", "name"), null, null))));

        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("message").asText()).isEqualTo("ไม่ถูกต้อง");
        assertThat(json.path("data").isNull()).isTrue();
        assertThat(json.path("meta").isNull()).isTrue();
        assertThat(json.path("error").path("code").asText()).isEqualTo("INVALID_ARGUMENT");
        assertThat(json.path("error").path("details").path("field").asText()).isEqualTo("name");
        assertThat(json.path("error").path("status").asInt()).isEqualTo(400);
        assertThat(java.time.Instant.parse(json.path("error").path("timestamp").asText())).isNotNull();
    }
}
