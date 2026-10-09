package th.ac.kku.freelance_hub.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import th.ac.kku.freelance_hub.exception.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;

class ApiErrorContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void unexpectedFailureUsesTraceableCommonEnvelope() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport.advice())
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

    @ParameterizedTest
    @CsvSource({"400,INVALID_ARGUMENT", "401,INVALID_CREDENTIALS", "403,ORIGIN_NOT_ALLOWED", "404,CLIENT_NOT_FOUND",
            "409,TIMER_ALREADY_RUNNING", "429,LOGIN_RATE_LIMITED", "500,INTERNAL_SERVER_ERROR"})
    void apiErrorsKeepStatusCodesThaiMessagesAndMetadata(int expectedStatus, String code) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport.advice())
                .addFilters(new RequestTraceFilter()).build();
        var response = mvc.perform(get("/api/error/{status}", expectedStatus))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse();
        var body = mapper.readTree(response.getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("meta").isNull()).isTrue();
        assertThat(body.path("message").asText()).containsPattern("[ก-๙]");
        assertThat(body.path("error").path("status").asInt()).isEqualTo(expectedStatus);
        assertThat(body.path("error").path("code").asText()).isEqualTo(code);
        assertThat(body.path("error").path("details").isNull() || body.path("error").path("details").isObject()).isTrue();
        assertThat(body.path("error").path("traceId").asText()).isEqualTo(response.getHeader(RequestTraceFilter.HEADER));
        Instant.parse(body.path("error").path("timestamp").asText());
        if (expectedStatus == 429) assertThat(response.getHeader("Retry-After")).isEqualTo("15");
        if (expectedStatus == 500) assertThat(response.getContentAsString()).doesNotContain("SQL", "secret");
    }

    @Test
    void methodMediaAndValidationUseTheSameContract() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport.advice())
                .addFilters(new RequestTraceFilter()).build();
        var method = mvc.perform(post("/api/fail")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andReturn().getResponse();
        assertThat(mapper.readTree(method.getContentAsString()).path("error").path("status").asInt()).isEqualTo(405);
        var media = mvc.perform(post("/api/validate").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType()).andReturn().getResponse();
        assertThat(mapper.readTree(media.getContentAsString()).path("error").path("status").asInt()).isEqualTo(415);
        var validation = mvc.perform(post("/api/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse();
        var body = mapper.readTree(validation.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.path("error").path("fieldErrors").path("name").asText()).isEqualTo("กรุณาระบุชื่อ");
        assertThat(body.path("error").path("details").isNull()).isTrue();
    }

    @RestController
    static class FailingController {
        @GetMapping("/api/error/{status}")
        void error(@PathVariable int status) {
            switch (status) {
                case 400 -> throw new InvalidArgumentException("กรุณาระบุชื่อ", java.util.Map.of("field", "name"));
                case 401 -> throw new InvalidCredentialsException();
                case 403 -> throw new OriginNotAllowedException();
                case 404 -> throw new ClientNotFoundException(UUID.randomUUID());
                case 409 -> throw new TimerAlreadyRunningException();
                case 429 -> throw new LoginRateLimitedException(15);
                default -> throw new RuntimeException("SQL secret");
            }
        }
        @PostMapping(value = "/api/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validate(@Valid @RequestBody Input input) { }
        record Input(@NotBlank(message = "กรุณาระบุชื่อ") String name) { }

        @GetMapping("/api/fail")
        void fail() {
            throw new RuntimeException("boom");
        }
    }
}
