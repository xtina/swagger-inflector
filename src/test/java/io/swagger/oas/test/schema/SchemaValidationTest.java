package io.swagger.oas.test.schema;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.swagger.oas.inflector.schema.SchemaValidator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SchemaValidationTest {

    @BeforeMethod
    public void setUp() {
        // Default to OAS 3.0 for tests (enables schema conversion)
        SchemaValidator.setOpenApiVersion("3.0");
    }

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
    public void testValidation() {
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

    // OpenAPI 3.1 / JSON Schema 2020-12 specific tests (require version switch)

    @Test
    public void testOpenApi31TypeArray() {
        SchemaValidator.setOpenApiVersion("3.1.0");
        // OpenAPI 3.1 allows type to be an array (replaces nullable)
        String schema = "{\"type\": [\"string\", \"null\"]}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOpenApi31Const() {
        SchemaValidator.setOpenApiVersion("3.1.0");
        String schema = "{\"const\": \"fixed\"}";

        assertTrue(SchemaValidator.validate("fixed", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate("other", schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOpenApi31ExclusiveMinMax() {
        SchemaValidator.setOpenApiVersion("3.1.0");
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

    // OpenAPI 3.0 compatibility tests - uses Draft-04 validator (nullable converted, boolean exclusiveMin/Max native)

    @Test
    public void testOas30Nullable() {
        // OAS 3.0 "nullable: true" is converted to type array (nullable is OAS extension, not in Draft-04)
        String schema = "{\"type\": \"string\", \"nullable\": true}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30ExclusiveMinimumBoolean() {
        // Draft-04 natively supports boolean exclusiveMinimum
        String schema = "{\"type\": \"integer\", \"minimum\": 5, \"exclusiveMinimum\": true}";

        assertTrue(SchemaValidator.validate(10, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(6, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30ExclusiveMaximumBoolean() {
        // Draft-04 natively supports boolean exclusiveMaximum
        String schema = "{\"type\": \"integer\", \"maximum\": 10, \"exclusiveMaximum\": true}";

        assertTrue(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(10, schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(9, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30NullableObject() {
        // OAS 3.0 nullable on object type - converted to type array
        String schema = "{\"type\": \"object\", \"nullable\": true, \"properties\": {\"name\": {\"type\": \"string\"}}}";

        User user = new User();
        user.name = "Test";
        assertTrue(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30TypeArrayNullable() {
        // Type arrays also work in Draft-04 for nullable
        String schema = "{\"type\": [\"string\", \"null\"]}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30NestedNullable() {
        // Nullable in nested properties is also converted
        String schema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\"type\": \"string\", \"nullable\": true},\n" +
                "    \"age\": {\"type\": \"integer\", \"minimum\": 0, \"exclusiveMinimum\": true}\n" +
                "  }\n" +
                "}";

        User user = new User();
        user.name = null;
        assertTrue(SchemaValidator.validate(user, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30NullableFalseUnchanged() {
        // nullable: false should not add null to type array
        String schema = "{\"type\": \"string\", \"nullable\": false}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas30ExclusiveMinFalseUnchanged() {
        // exclusiveMinimum: false keeps minimum as inclusive (Draft-04 native behavior)
        String schema = "{\"type\": \"integer\", \"minimum\": 5, \"exclusiveMinimum\": false}";

        assertTrue(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(6, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(4, schema, SchemaValidator.Direction.INPUT));
    }

    // Version switching tests

    @Test
    public void testVersionSwitchingTo31() {
        SchemaValidator.setOpenApiVersion("3.1.0");
        assertEquals(SchemaValidator.getOpenApiVersion(), SchemaValidator.OpenApiVersion.V3_1);
    }

    @Test
    public void testVersionSwitchingTo30() {
        SchemaValidator.setOpenApiVersion("3.0.3");
        assertEquals(SchemaValidator.getOpenApiVersion(), SchemaValidator.OpenApiVersion.V3_0);
    }

    @Test
    public void testOas31NullableNotConverted() {
        // In OAS 3.1 mode, nullable keyword is not converted (it's ignored by 2020-12 validator)
        SchemaValidator.setOpenApiVersion("3.1.0");
        String schema = "{\"type\": \"string\", \"nullable\": true}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        // Fails because nullable is not recognized by 2020-12 and no conversion happens
        assertFalse(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas31TypeArrayWorks() {
        // In OAS 3.1 mode, type arrays work natively
        SchemaValidator.setOpenApiVersion("3.1.0");
        String schema = "{\"type\": [\"string\", \"null\"]}";

        assertTrue(SchemaValidator.validate("hello", schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(null, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas31ExclusiveMinNumeric() {
        // In OAS 3.1 mode, numeric exclusiveMinimum works natively
        SchemaValidator.setOpenApiVersion("3.1.0");
        String schema = "{\"type\": \"integer\", \"exclusiveMinimum\": 5}";

        assertTrue(SchemaValidator.validate(6, schema, SchemaValidator.Direction.INPUT));
        assertFalse(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
    }

    @Test
    public void testOas31BooleanExclusiveMinIgnored() {
        // In OAS 3.1 mode, boolean exclusiveMinimum is ignored (no conversion)
        SchemaValidator.setOpenApiVersion("3.1.0");
        String schema = "{\"type\": \"integer\", \"minimum\": 5, \"exclusiveMinimum\": true}";

        // 5 passes because boolean exclusiveMinimum is ignored, minimum is still enforced
        assertTrue(SchemaValidator.validate(5, schema, SchemaValidator.Direction.INPUT));
        assertTrue(SchemaValidator.validate(6, schema, SchemaValidator.Direction.INPUT));
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
