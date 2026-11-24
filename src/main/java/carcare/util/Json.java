package CarCareBackend.src.main.java.carcare.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.io.IOException;
import java.util.stream.Collectors;

/**
 * One shared, strict JSON mapper for the whole app.
 * Bad input ("latitude":"abc", unknown fields, wrong types) becomes an IllegalArgumentException,
 * which the router turns into HTTP 400.
 */
public class Json {

    private static final ObjectMapper mapper=JsonMapper.builder()
            .addModule(new JavaTimeModule())                       // LocalDateTime support
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    public static String toJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert to JSON", e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            T value = mapper.readValue(json, type);
            if (value == null) {
                throw new IllegalArgumentException("Request body is required");
            }
            return value;
        } catch (JsonProcessingException e) {
            throw invalid(e);
        }
    }

    public static JsonNode objectBody(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            JsonNode node = mapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("Request body must be a JSON object");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw invalid(e);
        }
    }

    // applies a partial update on top of an existing object (PATCH)
    public static <T> T merge(T target, JsonNode patch) {
        try {
            return mapper.readerForUpdating(target).readValue(patch);
        } catch (JsonProcessingException e) {
            throw invalid(e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON request body");
        }
    }

    // a message that is safe to show the client
    private static IllegalArgumentException invalid(JsonProcessingException e) {
        if (e instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            return new IllegalArgumentException("Invalid value or unknown field: " + field);
        }
        return new IllegalArgumentException("Invalid JSON request body");
    }
}
