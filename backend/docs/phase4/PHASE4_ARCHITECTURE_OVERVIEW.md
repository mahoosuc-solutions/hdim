# Phase 4 Architecture Overview

## Scope

Phase 4 observability extends HDIM with anomaly detection, correlation, cost attribution, KPI tracking, predictive alerting, distributed tracing, dashboards, and operational readiness.

## Data Flow

1. Metrics scraped from services via Prometheus.
2. Anomaly detection classifies abnormal behavior.
3. Correlation engine links anomalies across services.
4. Root-cause candidates and impact are generated.
5. Cost attribution maps resource usage to service/feature/tenant.
6. KPI framework aggregates clinical and business indicators.
7. Dashboards and alerts drive operations.

## Components

- Metrics ingestion: Prometheus + exporters
- Alert routing: Alertmanager
- Visualization: Grafana dashboards
- Tracing: Jaeger and trace-aware analysis
- Persistence: PostgreSQL summaries and historical views

## Integration Points

- Gateway and service health endpoints
- Audit and compliance logging services
- Existing runbooks in `docs/runbooks/`
