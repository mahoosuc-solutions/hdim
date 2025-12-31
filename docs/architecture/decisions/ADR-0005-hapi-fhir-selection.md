# ADR-0005: HAPI FHIR for FHIR R4 Implementation

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Clinical Informatics Team
**Technical Story**: Need FHIR R4 compliant resource server for clinical data management

---

## Context and Problem Statement

HDIM requires a FHIR R4 compliant server for storing and serving clinical resources (Patient, Observation, Condition, MedicationRequest, etc.). The implementation must support:

- Multi-tenant data isolation (required for HIPAA)
- High performance (100+ requests/sec per tenant)
- HIPAA compliance (encryption, audit logging)
- Standard FHIR R4 operations (create, read, update, delete, search)
- Custom search parameters for quality measure evaluation
- Integration with CQL execution engine
- Support for FHIR Bulk Data Export

---

## Decision Drivers

* **FHIR R4 compliance certification** - Must be certified for FHIR R4 conformance
* **Production-ready maturity** - Proven in healthcare production environments
* **Performance at scale** - Must handle 1M+ patients across tenants
* **Multi-tenancy support** - Data isolation by tenant ID
* **Spring Boot integration** - Native integration with existing Java stack
* **Open source licensing** - Prefer Apache 2.0 or similar
* **Healthcare industry adoption** - Prefer industry-standard solutions
* **Extensibility** - Support for custom operations and interceptors

---

## Considered Options

1. **HAPI FHIR 7.x** - Open-source Java FHIR implementation
2. **Microsoft FHIR Server for Azure** - Cloud-managed FHIR server
3. **Google Cloud Healthcare API** - GCP-managed FHIR service
4. **IBM FHIR Server** - Open-source Java implementation
5. **Build Custom FHIR Server** - Custom implementation on Spring Boot

---

## Decision Outcome

**Chosen option**: "HAPI FHIR 7.x"

**Rationale**: HAPI FHIR is the industry-standard open-source FHIR implementation with:
- Apache 2.0 license (no vendor lock-in)
- Proven in production healthcare environments (Epic, Cerner integrations)
- Native Spring Boot integration via HAPI FHIR Starter
- Extensive customization capabilities for search parameters and interceptors
- Strong performance characteristics (tested to 500+ req/sec)
- Active community with regular updates to FHIR specification
- Multi-tenant support via tenant identifier interceptors
- Comprehensive audit logging and security features

---

## Consequences

### Positive

* **Faster development**: Pre-built FHIR operations reduce custom code by ~60%
* **Standards compliance**: FHIR R4 conformance guaranteed
* **Community support**: Large user base for troubleshooting
* **Flexibility**: Can customize search parameters, operations, and interceptors
* **Performance**: Battle-tested in healthcare production environments
* **Future-proof**: Regular updates as FHIR specification evolves

### Negative

* **Learning curve**: Team needs to understand HAPI-specific patterns (interceptors, resource providers)
* **Bundle size**: HAPI FHIR adds ~50MB to service artifacts
* **Opinionated architecture**: Must work within HAPI's interceptor model
* **JPA dependency**: Tightly coupled to JPA/Hibernate for persistence

**Mitigations**:
- Provide team training on HAPI FHIR architecture
- Accept larger bundle size as trade-off for functionality
- Document HAPI patterns in service README

### Neutral

* Requires Java 17+ (aligns with our Java 21 stack)
* PostgreSQL support is first-class (matches our database choice)

---

## Pros and Cons of Options

### Option 1: HAPI FHIR 7.x

Open-source Java FHIR implementation from University Health Network.

| Criterion | Assessment |
|-----------|------------|
| FHIR R4 Compliance | **Good** - Certified conformance, regularly updated |
| Multi-tenancy | **Good** - Tenant identifier interceptors available |
| Performance | **Good** - 500+ req/sec proven in production |
| Spring Boot Integration | **Good** - HAPI FHIR Starter provides native integration |
| Customization | **Good** - Interceptors, resource providers, custom operations |
| Open Source | **Good** - Apache 2.0 license |
| Learning Curve | **Neutral** - Requires understanding HAPI patterns |
| Bundle Size | **Bad** - ~50MB added to artifacts |

