# Docker Demo Deployment Issues & Correction Plan

## Executive Summary
The current `docker-compose-demo.yml` contains critical configuration mismatches that will prevent services from communicating with each other. While the `start-demo.sh` script works because it runs services on localhost using default ports, the Docker container deployment fails to override these defaults correctly due to mismatched environment variable names and port assignments.

## Identified Issues

### 1. Environment Variable Mismatches
The `docker-compose-demo.yml` uses environment variable names that do not map to the Spring Boot configuration properties defined in `application.yml` files.

**Gateway Service (`gateway-service`)**
- **Docker uses:** `FHIR_SERVICE_URL`, `CQL_ENGINE_SERVICE_URL`, `QUALITY_MEASURE_SERVICE_URL`, `PATIENT_SERVICE_URL`, `CARE_GAP_SERVICE_URL`
- **Application expects:** `BACKEND_SERVICES_FHIR_URL`, `BACKEND_SERVICES_CQL_ENGINE_URL`, `BACKEND_SERVICES_QUALITY_MEASURE_URL`, `BACKEND_SERVICES_PATIENT_URL`, `BACKEND_SERVICES_CARE_GAP_URL`
- **Result:** Gateway uses default `localhost` URLs inside the container, failing to reach other services.

**Other Services (Patient, Care Gap, Quality Measure, CQL Engine)**
- **Docker uses:** `FHIR_SERVICE_URL`, `CQL_ENGINE_SERVICE_URL`
- **Application expects:** `FHIR_SERVER_URL` (maps to `fhir.server.url`), `CQL_ENGINE_URL` (maps to `cql.engine.url`)
- **Result:** Services fail to coordinate (e.g., Care Gap service cannot query FHIR data).

### 2. Port Configuration Inconsistencies
There is a confusion of ports between the Docker composition and the application defaults. The Docker Compose file explicitly sets `SERVER_PORT` to values that differ from the application defaults, making the URL overrides even more critical.

| Service | Application Default Port | Docker Compose Port | Status |
|---------|--------------------------|---------------------|--------|
| FHIR Service | 8085 | 8081 | **CHANGED** |
| CQL Engine | 8081 | 8086 | **CHANGED** |
| Care Gap | 8086 | 8085 | **CHANGED** |
| Patient | 8084 | 8084 | Match |
| Quality Measure | 8087 | 8087 | Match |
| Gateway | 8080 | 9000 | **CHANGED** |

Because ports are changed in Docker, the default URLs (e.g., `http://localhost:8085/fhir`) are incorrect both in hostname (should be service name) and port (should be 8081).

### 3. Missing Configuration Profile
The `gateway-service` uses `SPRING_PROFILES_ACTIVE: docker,demo`, but there is no `application-demo.yml` for the gateway service. This risks missing configurations if `demo` profile logic is expected but defined nowhere. However, since the redundancy is mainly in `application.yml` conditional blocks or defaults, this is less critical than the Env Var mismatch.

## Correction Plan

I will update `docker-compose-demo.yml` to use the correct environment variable names that match the Spring Boot `application.yml` properties.

### Required Changes

1.  **Gateway Service Updates**:
    *   Change `FHIR_SERVICE_URL` -> `BACKEND_SERVICES_FHIR_URL`
    *   Change `CQL_ENGINE_SERVICE_URL` -> `BACKEND_SERVICES_CQL_ENGINE_URL`
    *   Change `QUALITY_MEASURE_SERVICE_URL` -> `BACKEND_SERVICES_QUALITY_MEASURE_URL`
    *   Change `PATIENT_SERVICE_URL` -> `BACKEND_SERVICES_PATIENT_URL`
    *   Change `CARE_GAP_SERVICE_URL` -> `BACKEND_SERVICES_CARE_GAP_URL`

2.  **Service Inter-dependency Updates**:
    *   Change `FHIR_SERVICE_URL` -> `FHIR_SERVER_URL` (for Patient, Care Gap, Quality, CQL)
    *   Change `CQL_ENGINE_SERVICE_URL` -> `CQL_ENGINE_URL` (for Care Gap, Quality)

3.  **Verification**:
    *   Ensure all `SERVER_PORT` settings align with the URLs provided in the environment variables.

### Proposed `docker-compose-demo.yml` Snippet (Example)

```yaml
  gateway-service:
    environment:
      # ...
      BACKEND_SERVICES_FHIR_URL: http://fhir-service:8081
      BACKEND_SERVICES_CQL_ENGINE_URL: http://cql-engine-service:8086
      # ...
```

## Next Steps
1. Apply the fixes to `docker-compose-demo.yml`.
2. (Optional) Create `application-demo.yml` for Gateway if specific demo behavior is missing.
