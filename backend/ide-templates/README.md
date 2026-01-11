# Liquibase Migration Templates for IDEs

This directory contains IDE templates (snippets/live templates) for quickly creating Liquibase migration files following HDIM project standards.

## Available Templates

| Template | Trigger | Description |
|----------|---------|-------------|
| Master Changelog | `lbmaster` | Creates db.changelog-master.xml file |
| Changeset with SQL | `lbchangeset` | Creates changeset wrapper for SQL file |
| Create Table | `lbcreatetable` | Creates table with tenant isolation |
| Add Column | `lbaddcolumn` | Adds column to existing table |
| Create Index | `lbindex` | Creates index on table |
| Add Foreign Key | `lbfk` | Adds foreign key constraint |
| Modify Data Type | `lbmodifytype` | Changes column data type |
| Enable Extensions | `lbextensions` | Enables PostgreSQL extensions (pg_trgm) |
| Add Not Null | `lbnotnull` | Adds NOT NULL constraint |
| Rename Column | `lbrename` | Renames column |
| Include | `lbinclude` | Adds include tag to master |

## Installation

### IntelliJ IDEA

**Method 1: UI Import (Recommended)**

1. Open IntelliJ IDEA
2. Go to **File** → **Settings** (or **Preferences** on macOS)
3. Navigate to **Editor** → **Live Templates**
4. Click the **gear icon** (⚙️) → **Import Settings...**
5. Select `intellij-liquibase-templates.xml` from this directory
6. Restart IntelliJ IDEA

**Method 2: Manual Copy**

1. Close IntelliJ IDEA
2. Copy `intellij-liquibase-templates.xml` to:
   - **Windows:** `%USERPROFILE%\.IntelliJIdea<version>\config\templates\Liquibase.xml`
   - **macOS:** `~/Library/Application Support/JetBrains/IntelliJIdea<version>/templates/Liquibase.xml`
   - **Linux:** `~/.IntelliJIdea<version>/config/templates/Liquibase.xml`
3. Restart IntelliJ IDEA

**Verification:**
1. Open any XML file
2. Type `lbchangeset` and press **Tab**
3. Template should expand with placeholders

### VS Code

**Installation:**

