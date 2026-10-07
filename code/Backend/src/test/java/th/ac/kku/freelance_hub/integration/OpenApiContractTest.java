package th.ac.kku.freelance_hub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.context.WebApplicationContext;
import th.ac.kku.freelance_hub.common.response.ApiResult;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestBodiesAndSuccessResponsesMatchEveryControllerReturnType() throws Exception {
        JsonNode document = document();
        JsonNode paths = document.path("paths");
        JsonNode schemas = document.path("components").path("schemas");
        Set<String> checked = new HashSet<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!handler.getBeanType().getPackageName().equals("th.ac.kku.freelance_hub.controller")) {
                continue;
            }

            RequestMappingInfo mapping = entry.getKey();
            assertThat(mapping.getPathPatternsCondition()).isNotNull();
            for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
                for (var method : mapping.getMethodsCondition().getMethods()) {
                    String label = method + " " + path;
                    checked.add(label);
                    JsonNode operation = paths.path(path).path(method.name().toLowerCase(Locale.ROOT));
                    assertThat(operation.isMissingNode()).as(label).isFalse();

                    boolean hasRequestBody = false;
                    for (Parameter parameter : handler.getMethod().getParameters()) {
                        if (parameter.isAnnotationPresent(RequestBody.class)) {
                            hasRequestBody = true;
                            JsonNode requestSchema = firstContentSchema(operation.path("requestBody"));
                            assertThat(requestSchema.path("$ref").asText())
                                    .as(label + " request body")
                                    .isEqualTo(ref(parameter.getType()));
                        }
                    }
                    if (!hasRequestBody) {
                        assertThat(operation.path("requestBody").isMissingNode())
                                .as(label + " must have no request body")
                                .isTrue();
                    }

                    ResolvableType bodyType = ResolvableType.forMethodReturnType(handler.getMethod())
                            .as(ResponseEntity.class).getGeneric(0);
                    assertThat(bodyType.resolve()).as(label + " Java return type").isNotNull();
                    JsonNode responses = operation.path("responses");
                    boolean hasSuccess = false;
                    for (Iterator<Map.Entry<String, JsonNode>> it = responses.fields(); it.hasNext();) {
                        Map.Entry<String, JsonNode> response = it.next();
                        if (!response.getKey().matches("2\\d\\d")) {
                            continue;
                        }
                        hasSuccess = true;
                        JsonNode content = response.getValue().path("content");
                        if (bodyType.resolve() == Void.class) {
                            assertThat(content.isMissingNode() || content.isEmpty())
                                    .as(label + " " + response.getKey() + " must have no body")
                                    .isTrue();
                            continue;
                        }

                        JsonNode responseSchema = firstContentSchema(response.getValue());
                        String responseRef = responseSchema.path("$ref").asText();
                        assertThat(responseRef).as(label + " response schema").startsWith("#/components/schemas/");
                        JsonNode resolved = schemas.path(responseRef.substring(responseRef.lastIndexOf('/') + 1));
                        assertThat(resolved.isMissingNode()).as(label + " referenced response schema").isFalse();

                        if (bodyType.resolve() == ApiResult.class) {
                            assertThat(resolved.path("properties").has("success")).as(label).isTrue();
                            assertThat(resolved.path("properties").has("message")).as(label).isTrue();
                            ResolvableType dataType = bodyType.getGeneric(0);
                            JsonNode data = resolved.path("properties").path("data");
                            if (dataType.resolve() == java.util.List.class) {
                                assertThat(data.path("type").asText()).as(label).isEqualTo("array");
                                assertThat(data.path("items").path("$ref").asText())
                                        .as(label + " array item")
                                        .isEqualTo(ref(dataType.getGeneric(0).resolve()));
                            } else if (dataType.resolve() != Void.class) {
                                assertThat(data.path("$ref").asText())
                                        .as(label + " data")
                                        .isEqualTo(ref(dataType.resolve()));
                            }
                        } else {
                            assertThat(responseRef).as(label + " direct response")
                                    .isEqualTo(ref(bodyType.resolve()));
                        }
                    }
                    assertThat(hasSuccess).as(label + " success response").isTrue();
                }
            }
        }
        Set<String> documented = new HashSet<>();
        for (Iterator<Map.Entry<String, JsonNode>> pathIt = paths.fields(); pathIt.hasNext();) {
            Map.Entry<String, JsonNode> path = pathIt.next();
            for (Iterator<Map.Entry<String, JsonNode>> methodIt = path.getValue().fields(); methodIt.hasNext();) {
                Map.Entry<String, JsonNode> method = methodIt.next();
                documented.add(method.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey());
            }
        }
        assertThat(checked).containsExactlyInAnyOrderElementsOf(documented);
    }

    @Test
    void reportFiltersAreDocumentedAsFlatQueryParameters() throws Exception {
        JsonNode paths = document().path("paths");
        for (String route : new String[] {"summary", "work-trend", "distribution", "work-pattern", "projects"}) {
            JsonNode parameters = paths.path("/api/reports/" + route).path("get").path("parameters");
            Set<String> names = new HashSet<>();
            for (JsonNode parameter : parameters) {
                assertThat(parameter.path("in").asText()).isEqualTo("query");
                names.add(parameter.path("name").asText());
            }
            assertThat(names).as(route).contains("from", "to", "clientId", "projectId")
                    .doesNotContain("filter", "request");
            if (route.equals("projects")) {
                assertThat(names).contains("page", "limit", "sortBy", "direction");
            }
        }
    }

    @Test
    void includeFieldsAndErrorResponsesDocumentTheirConditionalShape() throws Exception {
        JsonNode document = document();
        JsonNode schemas = document.path("components").path("schemas");
        assertThat(schemas.path("ApiResult").path("properties").path("data")
                .path("nullable").asBoolean()).isTrue();
        assertThat(schemas.path("ClientResponse").path("properties").path("projects")
                .path("description").asText()).contains("include=projects.tasks");
        assertThat(schemas.path("ProjectListItemResponse").path("properties").path("tasks")
                .path("description").asText()).contains("GET /api/projects/{id}");

        JsonNode paths = document.path("paths");
        for (Iterator<Map.Entry<String, JsonNode>> pathIt = paths.fields(); pathIt.hasNext();) {
            Map.Entry<String, JsonNode> path = pathIt.next();
            for (Iterator<Map.Entry<String, JsonNode>> operationIt = path.getValue().fields(); operationIt.hasNext();) {
                Map.Entry<String, JsonNode> operation = operationIt.next();
                for (Iterator<Map.Entry<String, JsonNode>> responseIt = operation.getValue().path("responses").fields(); responseIt.hasNext();) {
                    Map.Entry<String, JsonNode> response = responseIt.next();
                    if (!response.getKey().matches("[45]\\d\\d")) {
                        continue;
                    }
                    JsonNode schema = firstContentSchema(response.getValue());
                    String label = operation.getKey() + " " + path.getKey() + " " + response.getKey();
                    assertThat(schema.path("allOf").get(0).path("$ref").asText())
                            .as(label).isEqualTo(ref(ApiResult.class));
                    assertThat(schema.path("allOf").get(1).path("properties")
                            .path("success").path("enum").get(0).asBoolean())
                            .as(label).isFalse();
                }
            }
        }
    }

    private JsonNode document() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static JsonNode firstContentSchema(JsonNode responseOrRequest) {
        JsonNode content = responseOrRequest.path("content");
        Iterator<JsonNode> mediaTypes = content.elements();
        assertThat(mediaTypes.hasNext()).as("documented content").isTrue();
        return mediaTypes.next().path("schema");
    }

    private static String ref(Class<?> type) {
        return "#/components/schemas/" + type.getSimpleName();
    }
}
