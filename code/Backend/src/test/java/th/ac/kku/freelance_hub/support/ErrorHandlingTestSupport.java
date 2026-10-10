package th.ac.kku.freelance_hub.support;

import java.util.List;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.exception.GlobalExceptionHandler;
import th.ac.kku.freelance_hub.exception.handling.*;
import th.ac.kku.freelance_hub.security.SecurityErrorResponseWriter;

public final class ErrorHandlingTestSupport {
    private ErrorHandlingTestSupport() { }
    public static ErrorHandlerChain chain() {
        return new ErrorHandlerChain(List.of(new ApiExceptionHandler(), new ValidationErrorHandler(),
                new AuthenticationErrorHandler(), new RequestErrorHandler(), new UnknownErrorHandler()));
    }
    public static GlobalExceptionHandler advice() { return new GlobalExceptionHandler(new ApiErrorFactory(), chain()); }
    public static SecurityErrorResponseWriter security() { return new SecurityErrorResponseWriter(chain(), new ApiErrorFactory()); }
}
