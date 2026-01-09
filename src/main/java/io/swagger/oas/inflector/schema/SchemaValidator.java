package io.swagger.oas.inflector.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaValidator {
    static Map<String, Schema> SCHEMA_CACHE = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidator.class);
    private static final SchemaRegistry SCHEMA_REGISTRY = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    public enum Direction {
        INPUT,
        OUTPUT
    }

    public static boolean validate(Object argument, String schema, Direction direction) {
        try {
            JsonNode content = Json.mapper().convertValue(argument, JsonNode.class);
            Schema jsonSchema = SCHEMA_REGISTRY.getSchema(schema, InputFormat.JSON);

            List<Error> errors = jsonSchema.validate(content.toString(), InputFormat.JSON);
            if (!errors.isEmpty()) {
                if (direction.equals(Direction.INPUT)) {
                    LOGGER.warn("input: " + content.toString() + "\n" + "does not match schema: \n" + schema);
                } else {
                    LOGGER.warn("response: " + content.toString() + "\n" + "does not match schema: \n" + schema);
                }
                for (Error error : errors) {
                    LOGGER.warn("  validation error: " + error.getMessage());
                }
            }
            return errors.isEmpty();
        } catch (Exception e) {
            LOGGER.error("can't validate model against schema", e);
        }

        return true;
    }

    public static Schema getValidationSchema(String schema) {
        schema = schema.trim();

        Schema output = SCHEMA_CACHE.get(schema);

        if (output == null) {
            try {
                Schema jsonSchema = SCHEMA_REGISTRY.getSchema(schema, InputFormat.JSON);
                SCHEMA_CACHE.put(schema, jsonSchema);
                output = jsonSchema;
            } catch (Exception e) {
                LOGGER.error("can't parse schema: " + schema, e);
            }
        }
        return output;
    }
}
