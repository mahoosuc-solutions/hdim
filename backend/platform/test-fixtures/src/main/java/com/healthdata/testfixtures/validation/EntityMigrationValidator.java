package com.healthdata.testfixtures.validation;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.*;

// Additional imports for JPA Metamodel
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Validates that JPA entities match the actual database schema.
 *
 * Uses JPA Metamodel API to extract entity definitions and JDBC DatabaseMetaData
 * to introspect actual schema, comparing them for mismatches that indicate
 * entity-migration drift.
 *
 * @author HDIM Platform Team
 */
@Slf4j
public class EntityMigrationValidator {

    private final DataSource dataSource;
    private final SchemaIntrospector schemaIntrospector;

    public EntityMigrationValidator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.schemaIntrospector = new SchemaIntrospector(dataSource);
    }

    /**
     * Validate all entities against the database schema.
     *
     * @param entities set of JPA entity types to validate
     * @return validation report with findings
     */
    public ValidationReport validate(Set<EntityType<?>> entities) {
        ValidationReport report = new ValidationReport();

        log.info("Starting entity-migration validation for {} entities", entities.size());

        for (EntityType<?> entity : entities) {
            validateEntity(entity, report);
        }

        log.info("Validation complete: {} errors, {} warnings, {} infos",
                report.getErrors().size(), report.getWarnings().size(), report.getInfos().size());

        return report;
    }

    /**
     * Validate a single entity against its database table.
     *
     * @param entity the JPA entity type
     * @param report the validation report to update
     */
    private void validateEntity(EntityType<?> entity, ValidationReport report) {
        String tableName = getTableName(entity);

        log.debug("Validating entity: {} -> table: {}", entity.getName(), tableName);

        // Check table exists
        if (!schemaIntrospector.tableExists(tableName)) {
            report.addError(tableName, "Table does not exist in database");
            return;
        }

        report.incrementTablesChecked();

        // Get entity columns and database columns
        Map<String, AttributeInfo> entityColumns = getEntityAttributes(entity);
        Map<String, SchemaIntrospector.ColumnInfo> dbColumns = schemaIntrospector.getTableColumns(tableName);

        // Validate each entity column exists in database
        for (String columnName : entityColumns.keySet()) {
            if (!dbColumns.containsKey(columnName)) {
                report.addError(tableName, columnName, "Column does not exist in database");
            } else {
                // Validate column properties
                validateColumn(tableName, columnName, entityColumns.get(columnName), dbColumns.get(columnName), report);
            }
        }

        // Check for unexpected columns in database (columns without entity attribute)
        for (String columnName : dbColumns.keySet()) {
            if (!entityColumns.containsKey(columnName) && !isSystemColumn(columnName)) {
                report.addWarning(tableName, columnName, "Database column has no corresponding entity attribute");
            }
        }

        // Validate primary key
        validatePrimaryKey(entity, tableName, report);

        // Validate foreign keys
        validateForeignKeys(entity, tableName, report);

        // Validate indexes
        validateIndexes(entity, tableName, report);
    }

    /**
     * Validate a single column matches between entity and database.
     *
     * @param tableName the table name
     * @param columnName the column name
     * @param entityAttr the entity attribute info
     * @param dbColumn the database column info
     * @param report the validation report to update
     */
    private void validateColumn(String tableName, String columnName, AttributeInfo entityAttr,
                               SchemaIntrospector.ColumnInfo dbColumn, ValidationReport report) {
        report.incrementColumnsChecked();

        // Check type compatibility (skip for relationship fields - type is determined by target entity's ID)
        if (!entityAttr.isRelationship && !ColumnTypeMatcher.isTypeCompatible(entityAttr.javaType, dbColumn.getTypeName())) {
            List<String> expected = ColumnTypeMatcher.getExpectedPgTypes(entityAttr.javaType);
            report.addError(tableName, columnName,
                    String.format("Type mismatch: entity has %s, database has %s (expected: %s)",
                            entityAttr.javaType.getSimpleName(), dbColumn.getTypeName(), expected));
        }

        // Check nullability
        if (entityAttr.nullable != dbColumn.isNullable()) {
            report.addError(tableName, columnName,
                    String.format("Nullability mismatch: entity nullable=%s, database nullable=%s",
                            entityAttr.nullable, dbColumn.isNullable()));
        }

        // Check length constraint for VARCHAR
        if (entityAttr.length > 0 && ("varchar".equalsIgnoreCase(dbColumn.getTypeName()) ||
                "character varying".equalsIgnoreCase(dbColumn.getTypeName()))) {
            if (dbColumn.getColumnSize() > 0 && dbColumn.getColumnSize() < entityAttr.length) {
                report.addWarning(tableName, columnName,
                        String.format("Length constraint mismatch: entity expects %d, database allows %d",
                                entityAttr.length, dbColumn.getColumnSize()));
            }
        }
    }

    /**
     * Validate primary key column(s).
     *
     * @param entity the JPA entity type
     * @param tableName the table name
     * @param report the validation report to update
     */
    private void validatePrimaryKey(EntityType<?> entity, String tableName, ValidationReport report) {
        Set<String> entityPkColumns = new HashSet<>();
        Set<String> dbPkColumns = schemaIntrospector.getPrimaryKeyColumns(tableName);

        // Get PK from entity
        if (entity.hasSingleIdAttribute()) {
            // For simple @Id annotation (not IdClass)
            SingularAttribute<?, ?> idAttr = entity.getId(entity.getIdType().getJavaType());
            String columnName = getColumnName(idAttr);
            if (columnName != null) {
                entityPkColumns.add(columnName);
            }
        } else if (!entity.getIdClassAttributes().isEmpty()) {
            // For composite keys using @IdClass
            for (SingularAttribute<?, ?> idAttr : entity.getIdClassAttributes()) {
                String columnName = getColumnName(idAttr);
                if (columnName != null) {
                    entityPkColumns.add(columnName);
                }
            }
        }

        // Compare
        if (!entityPkColumns.isEmpty() && !dbPkColumns.isEmpty()) {
            if (!entityPkColumns.equals(dbPkColumns)) {
                report.addError(tableName, "Primary key columns do not match: " +
                        "entity has " + entityPkColumns + ", database has " + dbPkColumns);
            }
        }
    }

    /**
     * Validate foreign key constraints.
     *
     * @param entity the JPA entity type
     * @param tableName the table name
     * @param report the validation report to update
     */
    private void validateForeignKeys(EntityType<?> entity, String tableName, ValidationReport report) {
        Map<String, String> entityFkColumns = new HashMap<>();

        // Get FK columns from entity
        for (Attribute<?, ?> attr : entity.getAttributes()) {
            if (attr instanceof SingularAttribute) {
                SingularAttribute<?, ?> singularAttr = (SingularAttribute<?, ?>) attr;
                if (singularAttr.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE) {
                    JoinColumn jc = getJoinColumn(attr);
                    if (jc != null) {
                        entityFkColumns.put(jc.name(), attr.getName());
                    }
                }
            }
        }

        // Get FK columns from database
        List<SchemaIntrospector.ForeignKeyInfo> dbFks = schemaIntrospector.getForeignKeyConstraints(tableName);
        Map<String, SchemaIntrospector.ForeignKeyInfo> dbFksByColumn = new HashMap<>();
        dbFks.forEach(fk -> dbFksByColumn.put(fk.getColumnName(), fk));

        // Compare
        for (String columnName : entityFkColumns.keySet()) {
            if (!dbFksByColumn.containsKey(columnName)) {
                report.addWarning(tableName, columnName,
                        "Entity has foreign key but database constraint not found");
            }
        }
    }

    /**
     * Validate index definitions.
     *
     * @param entity the JPA entity type
     * @param tableName the table name
     * @param report the validation report to update
     */
    private void validateIndexes(EntityType<?> entity, String tableName, ValidationReport report) {
        Map<String, List<String>> entityIndexes = getEntityIndexes(entity);
        List<SchemaIntrospector.IndexInfo> dbIndexes = schemaIntrospector.getIndexes(tableName);

        report.incrementIndexesChecked(entityIndexes.size());

        // Convert DB indexes to map
        Map<String, SchemaIntrospector.IndexInfo> dbIndexesByName = new HashMap<>();
        dbIndexes.forEach(idx -> dbIndexesByName.put(idx.getName(), idx));

        // Check entity indexes exist in database
        for (String indexName : entityIndexes.keySet()) {
            if (!dbIndexesByName.containsKey(indexName)) {
                report.addWarning(tableName, null,
                        String.format("Index defined in entity but not found in database: %s", indexName));
            }
        }
    }

    /**
     * Get table name from entity.
     *
     * @param entity the JPA entity type
     * @return the table name
     */
    private String getTableName(EntityType<?> entity) {
        String tableName = entity.getName();

        // Check for @Table annotation
        try {
            Class<?> entityClass = entity.getJavaType();
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                tableName = tableAnnotation.name();
            }
        } catch (Exception e) {
            log.debug("Could not get @Table annotation", e);
        }

        return tableName;
    }

    /**
     * Get column name from attribute.
     *
     * @param attribute the JPA attribute
     * @return the column name, or null if not a column attribute
     */
    private String getColumnName(Attribute<?, ?> attribute) {
        if (attribute instanceof SingularAttribute) {
            SingularAttribute<?, ?> singularAttr = (SingularAttribute<?, ?>) attribute;

            try {
                Field field = attribute.getJavaType().getDeclaredField(attribute.getName());
                Column col = field.getAnnotation(Column.class);
                if (col != null && !col.name().isEmpty()) {
                    // Strip backticks used for escaping reserved words (e.g., `year` -> year)
                    return stripBackticks(col.name());
                }
            } catch (Exception e) {
                // Field not found or not accessible
            }
        }

        // Apply Hibernate's default naming strategy: convert camelCase to snake_case
        // This matches Hibernate's PhysicalNamingStrategy behavior
        return camelCaseToSnakeCase(attribute.getName());
    }

    /**
     * Strip backticks from a column name (used for escaping reserved words in SQL).
     * Examples: `year` -> year, `order` -> order
     *
     * @param columnName the column name potentially with backticks
     * @return the column name without backticks
     */
    private String stripBackticks(String columnName) {
        if (columnName != null && columnName.startsWith("`") && columnName.endsWith("`")) {
            return columnName.substring(1, columnName.length() - 1);
        }
        return columnName;
    }

    /**
     * Convert camelCase to snake_case (Hibernate's default naming convention).
     * Examples: firstName → first_name, createdAt → created_at
     *
     * @param camelCaseName the camelCase name
     * @return the snake_case name
     */
    private String camelCaseToSnakeCase(String camelCaseName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCaseName.length(); i++) {
            char ch = camelCaseName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Get JoinColumn annotation from attribute if present.
     *
     * @param attribute the JPA attribute
     * @return the JoinColumn annotation, or null
     */
    private JoinColumn getJoinColumn(Attribute<?, ?> attribute) {
        try {
            Field field = attribute.getJavaType().getDeclaredField(attribute.getName());
            return field.getAnnotation(JoinColumn.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all entity attributes with their metadata.
     *
     * @param entity the JPA entity type
     * @return map of column name to AttributeInfo
     */
    private Map<String, AttributeInfo> getEntityAttributes(EntityType<?> entity) {
        Map<String, AttributeInfo> attributes = new LinkedHashMap<>();

        for (Attribute<?, ?> attr : entity.getAttributes()) {
            if (attr instanceof SingularAttribute) {
                SingularAttribute<?, ?> singularAttr = (SingularAttribute<?, ?>) attr;
                String columnName = getColumnName(attr);
                Class<?> javaType = singularAttr.getJavaType();
                boolean nullable = true;
                int length = -1;
                boolean isRelationship = false;

                // Get column metadata from field
                try {
                    Field field = entity.getJavaType().getDeclaredField(attr.getName());
                    Column col = field.getAnnotation(Column.class);
                    JoinColumn joinCol = field.getAnnotation(JoinColumn.class);

                    // Handle relationship fields with @JoinColumn
                    if (joinCol != null) {
                        columnName = stripBackticks(joinCol.name());
                        nullable = joinCol.nullable();
                        isRelationship = true;
                        // For relationships, use Object.class as placeholder (type is determined by target entity)
                        javaType = Object.class;
                    } else if (col != null) {
                        // Handle regular @Column fields
                        String colName = col.name().isEmpty() ? columnName : col.name();
                        columnName = stripBackticks(colName);
                        nullable = col.nullable();
                        length = col.length();
                    }

                    // @Id fields are always non-nullable (generated primary keys)
                    if (field.getAnnotation(Id.class) != null) {
                        nullable = false;
                    }
                } catch (Exception e) {
                    // Field not found
                }

                attributes.put(columnName, new AttributeInfo(attr.getName(), javaType, nullable, length, isRelationship));
            }
        }

        return attributes;
    }

    /**
     * Get indexes defined in the entity.
     *
     * @param entity the JPA entity type
     * @return map of index name to list of column names
     */
    private Map<String, List<String>> getEntityIndexes(EntityType<?> entity) {
        Map<String, List<String>> indexes = new HashMap<>();

        try {
            Table table = entity.getJavaType().getAnnotation(Table.class);
            if (table != null && table.indexes() != null) {
                for (Index idx : table.indexes()) {
                    List<String> columns = Arrays.asList(idx.columnList().split(","));
                    columns.replaceAll(String::trim);
                    indexes.put(idx.name(), columns);
                }
            }
        } catch (Exception e) {
            log.debug("Could not get indexes from entity", e);
        }

        return indexes;
    }

    /**
     * Check if a column name is a system column (created automatically by Hibernate/DB).
     *
     * @param columnName the column name
     * @return true if it's a system column
     */
    private boolean isSystemColumn(String columnName) {
        // PostgreSQL system columns
        return columnName.startsWith("pg_") ||
               columnName.equalsIgnoreCase("xmin") ||
               columnName.equalsIgnoreCase("xmax") ||
               columnName.equalsIgnoreCase("cmin") ||
               columnName.equalsIgnoreCase("cmax") ||
               columnName.equalsIgnoreCase("ctid") ||
               columnName.equalsIgnoreCase("oid");
    }

    /**
     * Metadata about an entity attribute.
     */
    private static class AttributeInfo {
        final String name;
        final Class<?> javaType;
        final boolean nullable;
        final int length;
        final boolean isRelationship;

        AttributeInfo(String name, Class<?> javaType, boolean nullable, int length) {
            this(name, javaType, nullable, length, false);
        }

        AttributeInfo(String name, Class<?> javaType, boolean nullable, int length, boolean isRelationship) {
            this.name = name;
            this.javaType = javaType;
            this.nullable = nullable;
            this.length = length;
            this.isRelationship = isRelationship;
        }
    }
}
