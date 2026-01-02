package com.healthdata.testfixtures.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Introspects PostgreSQL database schema using JDBC DatabaseMetaData.
 *
 * Extracts table definitions, column types, constraints, and indexes
 * for comparison with JPA entity definitions.
 *
 * @author HDIM Platform Team
 */
@Slf4j
@RequiredArgsConstructor
public class SchemaIntrospector {

    private final DataSource dataSource;
    private static final String POSTGRESQL_CATALOG_NAME = "public";

    /**
     * Get list of all tables in the schema.
     *
     * @return set of table names
     */
    public Set<String> getAllTables() {
        Set<String> tables = new HashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, "%", new String[]{"TABLE"});

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error introspecting tables", e);
        }

        return tables;
    }

    /**
     * Check if a table exists.
     *
     * @param tableName the table name
     * @return true if table exists
     */
    public boolean tableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName, new String[]{"TABLE"});
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            log.error("Error checking table existence: {}", tableName, e);
            return false;
        }
    }

    /**
     * Get all columns for a table.
     *
     * @param tableName the table name
     * @return map of column name to ColumnInfo
     */
    public Map<String, ColumnInfo> getTableColumns(String tableName) {
        Map<String, ColumnInfo> columns = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName, null);

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                int nullable = rs.getInt("NULLABLE");
                String isNullable = rs.getString("IS_NULLABLE");

                ColumnInfo info = ColumnInfo.builder()
                        .name(columnName)
                        .typeName(typeName)
                        .columnSize(columnSize)
                        .decimalDigits(decimalDigits)
                        .nullable(DatabaseMetaData.columnNullable == nullable || "YES".equalsIgnoreCase(isNullable))
                        .build();

                columns.put(columnName, info);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error introspecting columns for table: {}", tableName, e);
        }

        return columns;
    }

    /**
     * Get primary key columns for a table.
     *
     * @param tableName the table name
     * @return set of column names that form the primary key
     */
    public Set<String> getPrimaryKeyColumns(String tableName) {
        Set<String> pkColumns = new HashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getPrimaryKeys(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName);

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                pkColumns.add(columnName);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error introspecting primary key for table: {}", tableName, e);
        }

        return pkColumns;
    }

    /**
     * Get foreign key constraints for a table.
     *
     * @param tableName the table name
     * @return list of ForeignKeyInfo
     */
    public List<ForeignKeyInfo> getForeignKeyConstraints(String tableName) {
        List<ForeignKeyInfo> fks = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getImportedKeys(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName);

            while (rs.next()) {
                ForeignKeyInfo fk = ForeignKeyInfo.builder()
                        .constraintName(rs.getString("FK_NAME"))
                        .columnName(rs.getString("FKCOLUMN_NAME"))
                        .referencedTable(rs.getString("PKTABLE_NAME"))
                        .referencedColumn(rs.getString("PKCOLUMN_NAME"))
                        .build();

                fks.add(fk);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error introspecting foreign keys for table: {}", tableName, e);
        }

        return fks;
    }

    /**
     * Get indexes for a table.
     *
     * @param tableName the table name
     * @return list of IndexInfo
     */
    public List<IndexInfo> getIndexes(String tableName) {
        List<IndexInfo> indexes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getIndexInfo(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName, false, true);

            Map<String, IndexInfo.IndexInfoBuilder> indexBuilders = new LinkedHashMap<>();

            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");

                if (indexName != null && columnName != null) {
                    indexBuilders.computeIfAbsent(indexName, k ->
                            IndexInfo.builder()
                                    .name(indexName)
                                    .unique(!nonUnique)
                                    .columns(new ArrayList<>())
                    ).columns.add(columnName);
                }
            }
            rs.close();

            indexBuilders.forEach((name, builder) -> indexes.add(builder.build()));
        } catch (SQLException e) {
            log.error("Error introspecting indexes for table: {}", tableName, e);
        }

        return indexes;
    }

    /**
     * Get unique constraints for a table.
     *
     * @param tableName the table name
     * @return list of UniqueConstraintInfo
     */
    public List<UniqueConstraintInfo> getUniqueConstraints(String tableName) {
        List<UniqueConstraintInfo> constraints = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            // Query information_schema for unique constraints
            String sql = """
                    SELECT constraint_name, column_name
                    FROM information_schema.constraint_column_usage
                    WHERE table_name = ? AND constraint_type = 'UNIQUE'
                    ORDER BY constraint_name, ordinal_position
                    """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();

            Map<String, UniqueConstraintInfo.UniqueConstraintInfoBuilder> builders = new LinkedHashMap<>();

            while (rs.next()) {
                String constraintName = rs.getString("constraint_name");
                String columnName = rs.getString("column_name");

                builders.computeIfAbsent(constraintName, k ->
                        UniqueConstraintInfo.builder()
                                .name(constraintName)
                                .columns(new ArrayList<>())
                ).columns.add(columnName);
            }

            builders.forEach((name, builder) -> constraints.add(builder.build()));

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.debug("Could not introspect unique constraints (may not be supported): {}", tableName, e);
        }

        return constraints;
    }

    /**
     * Check if a column is auto-increment.
     *
     * @param tableName the table name
     * @param columnName the column name
     * @return true if column is auto-increment
     */
    public boolean isAutoIncrement(String tableName, String columnName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(conn.getCatalog(), POSTGRESQL_CATALOG_NAME, tableName, columnName);

            if (rs.next()) {
                String isAutoIncrement = rs.getString("IS_AUTOINCREMENT");
                rs.close();
                return "YES".equalsIgnoreCase(isAutoIncrement);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error checking auto-increment for column: {}.{}", tableName, columnName, e);
        }

        return false;
    }

    /**
     * Information about a column in the database.
     */
    @lombok.Data
    @lombok.Builder
    public static class ColumnInfo {
        private String name;
        private String typeName;
        private int columnSize;
        private int decimalDigits;
        private boolean nullable;
    }

    /**
     * Information about a foreign key constraint.
     */
    @lombok.Data
    @lombok.Builder
    public static class ForeignKeyInfo {
        private String constraintName;
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
    }

    /**
     * Information about an index.
     */
    @lombok.Data
    @lombok.Builder
    public static class IndexInfo {
        private String name;
        private boolean unique;
        @lombok.Singular("column")
        private List<String> columns;
    }

    /**
     * Information about a unique constraint.
     */
    @lombok.Data
    @lombok.Builder
    public static class UniqueConstraintInfo {
        private String name;
        @lombok.Singular("column")
        private List<String> columns;
    }
}
