package th.ac.kku.freelance_hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import th.ac.kku.freelance_hub.common.response.ApiErrorFactory;
import th.ac.kku.freelance_hub.exception.handling.ErrorContext;
import th.ac.kku.freelance_hub.exception.handling.ErrorHandlerChain;

/** Security uses the same descriptor and factory as MVC. */
@Component
public class SecurityErrorResponseWriter {
    private final ErrorHandlerChain chain;
    private final ApiErrorFactory factory;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    public SecurityErrorResponseWriter(ErrorHandlerChain chain, ApiErrorFactory factory) {
        this.chain = chain;
        this.factory = factory;
    }
    public void write(Exception exception, ErrorContext context, HttpServletResponse response) throws IOException {
        var descriptor = chain.resolve(exception, context);
        response.setStatus(descriptor.status().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        descriptor.headers().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        mapper.writeValue(response.getWriter(), factory.body(descriptor));
    }
}
