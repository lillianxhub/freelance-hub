package th.ac.kku.freelance_hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import th.ac.kku.freelance_hub.common.response.ApiResult;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearer-jwt";

        return new OpenAPI()
                .info(new Info()
                        .title("Freelance Hub API")
                        .version("0.0.1")
                        .description("RESTful API for managing freelance projects, clients, tasks, and time tracking")
                        .contact(new Contact()
                                .name("Freelance Hub Team")
                                .email("support@freelance-hub.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    @Bean
    public OpenApiCustomizer errorResponseExampleCustomizer() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .flatMap(operation -> operation.getResponses().entrySet().stream())
                .filter(response -> isErrorStatus(response.getKey()))
                .filter(response -> response.getValue().getContent() != null)
                .forEach(response -> response.getValue().getContent().values().forEach(mediaType ->
                        mediaType.setExample(ApiResult.error(
                                response.getValue().getDescription(),
                                "HTTP_" + response.getKey(),
                                null))));
    }

    private boolean isErrorStatus(String responseCode) {
        return responseCode.matches("[45]\\d{2}");
    }
}
