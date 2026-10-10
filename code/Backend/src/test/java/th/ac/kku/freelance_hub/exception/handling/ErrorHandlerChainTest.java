package th.ac.kku.freelance_hub.exception.handling;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import th.ac.kku.freelance_hub.exception.*;
import th.ac.kku.freelance_hub.support.ErrorHandlingTestSupport;

class ErrorHandlerChainTest {
    private final ErrorContext mvc = ErrorContext.mvc("/api/test");

    @Test
    void sortsHandlersAndStopsAtFirstMatch() {
        ErrorHandler first = mock(ErrorHandler.class);
        ErrorHandler later = mock(ErrorHandler.class);
        when(first.getOrder()).thenReturn(1);
        when(later.getOrder()).thenReturn(2);
        var expected = ErrorDescriptor.of(HttpStatus.BAD_REQUEST, "TEST_ERROR", "ข้อมูลไม่ถูกต้อง");
        var exception = new Exception();
        when(first.handle(exception, mvc)).thenReturn(Optional.of(expected));
        var chain = new ErrorHandlerChain(List.of(new UnknownErrorHandler(), later, first));
        assertThat(chain.resolve(exception, mvc)).isSameAs(expected);
        verify(later, never()).handle(any(), any());
    }

    @Test
    void rejectsDuplicateOrdersAndInvalidFallbacksAtStartup() {
        assertThatThrownBy(() -> new ErrorHandlerChain(List.of(new ApiExceptionHandler())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ErrorHandlerChain(List.of(new UnknownErrorHandler(), new UnknownErrorHandler())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ErrorHandlerChain(List.of(new ApiExceptionHandler(), new ApiExceptionHandler(), new UnknownErrorHandler())))
                .isInstanceOf(IllegalStateException.class);
        ErrorHandler afterFallback = mock(ErrorHandler.class);
        when(afterFallback.getOrder()).thenReturn(600);
        assertThatThrownBy(() -> new ErrorHandlerChain(List.of(new UnknownErrorHandler(), afterFallback)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void preservesApiMetadataAndRateLimitHeader() {
        var descriptor = ErrorHandlingTestSupport.chain().resolve(new LoginRateLimitedException(17), mvc);
        assertThat(descriptor.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(descriptor.code()).isEqualTo("LOGIN_RATE_LIMITED");
        assertThat(descriptor.details()).containsEntry("retryAfterSeconds", 17L);
        assertThat(descriptor.headers().getFirst("Retry-After")).isEqualTo("17");
    }

    @Test
    void preservesFrameworkStatusAndAllowHeaderWithoutLeakingReasons() {
        var chain = ErrorHandlingTestSupport.chain();
        var method = chain.resolve(new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST")), mvc);
        assertThat(method.status()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(method.headers().getAllow()).containsExactlyInAnyOrder(org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.POST);
        assertThat(chain.resolve(new HttpMediaTypeNotSupportedException("secret"), mvc).status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(chain.resolve(new ResponseStatusException(HttpStatus.GONE, "SQL secret"), mvc).status()).isEqualTo(HttpStatus.GONE);
        assertThat(chain.resolve(new ResponseStatusException(HttpStatus.GONE, "SQL secret"), mvc).message()).doesNotContain("SQL");
    }

    @Test
    void rawProgrammingFailuresAndSecurityDependencyFailuresReturn500() {
        var chain = ErrorHandlingTestSupport.chain();
        for (Exception ex : List.of(new IllegalArgumentException("SQL password"), new IllegalStateException("token"), new NullPointerException("internal"))) {
            var result = chain.resolve(ex, mvc);
            assertThat(result.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.details()).isNull();
            assertThat(result.message()).isEqualTo("เกิดข้อผิดพลาดภายในระบบ");
        }
        assertThat(chain.resolve(new BadCredentialsException("dependency"),
                new ErrorContext(ErrorContext.Source.FILTER_DEPENDENCY, "/api/users/me")).status())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(chain.resolve(new InvalidCredentialsException(), mvc).status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exceptionContractValidatesMetadataAndDefensivelyCopiesDetails() {
        var details = new java.util.HashMap<String, Object>();
        details.put("field", "name");
        ApiException ex = new TestException(HttpStatus.BAD_REQUEST, "TEST_ERROR", "ข้อมูลไม่ถูกต้อง", details);
        details.put("field", "password");
        assertThat(ex.getDetails()).containsExactlyEntriesOf(Map.of("field", "name"));
        assertThatThrownBy(() -> ex.getDetails().put("field", "email")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new TestException(null, "TEST_ERROR", "ข้อความ", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TestException(HttpStatus.BAD_REQUEST, "", "ข้อความ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TestException(HttpStatus.BAD_REQUEST, "lowercase", "ข้อความ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TestException(HttpStatus.BAD_REQUEST, "TEST_ERROR", " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TestException(HttpStatus.OK, "TEST_ERROR", "ข้อความ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static class TestException extends ApiException {
        TestException(HttpStatus status, String code, String message, Map<String, Object> details) {
            super(status, code, message, details);
        }
    }
}
