package th.ac.kku.freelance_hub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.exception.handling.ErrorContext;
import th.ac.kku.freelance_hub.exception.handling.ErrorHandlerChain;

/** Single MVC entry point. All mappings live in the chain. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final ApiErrorFactory factory;
    private final ErrorHandlerChain chain;
    public GlobalExceptionHandler(ApiErrorFactory factory, ErrorHandlerChain chain) {
        this.factory = factory;
        this.chain = chain;
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handle(Exception exception, HttpServletRequest request) {
        return factory.response(chain.resolve(exception, ErrorContext.mvc(request.getRequestURI())));
    }
}