1. Open VS Code
2. Open **Command Palette** (Ctrl+Shift+P / Cmd+Shift+P)
3. Type: `Preferences: Configure User Snippets`
4. Select `xml.json` (or create if it doesn't exist)
5. Copy contents of `vscode-liquibase-snippets.json`
6. Paste into `xml.json` file
7. Save file

**Alternative (Workspace-Specific):**

1. Create `.vscode/xml.json` in your project root
2. Copy contents of `vscode-liquibase-snippets.json`
3. Paste into `.vscode/xml.json`
4. Save file

**Verification:**
1. Open any `.xml` file
2. Type `lbchangeset`
3. Snippet should appear in autocomplete
4. Press **Tab** or **Enter** to expand

## Usage Examples

### Example 1: Create New Service Migration

**Step 1: Create master changelog**
```xml
<!-- Type: lbmaster + Tab -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <!-- PostgreSQL extensions (if needed) -->
    <include file="db/changelog/0000-enable-extensions.xml"/>

    <!-- Schema migrations -->

</databaseChangeLog>
```

**Step 2: Add extensions changeset**
```xml
<!-- Type: lbextensions + Tab -->
<changeSet id="0000-enable-extensions" author="patient-service">
    <comment>Enable PostgreSQL extensions for full-text search</comment>

    <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>

    <rollback>
        <sql>DROP EXTENSION IF EXISTS pg_trgm;</sql>
    </rollback>
</changeSet>
```

**Step 3: Add table creation changeset**
```xml
<!-- Type: lbcreatetable + Tab -->
<changeSet id="0001-create-appointments-table" author="john-doe">
    <comment>Create appointments table</comment>

    <createTable tableName="appointments">
        <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true" primaryKeyName="pk_appointments"/>
        </column>
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
                defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
    </createTable>

    <createIndex indexName="idx_appointments_tenant_id" tableName="appointments">
        <column name="tenant_id"/>
    </createIndex>

    <rollback>
        <dropTable tableName="appointments"/>
    </rollback>
</changeSet>
```

### Example 2: Add Column to Existing Table

```xml
<!-- Type: lbaddcolumn + Tab -->
<changeSet id="0002-add-status-to-appointments" author="jane-smith">
    <comment>Add status column to appointments table</comment>

    <addColumn tableName="appointments">
        <column name="status" type="VARCHAR(50)">
            <constraints nullable="true"/>
        </column>
    </addColumn>

    <rollback>
        <dropColumn tableName="appointments" columnName="status"/>
    </rollback>
</changeSet>
```

### Example 3: Add Foreign Key

```xml
<!-- Type: lbfk + Tab -->
<changeSet id="0003-add-fk-appointments-patient" author="john-doe">
    <comment>Add foreign key from appointments to patients</comment>

    <addForeignKeyConstraint
        constraintName="fk_appointments_patient"
        baseTableName="appointments"
        baseColumnNames="patient_id"
        referencedTableName="patients"
        referencedColumnNames="id"
        onDelete="CASCADE"/>

    <rollback>
        <dropForeignKeyConstraint
            baseTableName="appointments"
            constraintName="fk_appointments_patient"/>
    </rollback>
</changeSet>
```

### Example 4: Create Index

```xml
<!-- Type: lbindex + Tab -->
<changeSet id="0004-create-index-appointments-date" author="jane-smith">
    <comment>Create index on appointments(appointment_date)</comment>

    <createIndex indexName="idx_appointments_date" tableName="appointments">
        <column name="appointment_date"/>
    </createIndex>

    <rollback>
        <dropIndex indexName="idx_appointments_date" tableName="appointments"/>
    </rollback>
</changeSet>
```

## Template Placeholders

When you expand a template, placeholders are highlighted. Use **Tab** to jump between them:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `$ID$` / `${1:id}` | Migration ID (sequential number) | `0001`, `0002`, `0003` |
| `$AUTHOR$` / `${2:author}` | Developer name or team | `john-doe`, `patient-team` |
| `$TABLE$` / `${2:table}` | Table name | `appointments`, `patients` |
| `$COLUMN$` / `${2:column}` | Column name | `status`, `provider_id` |
| `$TYPE$` / `${6:type}` | Column data type | `VARCHAR(255)`, `UUID`, `INT` |
| `$COMMENT$` / `${3:comment}` | Changeset description | `Create table`, `Add index` |
| `$ROLLBACK_SQL$` / `${4:rollback}` | Rollback SQL statement | `DROP TABLE`, `DROP COLUMN` |

**IntelliJ IDEA:** Placeholders use `$VARIABLE$` format
**VS Code:** Placeholders use `${1:default}` format with tab stops

## Best Practices

### 1. Sequential Migration Numbers
Always use sequential 4-digit numbers starting from 0001:
```
0000-enable-extensions.xml
0001-create-patients-table.xml
0002-add-status-column.xml
0003-create-indexes.xml
```

### 2. Descriptive Names
Use verb-noun-object pattern:
- `create-TABLE-table`
- `add-COLUMN-to-TABLE`
- `modify-COLUMN-type`
- `create-index-NAME`

### 3. Always Include Rollback
Every changeset MUST have explicit rollback SQL:
```xml
<rollback>
    <dropTable tableName="appointments"/>
</rollback>
```

### 4. Tenant Isolation
All tables should include `tenant_id` column with index:
```xml
<column name="tenant_id" type="VARCHAR(100)">
    <constraints nullable="false"/>
</column>
```

### 5. Audit Columns
Include standard audit columns:
```xml
<column name="created_at" type="TIMESTAMP WITH TIME ZONE"
        defaultValueComputed="CURRENT_TIMESTAMP">
    <constraints nullable="false"/>
</column>
<column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
```

## Workflow

**Creating a New Migration:**

1. **Determine next migration number**
   ```bash
   ls src/main/resources/db/changelog/
   # Last file: 0003-create-indexes.xml
   # Next file: 0004-add-foreign-keys.xml
   ```

2. **Create XML file with template**
   - Type: `lbaddcolumn` + Tab
   - Fill in: ID → `0004`, Table → `appointments`, Column → `provider_id`
   - Save as: `0004-add-provider-id-to-appointments.xml`

3. **Create SQL file (if using sqlFile)**
   ```bash
   mkdir -p src/main/resources/db/changelog/sql
   # Create: sql/0004-add-provider-id-to-appointments.sql
   ```

4. **Update master changelog**
   ```xml
   <!-- Type: lbinclude + Tab -->
   <include file="db/changelog/0004-add-provider-id-to-appointments.xml"/>
   ```

5. **Run validation test**
   ```bash
   ./gradlew test --tests "*EntityMigrationValidationTest"
   ```

## Keyboard Shortcuts

### IntelliJ IDEA
- **Expand Template:** Type prefix + **Tab**
- **Next Placeholder:** **Tab**
- **Previous Placeholder:** **Shift + Tab**
- **Exit Template:** **Enter** or **Escape**

### VS Code
- **Show Suggestions:** **Ctrl + Space**
- **Expand Snippet:** **Tab** or **Enter**
- **Next Placeholder:** **Tab**
- **Previous Placeholder:** **Shift + Tab**
- **Exit Snippet:** **Escape**

## Customization

### Add New Template (IntelliJ IDEA)

1. Go to **Settings** → **Editor** → **Live Templates**
2. Select **Liquibase** group
3. Click **+** → **Live Template**
4. Set:
   - **Abbreviation:** Template trigger (e.g., `lbmytemplate`)
   - **Description:** What the template does
   - **Template text:** XML content with `$VARIABLE$` placeholders
   - **Context:** XML

### Add New Snippet (VS Code)

Edit `xml.json`:
```json
{
  "My Custom Template": {
    "prefix": "lbmytemplate",
    "scope": "xml",
    "body": [
      "<changeSet id=\"${1:id}\" author=\"${2:author}\">",
      "    $0",
      "</changeSet>"
    ],
    "description": "My custom Liquibase template"
  }
}
```

## Troubleshooting

**IntelliJ: Template doesn't expand**
- Make sure you're in an XML file
- Check **Settings** → **Editor** → **Live Templates** → **Liquibase** is enabled
- Verify context is set to "XML"
- Try **Ctrl + J** to show available templates

**VS Code: Snippet doesn't appear**
- Verify `xml.json` file is in correct location
- Reload window: **Ctrl + Shift + P** → `Reload Window`
- Check file has `.xml` extension
- Enable suggestions: **Ctrl + Space**

**Templates have wrong format**
- Check XML indentation settings in your IDE
- IntelliJ: **Settings** → **Editor** → **Code Style** → **XML**
- VS Code: **Settings** → **Editor: Tab Size** (should be 4 for XML)

## Related Documentation

- **Migration Runbook:** `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **Migration Status:** `backend/docs/DATABASE_MIGRATION_STATUS.md`
- **CLAUDE.md:** Database Architecture section

## Support

For questions or issues with templates:
1. Check this README
2. Consult the migration runbook
3. Review existing migrations in services
4. Ask the database migration team

---

**Pro Tip:** Customize these templates to match your team's specific naming conventions and patterns!
