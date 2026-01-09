package io.swagger.oas.test.schema;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.swagger.oas.inflector.schema.SchemaValidator;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SchemaValidationTest {


    @Test
    public void testValidPayload() {
        User user = new User();
        user.id = 9873432343L;
        user.name = "Fred";

        String schema = "{\n" +
                "  \"required\": [\n" +
                "    \"id\"\n" +
                "  ],\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"format\": \"int64\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        assertTrue(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testInvalidPayloadMissingRequired() {
        User user = new User();
        user.name = "Fred";

        String schema = "{\n" +
                "  \"required\": [\n" +
                "    \"id\"\n" +
                "  ],\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"format\": \"int64\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        assertFalse(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testInvalidPayloadWithRange() {
        User user = new User();
        user.id = 0L;
        user.name = "Fred";

        String schema = "{\n" +
                "  \"required\": [\n" +
                "    \"id\"\n" +
                "  ],\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"format\": \"int64\",\n" +
                "      \"minimum\": 123,\n" +
                "      \"maximum\": 400\n" +
                "    }\n" +
                "  }\n" +
                "}";

        assertFalse(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOutputDirectionValidPayload() {
        User user = new User();
        user.id = 123L;
        user.name = "Test";

        String schema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"integer\"}}}";

        assertTrue(SchemaValidator.validate(user, schema, SchemaValidator.Direction.OUTPUT));
    }

    @Test
    public void testOutputDirectionInvalidPayload() {
        User user = new User();
        user.name = "Test";

        String schema = "{\"required\": [\"id\"], \"properties\": {\"id\": {\"type\": \"integer\"}}}";

        assertFalse(SchemaValidator.validate(user, schema, SchemaValidator.Direction.OUTPUT));
    }

    @Test
    public void testValidation() throws Exception {
        String schemaAsString =
                "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"format\": \"int64\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        String content = "{\n  \"id\": 123\n}";

        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        Schema schema = registry.getSchema(schemaAsString, InputFormat.JSON);

        List<Error> errors = schema.validate(content, InputFormat.JSON);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testGetValidationSchema() {
        String schema = "{\"type\": \"object\"}";

        Schema result = SchemaValidator.getValidationSchema(schema);

        assertNotNull(result);
    }

    @Test
    public void testGetValidationSchemaCaching() {
        // Use a unique schema to avoid cache pollution from other tests
        String schema = "{\"type\": \"boolean\", \"description\": \"cache-test\"}";

        Schema first = SchemaValidator.getValidationSchema(schema);
        Schema second = SchemaValidator.getValidationSchema(schema);

        assertSame(first, second);
    }

    @Test
    public void testGetValidationSchemaTrimsWhitespace() {
        String schemaWithWhitespace = "  {\"type\": \"number\"}  ";
        String schemaTrimmed = "{\"type\": \"number\"}";

        Schema fromWhitespace = SchemaValidator.getValidationSchema(schemaWithWhitespace);
        Schema fromTrimmed = SchemaValidator.getValidationSchema(schemaTrimmed);

        assertNotNull(fromWhitespace);
        assertSame(fromWhitespace, fromTrimmed);
    }

    @Test
    public void testGetValidationSchemaInvalidJson() {
        String invalidSchema = "not valid json";

        Schema result = SchemaValidator.getValidationSchema(invalidSchema);

        assertNull(result);
    }

    @Test
    public void testValidateWithInvalidSchema() {
        User user = new User();
        user.id = 1L;

        String invalidSchema = "not valid json";

        // Should return true (pass) when schema parsing fails
        assertTrue(SchemaValidator.validate(user, invalidSchema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testValidateTypeMismatch() {
        String schema = "{\"type\": \"string\"}";

        // Validating an integer against a string schema
        assertFalse(SchemaValidator.validate(123, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testValidateNullValueAgainstObjectSchema() {
        // Null doesn't match object type
        String schema = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";

        assertFalse(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testValidateNullValueAgainstNullableSchema() {
        // OpenAPI 3.1 style nullable using type array
        String schema = "{\"type\": [\"object\", \"null\"], \"properties\": {\"name\": {\"type\": \"string\"}}}";

        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    // OpenAPI 3.1 / JSON Schema 2020-12 specific tests

    @Test
    public void testOpenApi31TypeArray() {
        // OpenAPI 3.1 allows type to be an array (replaces nullable)
        String schema = "{\"type\": [\"string\", \"null\"]}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOpenApi31Const() {
        String schema = "{\"const\": \"fixed\"}";

        assertTrue(SchemaValidator.validate("fixed", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("other", schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOpenApi31ExclusiveMinMax() {
        // In 2020-12, exclusiveMinimum/Maximum are numeric values
        String schema = "{\"type\": \"integer\", \"exclusiveMinimum\": 0, \"exclusiveMaximum\": 10}";

        assertTrue(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(0, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(10, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testArrayValidation() {
        String schema = "{\"type\": \"array\", \"items\": {\"type\": \"integer\"}, \"minItems\": 1}";

        assertTrue(SchemaValidator.validate(new int[]{1, 2, 3}, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(new int[]{}, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testNestedObjectValidation() {
        Address address = new Address();
        address.street = "123 Main St";
        address.city = "Springfield";

        UserWithAddress user = new UserWithAddress();
        user.id = 1L;
        user.address = address;

        String schema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [\"id\", \"address\"],\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\"type\": \"integer\"},\n" +
                "    \"address\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"required\": [\"street\", \"city\"],\n" +
                "      \"properties\": {\n" +
                "        \"street\": {\"type\": \"string\"},\n" +
                "        \"city\": {\"type\": \"string\"}\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        assertTrue(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testNestedObjectValidationMissingRequired() {
        Address address = new Address();
        address.street = "123 Main St";
        // city is missing

        UserWithAddress user = new UserWithAddress();
        user.id = 1L;
        user.address = address;

        String schema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [\"id\", \"address\"],\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\"type\": \"integer\"},\n" +
                "    \"address\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"required\": [\"street\", \"city\"],\n" +
                "      \"properties\": {\n" +
                "        \"street\": {\"type\": \"string\"},\n" +
                "        \"city\": {\"type\": \"string\"}\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        assertFalse(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testStringPatternValidation() {
        String schema = "{\"type\": \"string\", \"pattern\": \"^[a-z]+$\"}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("Hello123", schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testStringLengthValidation() {
        String schema = "{\"type\": \"string\", \"minLength\": 2, \"maxLength\": 5}";

        assertTrue(SchemaValidator.validate("abc", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("a", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("toolong", schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testEnumValidation() {
        String schema = "{\"type\": \"string\", \"enum\": [\"red\", \"green\", \"blue\"]}";

        assertTrue(SchemaValidator.validate("red", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("yellow", schema, SchemaValidator.Direction.INPUT));
    }

    static class User {
        public Long id;
        public String name;
    }

    static class Address {
        public String street;
        public String city;
    }

    static class UserWithAddress {
        public Long id;
        public Address address;
    }
}
