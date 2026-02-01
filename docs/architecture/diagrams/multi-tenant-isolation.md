# Multi-Tenant Isolation Architecture

Row-level isolation strategy for ensuring patient data from one organization never leaks to another.

---

## Tenant Isolation Model

```mermaid
graph TB
    A["Healthcare Organizations<br/>(Tenants)"] --> B["Tenant 1<br/>Acme Hospital"]
    A --> C["Tenant 2<br/>Blue Cross"]
    A --> D["Tenant 3<br/>United Health"]

    B -->|"Patient Data"| E["PostgreSQL Database<br/>Row-level filtering"]
    C -->|"Patient Data"| E
    D -->|"Patient Data"| E

    E -->|"Table: patients"| F["patient_id | tenant_id | first_name | last_name"]
    F -->|"All Tenant 1"| G["✓ Tenant 1 sees only their patients"]
    F -->|"All Tenant 2"| H["✓ Tenant 2 sees only their patients"]
    F -->|"All Tenant 3"| I["✓ Tenant 3 sees only their patients"]
    F -->|"Never Mixed"| J["✗ No cross-tenant data leaks"]

    style E fill:#e1f5ff
    style G fill:#c8e6c9
    style H fill:#c8e6c9
    style I fill:#c8e6c9
    style J fill:#ffcdd2
```

---

## Query Filtering Pattern

```mermaid
graph LR
    A["Client Request<br/>GET /patients/123<br/>X-Tenant-ID: tenant1"] -->|"Extract tenant_id"| B["Controller"]

    B -->|"Query with filter"| C["Service Layer"]

    C -->|"SELECT * FROM patients<br/>WHERE tenant_id = 'tenant1'<br/>AND patient_id = '123'"| D["Repository"]

    D -->|"Result for tenant1 only"| E["Patient Data<br/>Tenant 1 patient"]

    D -->|"No results if<br/>tenant mismatch"| F["404 Not Found<br/>(Tenant 2 can't see<br/>Tenant 1 patient)"]

    style C fill:#fff9c4
    style D fill:#e1f5ff
    style E fill:#c8e6c9
    style F fill:#ffcdd2
```

**Critical Pattern**:
```java
// REQUIRED - Every query must filter by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(@Param("id") String id,
                                     @Param("tenantId") String tenantId);

// FORBIDDEN - Would leak data across tenants
@Query("SELECT p FROM Patient p WHERE p.id = :id")  // ❌ NO TENANT FILTER!
Optional<Patient> findById(@Param("id") String id);
```

---

## Tenant Identification: X-Tenant-ID Header

```mermaid
graph TB
    A["Client"] -->|"HTTP Request<br/>X-Tenant-ID: tenant1"| B["Load Balancer"]

    B -->|"Gateway"| C["gateway-*-service"]

    C -->|"Validate Header"| D["TrustedHeaderAuthFilter"]

    D -->|"1. Check header present"| E{Header<br/>Valid?}

    E -->|"Missing"| F["400 Bad Request<br/>X-Tenant-ID required"]

    E -->|"Exists"| G["2. Extract tenant_id"]

    G -->|"3. Verify user authorized<br/>for this tenant"| H{User has<br/>access?}

    H -->|"NO"| I["403 Forbidden<br/>Not authorized for this tenant"]

    H -->|"YES"| J["4. Inject into request<br/>X-Auth-Tenant-Ids: tenant1"]

    J -->|"5. Pass to backend"| K["Backend Service"]

    K -->|"6. Use for queries"| L["Database<br/>tenant_id filter"]

    style C fill:#e8f5e9
    style D fill:#c8e6c9
    style L fill:#e1f5ff
```

---

## Database Schema Enforcement

```mermaid
graph TB
    A["Table: patients"] --> B["Columns"]

    B --> C["id (UUID)<br/>PRIMARY KEY"]
    B --> D["tenant_id (VARCHAR)<br/>NOT NULL<br/>✓ ENFORCED"]
    B --> E["first_name (VARCHAR)"]
    B --> F["last_name (VARCHAR)"]
    B --> G["created_at (TIMESTAMP)"]

    D -->|"Index"| H["CREATE INDEX<br/>idx_patients_tenant_id<br/>ON patients(tenant_id)"]

    D -->|"Unique Constraint"| I["UNIQUE (tenant_id, patient_id)<br/>Ensures uniqueness<br/>per tenant"]

    style D fill:#ffcdd2
    style H fill:#c8e6c9
    style I fill:#c8e6c9
```

