package com.healthdata.testfixtures.validation;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Utility for matching Java column types to PostgreSQL database types.
 *
 * Provides type compatibility checking and conversion logic for entity-migration validation.
 * Handles standard Java types, temporal types, and custom types.
 *
 * @author HDIM Platform Team
 */
public class ColumnTypeMatcher {

    private static final Map<Class<?>, List<String>> JAVA_TO_PG_TYPES = new LinkedHashMap<>();

    static {
        // String types (including JSONB for JSON storage)
        JAVA_TO_PG_TYPES.put(String.class, Arrays.asList("varchar", "character varying", "text", "jsonb", "json"));

        // Numeric types
        JAVA_TO_PG_TYPES.put(Integer.class, Arrays.asList("integer", "int", "int4"));
        JAVA_TO_PG_TYPES.put(int.class, Arrays.asList("integer", "int", "int4"));
        JAVA_TO_PG_TYPES.put(Long.class, Arrays.asList("bigint", "int8"));
        JAVA_TO_PG_TYPES.put(long.class, Arrays.asList("bigint", "int8"));
        JAVA_TO_PG_TYPES.put(Short.class, Arrays.asList("smallint", "int2"));
        JAVA_TO_PG_TYPES.put(short.class, Arrays.asList("smallint", "int2"));
        JAVA_TO_PG_TYPES.put(Double.class, Arrays.asList("double precision", "float8"));
        JAVA_TO_PG_TYPES.put(double.class, Arrays.asList("double precision", "float8"));
        JAVA_TO_PG_TYPES.put(Float.class, Arrays.asList("real", "float4"));
        JAVA_TO_PG_TYPES.put(float.class, Arrays.asList("real", "float4"));
        JAVA_TO_PG_TYPES.put(BigDecimal.class, Arrays.asList("numeric", "decimal"));

        // Boolean types
        JAVA_TO_PG_TYPES.put(Boolean.class, Arrays.asList("boolean", "bool"));
        JAVA_TO_PG_TYPES.put(boolean.class, Arrays.asList("boolean", "bool"));

        // Temporal types
        JAVA_TO_PG_TYPES.put(java.util.Date.class, Arrays.asList("timestamp", "timestamp without time zone", "timestamp with time zone", "timestamptz"));
        JAVA_TO_PG_TYPES.put(java.sql.Date.class, Arrays.asList("date"));
        JAVA_TO_PG_TYPES.put(java.sql.Timestamp.class, Arrays.asList("timestamp", "timestamp without time zone", "timestamp with time zone", "timestamptz"));
        JAVA_TO_PG_TYPES.put(LocalDate.class, Arrays.asList("date"));
        JAVA_TO_PG_TYPES.put(LocalTime.class, Arrays.asList("time", "time without time zone"));
        JAVA_TO_PG_TYPES.put(LocalDateTime.class, Arrays.asList("timestamp", "timestamp without time zone"));
        JAVA_TO_PG_TYPES.put(ZonedDateTime.class, Arrays.asList("timestamp with time zone", "timestamptz"));
        JAVA_TO_PG_TYPES.put(OffsetDateTime.class, Arrays.asList("timestamp with time zone", "timestamptz"));
        JAVA_TO_PG_TYPES.put(Instant.class, Arrays.asList("timestamp", "timestamp without time zone", "timestamp with time zone", "timestamptz"));

        // UUID type
        JAVA_TO_PG_TYPES.put(UUID.class, Arrays.asList("uuid"));

        // Byte array
        JAVA_TO_PG_TYPES.put(byte[].class, Arrays.asList("bytea"));
        JAVA_TO_PG_TYPES.put(Byte[].class, Arrays.asList("bytea"));

        // String arrays (PostgreSQL uses _varchar internally for varchar[] arrays)
        JAVA_TO_PG_TYPES.put(String[].class, Arrays.asList("_varchar", "varchar[]", "_text", "text[]", "jsonb", "json"));

        // Collections (stored as JSONB)
        JAVA_TO_PG_TYPES.put(Map.class, Arrays.asList("jsonb", "json"));
        JAVA_TO_PG_TYPES.put(List.class, Arrays.asList("jsonb", "json"));
        JAVA_TO_PG_TYPES.put(Set.class, Arrays.asList("jsonb", "json"));

        // Object.class is used by JPA metamodel for custom Hibernate types like @Type(JsonBinaryType.class)
        // These are typically JSONB columns storing Map<String, Object> or other JSON structures
        JAVA_TO_PG_TYPES.put(Object.class, Arrays.asList("jsonb", "json", "uuid", "bytea"));
    }

    /**
     * PostgreSQL array type prefixes (internal representation uses underscore prefix).
     * Maps standard PostgreSQL array notation to internal names.
     */
    private static final Set<String> PG_ARRAY_PREFIXES = new HashSet<>(Arrays.asList(
            "_varchar", "_text", "_int4", "_int8", "_float8", "_bool", "_uuid"
    ));

