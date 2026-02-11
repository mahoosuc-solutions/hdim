# ─────────────────────────────────────────────────────────────────────────────
# hdim/simulation-customer-ehr
# Simulates a customer's existing FHIR-capable EHR (e.g. Epic, Cerner, Athena).
# Uses HAPI FHIR R4 with settings typical of a mid-size health system.
# ─────────────────────────────────────────────────────────────────────────────
FROM hapiproject/hapi:v7.0.2

LABEL maintainer="HDIM Platform Team"
LABEL description="Customer EHR simulation — HAPI FHIR R4 (NOT part of HDIM product)"
LABEL com.hdim.role="simulation"

# Override default HAPI config with customer-like settings
COPY application.yaml /app/config/application.yaml

ENV SPRING_CONFIG_ADDITIONAL_LOCATION=file:/app/config/
