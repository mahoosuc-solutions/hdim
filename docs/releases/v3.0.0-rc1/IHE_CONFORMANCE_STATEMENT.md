# IHE Conformance Statement

## Vendor Information

| Field | Value |
|-------|-------|
| **Product Name** | HealthData-in-Motion (HDIM) |
| **Version** | v3.0.0-rc1 "Shield" |
| **Vendor** | HDIM Project |
| **Release Date** | March 2026 |
| **Conformance Date** | March 7, 2026 |

---

## IHE Integration Profiles

### XDS.b -- Cross-Enterprise Document Sharing

HDIM implements the XDS.b profile for cross-enterprise document sharing, translating traditional IHE transactions to FHIR MHD (Mobile Health Documents) REST operations.

| Transaction | IHE ID | Actor | Implementation | Transport |
|-------------|--------|-------|----------------|-----------|
| Registry Stored Query | ITI-18 | Document Consumer | FHIR MHD REST query against DocumentReference resources | FHIR R4 REST |
| Provide and Register Document Set | ITI-41 | Document Source | FHIR MHD REST Bundle submission to DocumentReference + Binary | FHIR R4 REST |
| Retrieve Document Set | ITI-43 | Document Consumer | FHIR MHD REST retrieval of Binary resources by DocumentReference | FHIR R4 REST |

**Implementation Notes:**

- All XDS.b transactions are mapped to FHIR R4 DocumentReference and Binary resources via the MHD (Mobile Health Documents) profile.
- ITI-18 queries are translated to FHIR search operations on DocumentReference with support for `patient`, `date`, `type`, `status`, and `class` search parameters.
- ITI-41 submissions are translated to FHIR transaction Bundles containing DocumentReference and Binary resources.
- ITI-43 retrievals resolve DocumentReference attachments to Binary resource content.
- Document metadata conforms to the MHD DocumentReference profile with XDS.b metadata mapping.

---

### PIXv3 -- Patient Identifier Cross-Referencing

HDIM implements the PIXv3 profile for patient identity cross-referencing, translating HL7v3 patient identity queries to FHIR Patient $match operations.

| Transaction | IHE ID | Actor | Implementation | Transport |
|-------------|--------|-------|----------------|-----------|
| PIXV3 Query | ITI-45 | Patient Identifier Cross-reference Consumer | FHIR Patient `$match` operation | FHIR R4 REST |

**Implementation Notes:**

- ITI-45 PIXv3 queries are translated to the FHIR `Patient/$match` operation against the configured MPI (Master Patient Index).
- The `$match` operation uses patient demographics (name, date of birth, gender, identifiers) to find cross-referenced patient identities.
- Matching confidence scores are returned via the `search.score` field in the FHIR Bundle response.
- Supports both deterministic and probabilistic matching depending on the MPI configuration.

---

### XCA -- Cross-Community Access

HDIM implements the XCA profile for cross-community document query and retrieval, acting as both an Initiating Gateway and Responding Gateway.

| Transaction | IHE ID | Actor | Implementation | Transport |
|-------------|--------|-------|----------------|-----------|
| Cross Gateway Query | ITI-38 | Initiating Gateway, Responding Gateway | Federated FHIR DocumentReference search across communities | FHIR R4 REST |
| Cross Gateway Retrieve | ITI-39 | Initiating Gateway, Responding Gateway | Federated FHIR Binary retrieval across communities | FHIR R4 REST |

**Implementation Notes:**

- **Initiating Gateway:** Fans out ITI-38 queries to configured Responding Gateways, aggregates results, and deduplicates DocumentReference resources before returning to the consumer.
- **Responding Gateway:** Receives ITI-38 queries from external Initiating Gateways and responds with locally available DocumentReference resources.
- ITI-39 retrieval follows the same fan-out pattern, routing retrieval requests to the community that owns the document.
- Cross-community trust is established via mTLS with community-specific certificates.
- Query fan-out uses parallel asynchronous requests with configurable timeouts per community.
- OpenTelemetry trace context is propagated across community boundaries for end-to-end observability.

---

### ATNA -- Audit Trail and Node Authentication

HDIM implements the ATNA profile for audit trail recording, using Kafka as the Audit Record Repository (ARR) transport.

| Transaction | IHE ID | Actor | Implementation | Transport |
|-------------|--------|-------|----------------|-----------|
| Record Audit Event | ITI-20 | Secure Node / Secure Application | Kafka-based Audit Record Repository (ARR) | Apache Kafka |

**Implementation Notes:**

- All IHE transactions (ITI-18, ITI-41, ITI-43, ITI-45, ITI-38, ITI-39) generate ATNA audit events.
- Audit events are published to Kafka topics using the `external.*` topic prefix pattern.
- Audit records include: event identification, active participant(s), audit source, and participant objects per the DICOM/IHE audit message format.
- PHI classification level (NONE, LIMITED, FULL) is attached to each audit event as metadata.
- Node authentication is enforced via mTLS for FULL PHI-level services (Healthix, IHE Gateway).

