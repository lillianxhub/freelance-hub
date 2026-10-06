package th.ac.kku.freelance_hub.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;

class ApiErrorContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void unexpectedFailureUsesTraceableCommonEnvelope() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory()))
                .addFilters(new RequestTraceFilter())
                .build();

        var response = mvc.perform(get("/api/fail"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse();
        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("error").path("code").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.path("error").path("status").asInt()).isEqualTo(500);
        assertThat(body.path("error").path("traceId").asText())
                .isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
        UUID.fromString(body.path("error").path("traceId").asText());
        Instant.parse(body.path("error").path("timestamp").asText());
    }

    @RestController
    static class FailingController {
        @GetMapping("/api/fail")
        void fail() {
            throw new RuntimeException("boom");
        }
    }
}
