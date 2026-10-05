package th.ac.kku.freelance_hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;


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
                .forEach(response -> {
                    if (response.getValue().getContent() == null) {
                        response.getValue().setContent(new Content());
                    }
                    response.getValue().getContent().computeIfAbsent("application/json", ignored ->
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiResult")));
                    response.getValue().getContent().values().forEach(mediaType -> {
                        Schema<?> base = mediaType.getSchema();
                        if (base == null) base = new Schema<>().$ref("#/components/schemas/ApiResult");
                        BooleanSchema failure = new BooleanSchema();
                        failure.setEnum(java.util.List.of(false));
                        failure.setExample(false);
                        mediaType.setSchema(new ComposedSchema()
                                .addAllOfItem(base)
                                .addAllOfItem(new ObjectSchema().addProperty("success", failure)));
                    });
                });
    }

    private boolean isErrorStatus(String responseCode) {
        return responseCode.matches("[45]\\d{2}");
    }
}
