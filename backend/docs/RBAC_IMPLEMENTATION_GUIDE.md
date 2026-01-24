# HDIM Platform - RBAC Implementation Guide

**Status:** ✅ Production Ready
**Last Updated:** January 24, 2026
**Version:** 2.0

## Overview

HDIM implements a comprehensive 13-role, 31-permission Role-Based Access Control (RBAC) system for healthcare quality measurement and clinical decision support.

**Key Features:**
- 13 predefined roles organized by privilege level and functional area
- 31 fine-grained permissions across 8 categories
- Permission-based authorization using `@PreAuthorize`
- HIPAA-compliant PHI access controls with audit logging
- Multi-tenant isolation enforcement
- Role hierarchy with permission inheritance

---

## Table of Contents

1. [Roles](#roles)
2. [Permissions](#permissions)
3. [Role-Permission Matrix](#role-permission-matrix)
4. [API Enforcement](#api-enforcement)
5. [UI Enforcement](#ui-enforcement)
6. [Testing RBAC](#testing-rbac)
7. [HIPAA Compliance](#hipaa-compliance)

---

## Roles

### Role Hierarchy

Roles are ordered by privilege level (most to least privileged):

| # | Role | Privilege Level | Description |
|---|------|-----------------|-------------|
| 1 | **SUPER_ADMIN** | System | Full system access across all tenants |
| 2 | **ADMIN** | Tenant | Tenant-level administration |
| 3 | **CLINICAL_ADMIN** | Clinical | Clinical operations management |
| 4 | **QUALITY_OFFICER** | Quality | Quality measurement oversight |
| 5 | **CLINICIAN** | Clinical | Clinical staff with patient care access |
| 6 | **MEASURE_DEVELOPER** | Development | Quality measure development |
| 7 | **EVALUATOR** | Execution | Execute quality measure evaluations |
| 8 | **CARE_COORDINATOR** | Care Management | Care gap management |
| 9 | **AUDITOR** | Compliance | Compliance and audit access |
| 10 | **ANALYST** | Reporting | Analytics and reporting (read-only) |
| 11 | **DEVELOPER** | Integration | API and integration access |
| 12 | **VIEWER** | Read-Only | Basic read-only access |
| 13 | **RESTRICTED** | Minimal | Minimal access for specific workflows |

### Role Descriptions

#### 1. SUPER_ADMIN
**Privilege:** System-wide administrator
**Use Cases:** Platform operators, SRE teams
**Permissions:** All 31 permissions

**Key Capabilities:**
- Full system access across all tenants
- User and tenant management
- System configuration
- Delete operations (users, patients, measures)

**Example Users:**
- Platform administrators
- Site Reliability Engineers
- Database administrators

---

#### 2. ADMIN
**Privilege:** Tenant-level administrator
**Use Cases:** Health plan IT administrators, tenant admins
**Permissions:** 27 permissions (all except SUPER_ADMIN-only)

**Key Capabilities:**
- Tenant-level user management
- Configuration and settings
- Quality measure management
- Patient data management (within tenant)

**Example Users:**
- IT administrators
- System administrators
- Health plan operations managers

---

#### 3. CLINICAL_ADMIN
**Privilege:** Clinical operations manager
**Use Cases:** Chief clinical officers, nursing directors
**Permissions:** 16 permissions

**Key Capabilities:**
- Clinical workflow configuration
- Care gap management oversight
- Clinical staff management
- Patient data access (read/write)

**Example Users:**
- Chief Nursing Officers
- Clinical Directors
- Medical Group Administrators

---

#### 4. QUALITY_OFFICER
**Privilege:** Quality measurement oversight
**Use Cases:** Quality directors, HEDIS coordinators
**Permissions:** 14 permissions

**Key Capabilities:**
- Quality measurement oversight
- HEDIS/CMS measure management
- Quality reporting and compliance
- Audit review and approval

**Example Users:**
- Quality Directors
- HEDIS Coordinators
- Quality Improvement Managers

---

#### 5. CLINICIAN
**Privilege:** Clinical care provider
**Use Cases:** Physicians, nurse practitioners, PAs
**Permissions:** 7 permissions

**Key Capabilities:**
- Patient care access (demographics, clinical data)
- Care gap review and closure
- Clinical decision support access
- Quality measure review

**Example Users:**
- Physicians
- Nurse Practitioners
- Physician Assistants

---

#### 6. MEASURE_DEVELOPER
**Privilege:** Quality measure author
**Use Cases:** Clinical informaticists, quality measure authors
**Permissions:** 6 permissions

**Key Capabilities:**
- Create and manage measure definitions
- CQL authoring and testing
- Value set management
- Measure version control

**Example Users:**
- Clinical Informaticists
- Measure Development Specialists
- CQL Developers

---

#### 7. EVALUATOR
**Privilege:** Evaluation execution
**Use Cases:** Quality analysts, evaluation specialists
**Permissions:** 6 permissions

**Key Capabilities:**
- Execute quality measure evaluations
- Run CQL evaluations
- View evaluation results
- Generate quality reports

**Example Users:**
- Quality Analysts
- Data Analysts
- Evaluation Coordinators

---

#### 8. CARE_COORDINATOR
**Privilege:** Care management
**Use Cases:** Care managers, patient navigators
**Permissions:** 5 permissions

**Key Capabilities:**
- Care gap identification and tracking
- Patient outreach management
- Care plan coordination
- Gap closure documentation

**Example Users:**
- Care Managers
- Patient Navigators
- Care Coordinators

---

#### 9. AUDITOR
**Privilege:** Compliance oversight
**Use Cases:** Compliance officers, security auditors
**Permissions:** 5 permissions

**Key Capabilities:**
- Access audit logs and compliance reports
- PHI access auditing (HIPAA §164.312(b))
- Security incident investigation
- Read-only access to clinical data

**Example Users:**
- Compliance Officers
- Security Auditors
- Internal Auditors

---

#### 10. ANALYST
**Privilege:** Reporting and analytics
**Use Cases:** Data analysts, business intelligence teams
**Permissions:** 5 permissions

**Key Capabilities:**
- View evaluation results and analytics
- Generate reports and dashboards
- Export de-identified data for analysis
- Read-only access (no execution)

**Example Users:**
- Data Analysts
- Business Intelligence Analysts
- Reporting Specialists

---

#### 11. DEVELOPER
**Privilege:** Integration development
**Use Cases:** Integration developers, API consumers
**Permissions:** 5 permissions

**Key Capabilities:**
- API access for integrations
- Test environment access
- Webhook configuration
- No PHI access (synthetic data only)

**Example Users:**
- Integration Developers
- API Developers
- Third-party integrators

---

#### 12. VIEWER
**Privilege:** Read-only access
**Use Cases:** External stakeholders, read-only users
**Permissions:** 2 permissions

**Key Capabilities:**
- View measure definitions
- Limited evaluation results
- No PHI access

**Example Users:**
- External Auditors
- Board Members
- Observers

---

#### 13. RESTRICTED
**Privilege:** Minimal access
**Use Cases:** Limited-scope service accounts, temporary access
**Permissions:** 0 permissions (explicitly granted only)

**Key Capabilities:**
- Explicitly granted permissions only
- No general system access

**Example Users:**
- Service Accounts
- Temporary Contractors
- Limited API Access

---

## Permissions

### Permission Categories

HDIM defines **31 permissions** across **8 categories**:

| Category | Count | Permissions |
|----------|-------|-------------|
| **User Management** | 4 | USER_READ, USER_WRITE, USER_DELETE, USER_MANAGE_ROLES |
| **Quality Measures** | 5 | MEASURE_READ, MEASURE_WRITE, MEASURE_DELETE, MEASURE_EXECUTE, MEASURE_PUBLISH |
| **Patient Data** | 5 | PATIENT_READ, PATIENT_WRITE, PATIENT_DELETE, PATIENT_SEARCH, PATIENT_EXPORT |
| **Care Gaps** | 4 | CARE_GAP_READ, CARE_GAP_WRITE, CARE_GAP_CLOSE, CARE_GAP_ASSIGN |
| **Audit & Compliance** | 3 | AUDIT_READ, AUDIT_EXPORT, AUDIT_REVIEW |
| **Configuration** | 4 | CONFIG_READ, CONFIG_WRITE, TENANT_MANAGE, INTEGRATION_MANAGE |
| **API & Integration** | 3 | API_READ, API_WRITE, API_MANAGE_KEYS |
| **Reporting** | 3 | REPORT_READ, REPORT_CREATE, REPORT_EXPORT |

### Permission Details

#### User Management Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **USER_READ** | View user accounts and profiles | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN |
| **USER_WRITE** | Create and update user accounts | SUPER_ADMIN, ADMIN |
| **USER_DELETE** | Delete user accounts | SUPER_ADMIN |
| **USER_MANAGE_ROLES** | Assign roles and permissions | SUPER_ADMIN, ADMIN |

#### Quality Measures Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **MEASURE_READ** | View quality measure definitions | All except RESTRICTED |
| **MEASURE_WRITE** | Create and update measures | SUPER_ADMIN, ADMIN, MEASURE_DEVELOPER |
| **MEASURE_DELETE** | Delete measure definitions | SUPER_ADMIN, ADMIN |
| **MEASURE_EXECUTE** | Execute quality evaluations | SUPER_ADMIN, ADMIN, QUALITY_OFFICER, MEASURE_DEVELOPER, EVALUATOR, CLINICAL_ADMIN |
| **MEASURE_PUBLISH** | Publish measures to production | SUPER_ADMIN, ADMIN, QUALITY_OFFICER |

#### Patient Data Permissions (PHI)

| Permission | Description | Granted To |
|------------|-------------|------------|
| **PATIENT_READ** | View patient demographics and clinical data (PHI) | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, QUALITY_OFFICER, CARE_COORDINATOR, AUDITOR, MEASURE_DEVELOPER, EVALUATOR |
| **PATIENT_WRITE** | Create and update patient records | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, CARE_COORDINATOR |
| **PATIENT_DELETE** | Delete patient records | SUPER_ADMIN |
| **PATIENT_SEARCH** | Search patient data across tenants | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN |
| **PATIENT_EXPORT** | Export patient data for reporting | SUPER_ADMIN, ADMIN, QUALITY_OFFICER, CLINICAL_ADMIN, ANALYST |

#### Care Gaps Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **CARE_GAP_READ** | View care gaps | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, QUALITY_OFFICER, CARE_COORDINATOR, ANALYST, EVALUATOR |
| **CARE_GAP_WRITE** | Create and update care gaps | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, CARE_COORDINATOR |
| **CARE_GAP_CLOSE** | Close and resolve care gaps | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, CARE_COORDINATOR |
| **CARE_GAP_ASSIGN** | Assign gaps to coordinators | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, QUALITY_OFFICER |

#### Audit & Compliance Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **AUDIT_READ** | View audit logs and access history | SUPER_ADMIN, ADMIN, AUDITOR, QUALITY_OFFICER |
| **AUDIT_EXPORT** | Export audit logs for compliance | SUPER_ADMIN, ADMIN, AUDITOR |
| **AUDIT_REVIEW** | Review and approve compliance reports | SUPER_ADMIN, QUALITY_OFFICER, AUDITOR |

#### Configuration Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **CONFIG_READ** | View system settings | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, DEVELOPER |
| **CONFIG_WRITE** | Update system settings | SUPER_ADMIN, ADMIN, CLINICAL_ADMIN |
| **TENANT_MANAGE** | Manage tenant configuration | SUPER_ADMIN |
| **INTEGRATION_MANAGE** | Configure integrations and APIs | SUPER_ADMIN, ADMIN, DEVELOPER |

#### API & Integration Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **API_READ** | Access APIs for data retrieval | SUPER_ADMIN, ADMIN, DEVELOPER, EVALUATOR, CLINICIAN, MEASURE_DEVELOPER |
| **API_WRITE** | Use APIs for data modification | SUPER_ADMIN, ADMIN, DEVELOPER |
| **API_MANAGE_KEYS** | Generate and manage API keys | SUPER_ADMIN, ADMIN, DEVELOPER |

#### Reporting Permissions

| Permission | Description | Granted To |
|------------|-------------|------------|
| **REPORT_READ** | View quality reports and dashboards | All except RESTRICTED |
| **REPORT_CREATE** | Create and schedule custom reports | SUPER_ADMIN, ADMIN, QUALITY_OFFICER, ANALYST, CLINICAL_ADMIN, MEASURE_DEVELOPER, EVALUATOR |
| **REPORT_EXPORT** | Export reports for distribution | SUPER_ADMIN, ADMIN, QUALITY_OFFICER, ANALYST, CLINICAL_ADMIN, AUDITOR |

---

## Role-Permission Matrix

### Complete Permission Assignment

| Permission | SUPER_ADMIN | ADMIN | CLINICAL_ADMIN | QUALITY_OFFICER | CLINICIAN | MEASURE_DEVELOPER | EVALUATOR | CARE_COORDINATOR | AUDITOR | ANALYST | DEVELOPER | VIEWER | RESTRICTED |
|------------|:-----------:|:-----:|:--------------:|:---------------:|:---------:|:-----------------:|:---------:|:----------------:|:-------:|:-------:|:---------:|:------:|:----------:|
| **User Management** | | | | | | | | | | | | | |
| USER_READ | ✅ | ✅ | ✅ | | | | | | | | | | |
| USER_WRITE | ✅ | ✅ | | | | | | | | | | | |
| USER_DELETE | ✅ | | | | | | | | | | | | |
| USER_MANAGE_ROLES | ✅ | ✅ | | | | | | | | | | | |
| **Quality Measures** | | | | | | | | | | | | | |
| MEASURE_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | | | ✅ | ✅ | ✅ | |
| MEASURE_WRITE | ✅ | ✅ | | | | ✅ | | | | | | | |
| MEASURE_DELETE | ✅ | ✅ | | | | | | | | | | | |
| MEASURE_EXECUTE | ✅ | ✅ | ✅ | ✅ | | ✅ | ✅ | | | | | | |
| MEASURE_PUBLISH | ✅ | ✅ | | ✅ | | | | | | | | | |
| **Patient Data (PHI)** | | | | | | | | | | | | | |
| PATIENT_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | | | | |
| PATIENT_WRITE | ✅ | ✅ | ✅ | | ✅ | | | ✅ | | | | | |
| PATIENT_DELETE | ✅ | | | | | | | | | | | | |
| PATIENT_SEARCH | ✅ | ✅ | ✅ | | | | | | | | | | |
| PATIENT_EXPORT | ✅ | ✅ | ✅ | ✅ | | | | | | ✅ | | | |
| **Care Gaps** | | | | | | | | | | | | | |
| CARE_GAP_READ | ✅ | ✅ | ✅ | ✅ | ✅ | | ✅ | ✅ | | ✅ | | | |
| CARE_GAP_WRITE | ✅ | ✅ | ✅ | | ✅ | | | ✅ | | | | | |
| CARE_GAP_CLOSE | ✅ | ✅ | ✅ | | ✅ | | | ✅ | | | | | |
| CARE_GAP_ASSIGN | ✅ | ✅ | ✅ | ✅ | | | | | | | | | |
| **Audit & Compliance** | | | | | | | | | | | | | |
| AUDIT_READ | ✅ | ✅ | | ✅ | | | | | ✅ | | | | |
| AUDIT_EXPORT | ✅ | ✅ | | | | | | | ✅ | | | | |
| AUDIT_REVIEW | ✅ | ✅ | | ✅ | | | | | ✅ | | | | |
| **Configuration** | | | | | | | | | | | | | |
| CONFIG_READ | ✅ | ✅ | ✅ | | | | | | | | ✅ | | |
| CONFIG_WRITE | ✅ | ✅ | ✅ | | | | | | | | | | |
| TENANT_MANAGE | ✅ | | | | | | | | | | | | |
| INTEGRATION_MANAGE | ✅ | ✅ | | | | | | | | | ✅ | | |
| **API & Integration** | | | | | | | | | | | | | |
| API_READ | ✅ | ✅ | | | ✅ | ✅ | ✅ | | | | ✅ | | |
| API_WRITE | ✅ | ✅ | | | | | | | | | ✅ | | |
| API_MANAGE_KEYS | ✅ | ✅ | | | | | | | | | ✅ | | |
| **Reporting** | | | | | | | | | | | | | |
| REPORT_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | | ✅ | |
| REPORT_CREATE | ✅ | ✅ | ✅ | ✅ | | ✅ | ✅ | | | ✅ | | | |
| REPORT_EXPORT | ✅ | ✅ | ✅ | ✅ | | | | | ✅ | ✅ | | | |
| **Total Permissions** | **31** | **27** | **16** | **14** | **7** | **6** | **6** | **5** | **5** | **5** | **5** | **2** | **0** |

---

## API Enforcement

### Using @PreAuthorize

All API endpoints use Spring Security's `@PreAuthorize` annotation for permission checking.

**Pattern:**
```java
@PreAuthorize("hasPermission('PERMISSION_NAME')")
```

**Example:**
```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    @GetMapping("/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(eventType = "PATIENT_ACCESS")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable String patientId) {
        // Method implementation
    }

    @PostMapping
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(eventType = "PATIENT_CREATE")
    public ResponseEntity<PatientResponse> createPatient(@RequestBody CreatePatientRequest request) {
        // Method implementation
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_DELETE')")
    @Audited(eventType = "PATIENT_DELETE")
    public ResponseEntity<Void> deletePatient(@PathVariable String patientId) {
        // Method implementation
    }
}
```

### Permission Evaluator

HDIM uses a custom `HdimPermissionEvaluator` that:

1. **Extracts user roles** from JWT token
2. **Looks up permissions** from `RolePermissions` mapping
3. **Checks permission** against required permission
4. **Logs audit events** for PHI-related permissions (HIPAA compliance)

**Location:**
```
backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/security/HdimPermissionEvaluator.java
```

### Multiple Permissions

**OR logic** (user needs ANY of the permissions):
```java
@PreAuthorize("hasPermission('MEASURE_EXECUTE') or hasPermission('MEASURE_PUBLISH')")
public ResponseEntity<EvaluationResponse> runEvaluation(...) { }
```

**AND logic** (user needs ALL permissions):
```java
@PreAuthorize("hasPermission('PATIENT_WRITE') and hasPermission('CARE_GAP_WRITE')")
public ResponseEntity<Void> closeCareGapWithPatientUpdate(...) { }
```

### Current RBAC Coverage

**RBAC enforcement statistics:**
- **704 total @PreAuthorize checks** across 89 controller files
- **100% coverage** on patient-service endpoints
- **100% coverage** on quality-measure-service endpoints
- **100% coverage** on care-gap-service endpoints
- **100% coverage** on fhir-service endpoints

---

## UI Enforcement

### Angular Component-Level Enforcement

**Hide buttons/actions based on user permissions:**

```typescript
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.component.html'
})
export class PatientListComponent implements OnInit {
  canCreatePatient = false;
  canDeletePatient = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.canCreatePatient = this.authService.hasPermission('PATIENT_WRITE');
    this.canDeletePatient = this.authService.hasPermission('PATIENT_DELETE');
  }
}
```

**Template:**
```html
<!-- Show create button only if user has PATIENT_WRITE permission -->
<button mat-raised-button color="primary"
        (click)="createPatient()"
        *ngIf="canCreatePatient">
  Create Patient
</button>

<!-- Show delete button only if user has PATIENT_DELETE permission -->
<button mat-icon-button
        (click)="deletePatient(patient.id)"
        *ngIf="canDeletePatient"
        [attr.aria-label]="'Delete patient ' + patient.name">
  <mat-icon aria-hidden="true">delete</mat-icon>
</button>
```

### AuthService Implementation

**Check user permissions in Angular:**

```typescript
export class AuthService {
  private currentUser: User | null = null;

  hasPermission(permission: string): boolean {
    if (!this.currentUser || !this.currentUser.roles) {
      return false;
    }

    // Check if any of the user's roles has the required permission
    return this.currentUser.roles.some(role =>
      this.getRolePermissions(role).includes(permission)
    );
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(permission => this.hasPermission(permission));
  }

  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(permission => this.hasPermission(permission));
  }

  private getRolePermissions(role: string): string[] {
    // Map roles to permissions (should match backend RolePermissions.java)
    const rolePermissionMap: Record<string, string[]> = {
      'SUPER_ADMIN': ['USER_READ', 'USER_WRITE', 'USER_DELETE', 'USER_MANAGE_ROLES', /* all 31 permissions */],
      'ADMIN': ['USER_READ', 'USER_WRITE', 'USER_MANAGE_ROLES', 'MEASURE_READ', /* 27 permissions */],
      'CLINICIAN': ['PATIENT_READ', 'PATIENT_WRITE', 'CARE_GAP_READ', 'CARE_GAP_WRITE', 'CARE_GAP_CLOSE', 'MEASURE_READ', 'API_READ', 'REPORT_READ'],
      // ... other roles
    };

    return rolePermissionMap[role] || [];
  }
}
```

### Route Guards

**Prevent unauthorized navigation:**

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class PermissionGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredPermission = route.data['permission'];

    if (!requiredPermission) {
      return true; // No permission required
    }

    if (this.authService.hasPermission(requiredPermission)) {
      return true;
    }

    // User lacks permission - redirect to unauthorized page
    this.router.navigate(['/unauthorized']);
    return false;
  }
}
```

**Route configuration:**
```typescript
const routes: Routes = [
  {
    path: 'patients/create',
    component: CreatePatientComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'PATIENT_WRITE' }
  },
  {
    path: 'measures/execute',
    component: ExecuteMeasureComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'MEASURE_EXECUTE' }
  }
];
```

---

## Testing RBAC

### Unit Tests

**Test permission checks:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class RbacAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CLINICIAN")
    void clinician_canReadPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients/PAT-123")
                .header("X-Tenant-ID", "TENANT-001"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLINICIAN")
    void clinician_cannotDeletePatient() throws Exception {
        mockMvc.perform(delete("/api/v1/patients/PAT-123")
                .header("X-Tenant-ID", "TENANT-001"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    void evaluator_canExecuteMeasure() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"PAT-123\",\"measureId\":\"COL-001\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_cannotExecuteMeasure() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"PAT-123\",\"measureId\":\"COL-001\"}"))
            .andExpect(status().isForbidden());
    }
}
```

### Integration Tests

**Test full authentication flow:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullAuthenticationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testFullWorkflow_createPatient_runEvaluation_viewCareGaps() {
        // 1. Login as CLINICIAN
        LoginRequest loginRequest = new LoginRequest("test_clinician", "password123");
        LoginResponse loginResponse = restTemplate.postForObject("/auth/login", loginRequest, LoginResponse.class);
        String token = loginResponse.getToken();

        // 2. Create patient (CLINICIAN has PATIENT_WRITE)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Tenant-ID", "TENANT-001");

        CreatePatientRequest createRequest = new CreatePatientRequest("John", "Doe", "1980-01-01", "MALE");
        HttpEntity<CreatePatientRequest> requestEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<PatientResponse> createResponse =
            restTemplate.exchange("/api/v1/patients", HttpMethod.POST, requestEntity, PatientResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. Try to execute measure (CLINICIAN lacks MEASURE_EXECUTE)
        EvaluationRequest evalRequest = new EvaluationRequest(createResponse.getBody().getId(), "COL-001");
        HttpEntity<EvaluationRequest> evalEntity = new HttpEntity<>(evalRequest, headers);

        ResponseEntity<EvaluationResponse> evalResponse =
            restTemplate.exchange("/api/v1/evaluations", HttpMethod.POST, evalEntity, EvaluationResponse.class);

        assertThat(evalResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);  // Forbidden - lacks MEASURE_EXECUTE
    }
}
```

---

## HIPAA Compliance

### PHI Access Permissions

**Permissions that grant PHI access:**
- `PATIENT_READ`
- `PATIENT_WRITE`
- `PATIENT_DELETE`
- `PATIENT_EXPORT`
- `CARE_GAP_READ`
- `CARE_GAP_WRITE`

**Roles with PHI access:**
- SUPER_ADMIN
- ADMIN
- CLINICAL_ADMIN
- CLINICIAN
- QUALITY_OFFICER
- CARE_COORDINATOR
- AUDITOR

### Audit Logging

**All PHI-related permissions trigger automatic audit logging:**

```java
@PreAuthorize("hasPermission('PATIENT_READ')")
@Audited(eventType = "PATIENT_ACCESS")
public PatientResponse getPatient(String patientId) {
    // Automatically logged:
    // - User ID
    // - Patient ID
    // - Timestamp
    // - Access reason (from X-Access-Reason header if provided)
    // - IP address
    // - Tenant ID
}
```

### HIPAA §164.312(b) Compliance

**Requirement:** Implement hardware, software, and procedural mechanisms that record and examine activity in information systems that contain or use electronic protected health information.

**HDIM Implementation:**
- ✅ All PHI access logged via `@Audited` annotation
- ✅ Audit logs include user, patient, timestamp, action
- ✅ Audit logs retained for 6 years (configurable)
- ✅ Read-only audit access for AUDITOR role
- ✅ Tamper-proof audit trail (append-only)

---

## Quick Reference

### Role Permission Counts

| Role | Permissions | PHI Access |
|------|-------------|------------|
| SUPER_ADMIN | 31 | ✅ Yes |
| ADMIN | 27 | ✅ Yes |
| CLINICAL_ADMIN | 16 | ✅ Yes |
| QUALITY_OFFICER | 14 | ✅ Yes |
| CLINICIAN | 7 | ✅ Yes |
| MEASURE_DEVELOPER | 6 | ✅ Yes (read) |
| EVALUATOR | 6 | ✅ Yes (read) |
| CARE_COORDINATOR | 5 | ✅ Yes |
| AUDITOR | 5 | ✅ Yes (read) |
| ANALYST | 5 | ❌ No (de-identified only) |
| DEVELOPER | 5 | ❌ No |
| VIEWER | 2 | ❌ No |
| RESTRICTED | 0 | ❌ No |

### Common Permission Checks

**Patient Management:**
- View patient: `@PreAuthorize("hasPermission('PATIENT_READ')")`
- Create patient: `@PreAuthorize("hasPermission('PATIENT_WRITE')")`
- Delete patient: `@PreAuthorize("hasPermission('PATIENT_DELETE')")`

**Quality Measures:**
- View measures: `@PreAuthorize("hasPermission('MEASURE_READ')")`
- Run evaluation: `@PreAuthorize("hasPermission('MEASURE_EXECUTE')")`
- Create measure: `@PreAuthorize("hasPermission('MEASURE_WRITE')")`

**Care Gaps:**
- View gaps: `@PreAuthorize("hasPermission('CARE_GAP_READ')")`
- Close gap: `@PreAuthorize("hasPermission('CARE_GAP_CLOSE')")`
- Assign gap: `@PreAuthorize("hasPermission('CARE_GAP_ASSIGN')")`

---

## Related Documentation

- **[Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)** - JWT authentication flow
- **[HIPAA Compliance](../HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
- **[API Getting Started Guide](../../docs/api/GETTING_STARTED.md)** - API authentication
- **[CLAUDE.md](../../CLAUDE.md)** - HDIM development quick reference

---

**Last Updated:** January 24, 2026
**Document Version:** 2.0
**Status:** ✅ Production Ready
