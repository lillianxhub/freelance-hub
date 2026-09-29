package th.ac.kku.freelance_hub.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResultTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                ApiResult.error("ไม่ถูกต้อง", "VALIDATION_ERROR", java.util.Map.of("email", "จำเป็น"))));

        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("message").asText()).isEqualTo("ไม่ถูกต้อง");
        assertThat(json.path("data").isNull()).isTrue();
        assertThat(json.path("meta").isNull()).isTrue();
        assertThat(json.path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(json.path("error").path("details").path("email").asText()).isEqualTo("จำเป็น");
    }
}
