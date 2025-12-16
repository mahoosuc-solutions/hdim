# Analytics Service

Healthcare analytics and business intelligence service providing dashboards, KPIs, reports, and alerts for value-based care programs.

## Overview

The Analytics Service aggregates data from quality measures, HCC risk scores, and care gaps to provide comprehensive analytics dashboards and KPI tracking for healthcare organizations. It supports customizable dashboards, scheduled reports, trend analysis, and real-time alerting.

## Key Features

### Dashboard Management
- Create and manage custom dashboards
- Widget-based composition (charts, tables, metrics)
- User-specific and organization-wide dashboards
- Role-based dashboard access control
- Real-time dashboard updates

### KPI Tracking
- Quality measure KPIs (HEDIS completion rates)
- HCC risk adjustment KPIs (RAF scores, gaps)
- Care gap KPIs (open gaps, closure rates)
- Trend analysis over configurable time periods
- Automated KPI snapshot capture

### Report Generation
- Define custom report templates
- Parameterized report execution
- Scheduled report generation
- Report execution history and versioning
- Export to multiple formats (PDF, Excel, CSV)

### Real-Time Alerts
- Threshold-based alerting
- Multi-channel notifications (email, SMS, webhook)
- Alert acknowledgment and resolution tracking
- Alert escalation rules
- Alert history and audit trail

### Data Aggregation
- Population-level statistics
- Cohort analysis and segmentation
- Time-series metric aggregation
- Cross-measure correlation analysis

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Analytics data warehouse
- **Redis**: Caching and real-time updates
- **Apache Kafka**: Event streaming
- **Feign Clients**: Service integration
- **Resilience4j**: Circuit breakers and retry logic

## API Endpoints

### Dashboards
```
GET    /api/analytics/dashboards
       - Get all dashboards for tenant

GET    /api/analytics/dashboards/accessible
       - Get dashboards accessible to current user

GET    /api/analytics/dashboards/{id}
       - Get specific dashboard

POST   /api/analytics/dashboards
       - Create new dashboard

PUT    /api/analytics/dashboards/{id}
       - Update dashboard

DELETE /api/analytics/dashboards/{id}
       - Delete dashboard
```

### Widgets
```
POST   /api/analytics/dashboards/widgets
       - Add widget to dashboard

PUT    /api/analytics/dashboards/widgets/{id}
       - Update widget configuration

DELETE /api/analytics/dashboards/widgets/{id}
       - Remove widget from dashboard
```

### KPIs
```
GET /api/analytics/kpis
    - Get all KPIs for tenant

GET /api/analytics/kpis/quality
    - Get quality measure KPIs

GET /api/analytics/kpis/hcc
    - Get HCC risk adjustment KPIs

GET /api/analytics/kpis/care-gaps
    - Get care gap KPIs

GET /api/analytics/kpis/trends?metricType={type}&days={days}
    - Get KPI trends over time

GET /api/analytics/kpis/statistics
    - Get snapshot statistics

POST /api/analytics/kpis/capture
     - Trigger KPI snapshot capture
```

### Reports
```
GET  /api/analytics/reports
     - List all reports for tenant

GET  /api/analytics/reports/{id}
     - Get specific report definition

POST /api/analytics/reports
     - Create new report template

PUT  /api/analytics/reports/{id}
     - Update report template

DELETE /api/analytics/reports/{id}
       - Delete report

POST /api/analytics/reports/{id}/execute
     - Execute report with parameters

GET  /api/analytics/reports/{id}/executions
     - Get report execution history

GET  /api/analytics/reports/executions/{executionId}
     - Get specific execution result
```

### Alerts
```
GET    /api/analytics/alerts
       - Get all alerts for tenant

GET    /api/analytics/alerts/active
       - Get active/unacknowledged alerts

POST   /api/analytics/alerts
       - Create new alert rule

PUT    /api/analytics/alerts/{id}
       - Update alert rule

DELETE /api/analytics/alerts/{id}
       - Delete alert rule

POST   /api/analytics/alerts/{id}/acknowledge
       - Acknowledge an alert
```

## Configuration

### Application Properties
```yaml
server.port: 8092
spring.datasource.url: jdbc:postgresql://localhost:5435/healthdata_analytics
spring.cache.type: redis
spring.cache.redis.time-to-live: 300000  # 5 minutes
```

### Service Integration
```yaml
feign.client.config:
  quality-measure-service:
    url: http://localhost:8087
  hcc-service:
    url: http://localhost:8091
  care-gap-service:
    url: http://localhost:8086
```

### Resilience
```yaml
resilience4j:
  circuitbreaker:
    instances:
      analyticsDefault:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+

### Build
```bash
./gradlew :modules:services:analytics-service:build
```

### Run
```bash
./gradlew :modules:services:analytics-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:analytics-service:test
```

## Integration

This service integrates with:
- **Quality Measure Service**: HEDIS measure results
- **HCC Service**: Risk adjustment scores
- **Care Gap Service**: Care gap data
- **Predictive Analytics**: Risk predictions and forecasts

## Security

- JWT-based authentication
- Role-based access control (USER, ANALYST, ADMIN)
- Tenant isolation via X-Tenant-ID header
- Dashboard-level access controls
- Audit logging for sensitive operations

## Data Refresh

### KPI Snapshots
- Automated capture via scheduled jobs
- On-demand capture via API
- Configurable snapshot intervals
- Historical trend preservation

### Cache Strategy
- 5-minute TTL for KPI data
- Immediate invalidation on data updates
- Redis-based distributed caching

## API Documentation

Swagger UI available at:
```
http://localhost:8092/analytics/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## License

Copyright (c) 2024 Mahoosuc Solutions
