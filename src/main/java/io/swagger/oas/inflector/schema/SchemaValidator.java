package io.swagger.oas.inflector.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchemaValidator {
    static Map<String, JsonSchema> SCHEMA_CACHE = new HashMap<String, JsonSchema>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidator.class);
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public enum Direction {
        INPUT,
        OUTPUT
    }

    public static boolean validate(Object argument, String schema, Direction direction) {
        try {
            JsonNode schemaObject = Json.mapper().readTree(schema);
            JsonNode content = Json.mapper().convertValue(argument, JsonNode.class);
            JsonSchema jsonSchema = SCHEMA_FACTORY.getSchema(schemaObject);

            Set<ValidationMessage> errors = jsonSchema.validate(content);
            if (!errors.isEmpty()) {
                if (direction.equals(Direction.INPUT)) {
                    LOGGER.warn("input: " + content.toString() + "\n" + "does not match schema: \n" + schema);
                } else {
                    LOGGER.warn("response: " + content.toString() + "\n" + "does not match schema: \n" + schema);
                }
                for (ValidationMessage error : errors) {
                    LOGGER.warn("  validation error: " + error.getMessage());
                }
            }
            return errors.isEmpty();
        } catch (Exception e) {
            LOGGER.error("can't validate model against schema", e);
        }

        return true;
    }

    public static JsonSchema getValidationSchema(String schema) {
        schema = schema.trim();

        JsonSchema output = SCHEMA_CACHE.get(schema);

        if (output == null) {
            try {
                JsonNode schemaObject = Json.mapper().readTree(schema);
                JsonSchema jsonSchema = SCHEMA_FACTORY.getSchema(schemaObject);
                SCHEMA_CACHE.put(schema, jsonSchema);
                output = jsonSchema;
            } catch (Exception e) {
                LOGGER.error("can't parse schema: " + schema, e);
            }
        }
        return output;
    }
}