    /**
     * Check if a PostgreSQL type is compatible with a Java type.
     *
     * @param javaType the Java class type
     * @param pgType the PostgreSQL type name
     * @return true if types are compatible
     */
    public static boolean isTypeCompatible(Class<?> javaType, String pgType) {
        if (javaType == null || pgType == null) {
            return false;
        }

        String normalizedPgType = pgType.toLowerCase().trim();

        // Handle parameterized types like varchar(255) -> varchar
        if (normalizedPgType.contains("(")) {
            normalizedPgType = normalizedPgType.substring(0, normalizedPgType.indexOf("(")).trim();
        }

        // Check direct match
        List<String> compatiblePgTypes = JAVA_TO_PG_TYPES.get(javaType);
        if (compatiblePgTypes != null && compatiblePgTypes.contains(normalizedPgType)) {
            return true;
        }

        // Check enum type (custom enums stored as VARCHAR)
        if (Enum.class.isAssignableFrom(javaType)) {
            return "varchar".equals(normalizedPgType) || "character varying".equals(normalizedPgType) || "text".equals(normalizedPgType);
        }

        // Check if it's an array type
        if (javaType.isArray()) {
            // PostgreSQL uses underscore prefix for array types (e.g., _varchar for varchar[])
            if (PG_ARRAY_PREFIXES.contains(normalizedPgType) || normalizedPgType.endsWith("[]")) {
                return true;
            }
            // Arrays can also be stored as JSONB
            if ("jsonb".equals(normalizedPgType) || "json".equals(normalizedPgType)) {
                return true;
            }
        }

        // Check if it's a custom class (POJO) stored as JSONB
        // Custom classes that are not standard Java types are typically serialized to JSONB
        if (isCustomClass(javaType) && ("jsonb".equals(normalizedPgType) || "json".equals(normalizedPgType))) {
            return true;
        }

        // Check if it's a generic type with a superclass we recognize
        Class<?> superClass = javaType.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return isTypeCompatible(superClass, pgType);
        }

        return false;
    }

    /**
     * Check if a class is a custom class (POJO) that would be serialized to JSONB.
     * Custom classes are those that:
     * - Are not primitive types or their wrappers
     * - Are not standard Java library classes (java.*, javax.*)
     * - Are not enums
     * - Are not collections or maps
     *
     * @param clazz the class to check
     * @return true if it's a custom class
     */
    private static boolean isCustomClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        // Primitive types and wrappers are not custom classes
        if (clazz.isPrimitive()) {
            return false;
        }

        // Standard library types are not custom classes
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jakarta.")) {
            return false;
        }

        // Enums are handled separately
        if (clazz.isEnum()) {
            return false;
        }

        // If it's not in our type map and not a standard type, it's a custom class
        return !JAVA_TO_PG_TYPES.containsKey(clazz);
    }

    /**
     * Get expected PostgreSQL type names for a Java class.
     *
     * @param javaType the Java class type
     * @return list of compatible PostgreSQL types, or empty list if not found
     */
    public static List<String> getExpectedPgTypes(Class<?> javaType) {
        if (javaType == null) {
            return Collections.emptyList();
        }

        List<String> types = JAVA_TO_PG_TYPES.get(javaType);
        if (types != null) {
            return new ArrayList<>(types);
        }

        // Check if it's an enum
        if (Enum.class.isAssignableFrom(javaType)) {
            return Arrays.asList("varchar", "character varying");
        }

        return Collections.emptyList();
    }

    /**
     * Get the primary (preferred) PostgreSQL type for a Java class.
     *
     * @param javaType the Java class type
     * @return the preferred PostgreSQL type name, or null if not found
     */
    public static String getPreferredPgType(Class<?> javaType) {
        List<String> types = getExpectedPgTypes(javaType);
        return types.isEmpty() ? null : types.get(0);
    }

    /**
     * Check if a length constraint is reasonable for a type.
     *
     * @param javaType the Java class type
     * @param length the declared length
     * @return true if length is valid for the type
     */
    public static boolean isValidLength(Class<?> javaType, int length) {
        if (length <= 0) {
            return false;  // Length must be positive
        }

        // String types can have any reasonable length
        if (String.class.equals(javaType)) {
            return length <= 1_000_000;  // Reasonable upper limit
        }

        // Other types shouldn't have length constraints
        return false;
    }

    /**
     * Extract the base type name from a PostgreSQL type definition.
     *
     * Examples:
     * - "varchar(255)" -> "varchar"
     * - "numeric(10,2)" -> "numeric"
     * - "timestamp with time zone" -> "timestamp with time zone"
     *
     * @param pgTypeDef the PostgreSQL type definition
     * @return the base type name
     */
    public static String extractBaseType(String pgTypeDef) {
        if (pgTypeDef == null || pgTypeDef.isEmpty()) {
            return "";
        }

        String normalized = pgTypeDef.toLowerCase().trim();

        // Handle parameterized types
        if (normalized.contains("(")) {
            normalized = normalized.substring(0, normalized.indexOf("(")).trim();
        }

        return normalized;
    }

    /**
     * Extract length parameter from a PostgreSQL type definition.
     *
     * Examples:
     * - "varchar(255)" -> 255
     * - "varchar" -> -1 (no length specified)
     * - "numeric(10,2)" -> 10
     *
     * @param pgTypeDef the PostgreSQL type definition
     * @return the length, or -1 if not specified
     */
    public static int extractLength(String pgTypeDef) {
        if (pgTypeDef == null || !pgTypeDef.contains("(")) {
            return -1;
        }

        try {
            int start = pgTypeDef.indexOf("(");
            int end = pgTypeDef.indexOf(",", start);
            if (end == -1) {
                end = pgTypeDef.indexOf(")", start);
            }
            if (end == -1) {
                return -1;
            }

            String lengthStr = pgTypeDef.substring(start + 1, end).trim();
            return Integer.parseInt(lengthStr);
        } catch (Exception e) {
            return -1;
        }
    }
}