**Liquibase Enforcement**:

```xml
<changeSet id="0001-create-patients-table">
    <createTable tableName="patients">
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="patient_id" type="UUID" primaryKey="true"/>
        <!-- more columns -->
    </createTable>

    <!-- Enforce tenant isolation -->
    <createIndex indexName="idx_patients_tenant" tableName="patients">
        <column name="tenant_id"/>
    </createIndex>

    <addUniqueConstraint
        tableName="patients"
        columnNames="tenant_id,patient_id"/>
</changeSet>
```

---

## Testing: Verifying Tenant Isolation

```mermaid
graph TB
    A["Test: Tenant Isolation"] --> B["Setup"]

    B -->|"Create patient"| B1["tenantA/patient123"]
    B -->|"Create patient"| B2["tenantB/patient123<br/>(same ID, diff tenant)"]

    A -->|"Test 1: Tenant A reads<br/>their data"| C1["Query: WHERE tenant_id='tenantA'<br/>AND id='patient123'"]
    C1 -->|"✓ Found"| D1["PASS"]

    A -->|"Test 2: Tenant A can't<br/>read Tenant B data"| C2["Query: WHERE tenant_id='tenantA'<br/>AND id='patient123'<br/>(for tenantB patient)"]
    C2 -->|"✗ Not Found<br/>403 Forbidden"| D2["PASS"]

    A -->|"Test 3: Tenant B reads<br/>their data"| C3["Query: WHERE tenant_id='tenantB'<br/>AND id='patient123'"]
    C3 -->|"✓ Found"| D3["PASS"]

    style D1 fill:#c8e6c9
    style D2 fill:#c8e6c9
    style D3 fill:#c8e6c9
```

**Test Code**:

```java
@Test
void testTenantIsolation() {
    // Create same patient ID in two tenants
    Patient p1 = patientService.create("John", "tenant1");
    Patient p2 = patientService.create("John", "tenant2");

    // Tenant 1 should only see their patient
    Optional<Patient> result1 = service.getPatient(p1.getId(), "tenant1");
    assertThat(result1).isPresent();

    // Tenant 2 should NOT see Tenant 1's patient (even with same ID)
    assertThatThrownBy(() ->
        service.getPatient(p1.getId(), "tenant2")
    ).isInstanceOf(TenantAccessDeniedException.class);

    // Tenant 2 should see their own patient
    Optional<Patient> result2 = service.getPatient(p2.getId(), "tenant2");
    assertThat(result2).isPresent();
}
```

---

## Data Lifecycle: Multi-Tenant Operations

```mermaid
graph TB
    A["Patient Data Operations"] --> B["CREATE"]
    A --> C["READ"]
    A --> D["UPDATE"]
    A --> E["DELETE"]

    B -->|"INSERT INTO patients<br/>VALUES (id, 'tenant1', 'John', ...)"| B1["Stored with tenant_id"]

    C -->|"SELECT WHERE<br/>tenant_id='tenant1'"| C1["Only tenant1 data"]

    D -->|"UPDATE patients<br/>WHERE tenant_id='tenant1'<br/>AND id='123'"| D1["Updates only<br/>tenant1 record"]

    E -->|"DELETE FROM patients<br/>WHERE tenant_id='tenant1'<br/>AND id='123'"| E1["Deletes only<br/>tenant1 record"]

    style B1 fill:#c8e6c9
    style C1 fill:#c8e6c9
    style D1 fill:#c8e6c9
    style E1 fill:#c8e6c9
```

---

## Compliance: HIPAA PHI Protection