**Summary**: Industry standard with best balance of features, performance, and flexibility.

---

### Option 2: Microsoft FHIR Server for Azure

Cloud-managed FHIR server on Azure.

| Criterion | Assessment |
|-----------|------------|
| FHIR R4 Compliance | **Good** - Microsoft certified |
| Multi-tenancy | **Good** - Built-in Azure AD integration |
| Performance | **Good** - Managed scaling |
| Spring Boot Integration | **Bad** - Requires Azure SDK, not native |
| Customization | **Bad** - Limited to Azure capabilities |
| Open Source | **Neutral** - Open source but Azure-dependent |
| Vendor Lock-in | **Bad** - Requires Azure infrastructure |

**Summary**: Good managed option but creates Azure dependency.

---

### Option 3: Google Cloud Healthcare API

GCP-managed FHIR service.

| Criterion | Assessment |
|-----------|------------|
| FHIR R4 Compliance | **Good** - Google certified |
| Multi-tenancy | **Good** - Project-level isolation |
| Performance | **Good** - GCP managed scaling |
| Spring Boot Integration | **Bad** - Requires GCP SDK |
| Customization | **Bad** - Limited to GCP capabilities |
| GCP Integration | **Good** - BigQuery, Pub/Sub integration |
| Vendor Lock-in | **Bad** - Requires GCP infrastructure |

**Summary**: Good for GCP-native architectures but creates dependency.

---

### Option 4: IBM FHIR Server

Open-source Java FHIR implementation from IBM.

| Criterion | Assessment |
|-----------|------------|
| FHIR R4 Compliance | **Good** - IBM certified |
| Multi-tenancy | **Neutral** - Less documented than HAPI |
| Performance | **Good** - IBM enterprise quality |
| Spring Boot Integration | **Neutral** - Not as seamless as HAPI |
| Community Size | **Bad** - Smaller than HAPI |
| Healthcare Adoption | **Neutral** - Less common in industry |

**Summary**: Viable alternative but smaller community and less documentation.

---

### Option 5: Build Custom FHIR Server

Custom implementation on Spring Boot.

| Criterion | Assessment |
|-----------|------------|
| FHIR R4 Compliance | **Bad** - Would require custom conformance testing |
| Development Time | **Bad** - 6+ months estimated |
| Customization | **Good** - Full control |
| Maintenance Burden | **Bad** - Ongoing FHIR spec updates |
| Risk | **Bad** - Potential for conformance issues |

**Summary**: Maximum flexibility but prohibitive development and maintenance cost.

---

## Implementation Notes

### Version Selected

**HAPI FHIR 7.0.2** - Latest stable release as of Q3 2024

### Deployment Model

Embedded in `fhir-service` microservice (port 8085)

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/${POSTGRES_DB:healthdata_qm}
  jpa:
    hibernate:
      ddl-auto: validate  # Schema managed by Liquibase

hapi:
  fhir:
    server:
      path: /fhir/*
    fhir_version: R4
    tenant_identification_strategy: URL_BASED
```

### Customizations Implemented

1. **Multi-tenant Interceptor**: Filters all queries by `tenantId`
2. **Cache Interceptor**: 5-minute TTL for PHI (HIPAA compliance)
3. **Audit Interceptor**: Logs all PHI access to Kafka
4. **Custom Search Parameters**: Quality measure-specific searches
5. **Resource Validation**: FHIR R4 profile enforcement

### Performance Targets

| Operation | Target Latency (p95) | Actual (Dec 2024) |
|-----------|---------------------|-------------------|
| Read | <50ms | 35ms |
| Write | <200ms | 150ms |
| Search (simple) | <500ms | 400ms |
| Search (complex) | <1000ms | 850ms |
| Throughput | 200+ req/sec | 220 req/sec |

---

## Links

* [HAPI FHIR Documentation](https://hapifhir.io/)
* [FHIR R4 Specification](https://hl7.org/fhir/R4/)
* [FHIR Service README](/backend/modules/services/fhir-service/README.md)
* Related: [ADR-0007 - PostgreSQL Database](ADR-0007-postgresql-database.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added performance actuals, formatting |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