---

## Security

### Mutual TLS (mTLS)

- Required for all FULL PHI-level communications (Healthix Adapter, IHE Gateway).
- Client and server certificates are validated on every connection.
- Certificate chain validation follows X.509 standards.
- Controlled via `HEALTHIX_MTLS_ENABLED` environment variable.

### JWT + RBAC

- All adapter API endpoints require valid JWT Bearer tokens.
- Role-based access control (RBAC) enforces authorization at the endpoint level.
- JWT tokens are validated using the shared `JWT_SECRET`.
- Role hierarchy: SUPER_ADMIN > ADMIN > EVALUATOR > ANALYST > VIEWER.

### PHI Classification

All data flowing through the external adapters is classified by PHI exposure level:

| PHI Level | Adapters | Security Requirements |
|-----------|----------|----------------------|
| **NONE** | CoreHive Adapter | Standard JWT authentication; all data de-identified before crossing boundary |
| **LIMITED** | HEDIS Adapter | JWT + RBAC; limited demographic data; no clinical narratives |
| **FULL** | Healthix Adapter, IHE Gateway | mTLS + BAA (Business Associate Agreement); full clinical data; HIPAA audit logging |

---

## Transport Protocols

| Protocol | Usage | Specification |
|----------|-------|---------------|
| FHIR R4 REST | Primary API transport for all IHE transactions | HL7 FHIR R4 (v4.0.1) |
| FHIR MHD | Document sharing (XDS.b translation layer) | IHE MHD (Mobile access to Health Documents) |
| Apache Kafka | Audit trail (ATNA/ITI-20), event-driven integration | Apache Kafka 3.x with `external.*` topics |

---

## Oregon HIE Compliance

HDIM v3.0.0-rc1 is designed to comply with Oregon state requirements for Health Information Exchange participation.

### Regulatory Compliance

| Regulation | Description | HDIM Compliance |
|------------|-------------|-----------------|
| **ORS 192.553** | Oregon Health Information Technology | PHI classification and access controls enforce data sharing restrictions per state law. Feature toggles allow selective participation in HIE networks. |
| **OAR 943-120** | Oregon Health Authority -- Health Information Exchange Rules | IHE XDS.b/XCA profiles support standardized document exchange. ATNA audit trail meets record-keeping requirements. mTLS enforces node authentication per OAR guidelines. |
| **42 CFR Part 2** | Confidentiality of Substance Use Disorder Patient Records | PHI classification system distinguishes substance use disorder records. FULL PHI-level services enforce additional consent-based access controls. Audit records track all access to Part 2 protected information. |
| **Oregon CCO Metrics** | Coordinated Care Organization Quality Measures | HEDIS Adapter integrates with CCO quality measure reporting. CQL evaluation engine supports Oregon-specific measure definitions. Quality measure results are exchanged via FHIR MeasureReport resources. |

### Compliance Features

- **Consent Management:** PHI-level tagging ensures data is only shared with appropriate consent.
- **Audit Trail:** All transactions are logged via ATNA (ITI-20) with full provenance tracking.
- **Data Segmentation:** PHI classification (NONE/LIMITED/FULL) enables granular data sharing policies.
- **Break-the-Glass:** Emergency access workflows are audited with elevated logging.
- **Patient Right of Access:** FHIR Patient and DocumentReference APIs support patient data export.

---

## Conformance Testing

### Test Environment

Conformance was validated against:

- HAPI FHIR Server (R4, v7.x)
- IHE Gazelle Test Platform (simulated)
- Internal end-to-end smoke tests (`scripts/tests/smoke-test.sh`)
- Performance validation suite (`scripts/tests/perf-validation.sh`)

### Known Limitations

- XDS.b metadata mapping covers the core required metadata elements; optional metadata attributes may not be fully mapped.
- PIXv3 (ITI-45) supports query operations only; patient identity feed (ITI-44) is not implemented in this release.
- XCA cross-community discovery relies on static configuration; dynamic community discovery (e.g., via HPD) is planned for a future release.
- ATNA audit messages use a Kafka-based transport rather than traditional Syslog (RFC 5424); organizations requiring Syslog transport should deploy a Kafka-to-Syslog bridge.

---

## References

- [IHE IT Infrastructure Technical Framework](https://profiles.ihe.net/ITI/)
- [HL7 FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [IHE MHD Profile](https://profiles.ihe.net/ITI/MHD/)
- [Oregon Health Authority -- HIE Rules](https://www.oregon.gov/oha/HPA/OHIT/Pages/HIE-Rules.aspx)
- [42 CFR Part 2](https://www.ecfr.gov/current/title-42/chapter-I/subchapter-A/part-2)