```mermaid
graph TB
    A["HIPAA Requirements<br/>for Multi-Tenant Systems"] --> B["PHI Isolation"]
    A --> C["Access Controls"]
    A --> D["Audit Trail"]

    B -->|"Row-level filtering"| B1["Tenant A ≠ Tenant B<br/>Patient data isolated"]
    B -->|"Database constraints"| B2["tenant_id NOT NULL<br/>on all PHI tables"]
    B -->|"Application enforcement"| B3["Every query<br/>filters tenant_id"]

    C -->|"Header validation"| C1["X-Tenant-ID<br/>required"]
    C -->|"RBAC"| C2["User permissions<br/>per tenant"]
    C -->|"API-level checks"| C3["@PreAuthorize"]

    D -->|"Audit logging"| D1["Who accessed<br/>what when"]
    D -->|"Compliance checks"| D2["Automated validation<br/>no data leaks"]

    style B1 fill:#c8e6c9
    style B2 fill:#c8e6c9
    style B3 fill:#c8e6c9
    style C1 fill:#fff9c4
    style C2 fill:#fff9c4
    style C3 fill:#fff9c4
    style D1 fill:#b3e5fc
    style D2 fill:#b3e5fc
```

---

## Anti-Patterns: What NOT to Do

```mermaid
graph TB
    A["❌ Common Mistakes<br/>That Break Isolation"]

    A -->|"AVOID"| A1["Missing tenant_id filter"]
    A1 -->|"@Query('SELECT FROM patients<br/>WHERE id = :id')<br/>← NO tenant filter!"| B1["🔴 DATA LEAK<br/>All tenants see patient"]

    A -->|"AVOID"| A2["No tenant_id in database"]
    A2 -->|"CREATE TABLE patients<br/>(id, first_name, last_name)<br/>← Missing tenant_id!"| B2["🔴 NO ISOLATION<br/>Can't filter by tenant"]

    A -->|"AVOID"| A3["Forget header validation"]
    A3 -->|"No X-Tenant-ID header check<br/>← Client can send wrong tenant"| B3["🔴 SPOOFING RISK<br/>Client claims wrong tenant"]

    A -->|"AVOID"| A4["Shared queries"]
    A4 -->|"Global function getPatient(id)<br/>← No tenant parameter"| B4["🔴 AMBIGUOUS<br/>Which tenant's patient?"]

    style B1 fill:#ffcdd2
    style B2 fill:#ffcdd2
    style B3 fill:#ffcdd2
    style B4 fill:#ffcdd2
```

---

## Performance: Tenant Indexing

```mermaid
graph TB
    A["Query Performance<br/>Multi-Tenant"] --> B["Index Strategy"]

    B -->|"Simple lookup"| B1["CREATE INDEX<br/>idx_patients_tenant_id"]
    B1 -->|"WHERE tenant_id = ?"| C1["✓ O(log n) lookup"]

    B -->|"Composite lookup"| B2["CREATE INDEX<br/>idx_patients_tenant_id_patient_id"]
    B2 -->|"WHERE tenant_id = ? AND id = ?"| C2["✓ Highly optimized"]

    B -->|"Range queries"| B3["CREATE INDEX<br/>idx_patients_tenant_created"]
    B3 -->|"WHERE tenant_id = ? AND created_at > ?"| C3["✓ Time-range scans fast"]

    style C1 fill:#c8e6c9
    style C2 fill:#c8e6c9
    style C3 fill:#c8e6c9
```

---

## Monitoring: Isolation Validation

```mermaid
graph TB
    A["Automated Isolation<br/>Monitoring"] --> B["Queries"]

    B -->|"SELECT COUNT(*) FROM patients<br/>WHERE tenant_id IS NULL"| B1["Alert if NULL tenant_id<br/>(should be 0)"]

    B -->|"Run unit tests<br/>Cross-tenant access"| B2["CI/CD validates<br/>isolation working"]

    B -->|"Audit log review"| B3["Check for<br/>suspicious patterns"]

    B1 --> C["Health Status"]
    B2 --> C
    B3 --> C

    C -->|"Healthy"| D["✓ Isolation verified"]
    C -->|"Issues found"| E["🔴 Alert team<br/>Investigate"]

    style D fill:#c8e6c9
    style E fill:#ffcdd2
```

---

## References

- **[HIPAA Compliance Guide](../../backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI protection details
- **[ADR-009: Multi-Tenant Isolation](../decisions/ADR-009-multi-tenant-isolation.md)** - Decision rationale
- **[Coding Standards](../../backend/docs/CODING_STANDARDS.md)** - Implementation patterns
- **[Entity-Migration Guide](../../backend/docs/ENTITY_MIGRATION_GUIDE.md)** - Database schema

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Compliance: HIPAA PHI Protection_
_Pattern: Row-Level Tenant Isolation_
