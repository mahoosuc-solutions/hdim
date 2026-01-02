# Cost Monitoring and Optimization Guide

## Overview

This document outlines the cost monitoring strategy for the HDIM platform, covering infrastructure costs, resource utilization, and optimization recommendations.

## Cost Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Cost Monitoring Stack                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Cloud Cost   │  │ Prometheus   │  │ Custom       │       │
│  │ APIs         │  │ Metrics      │  │ Collectors   │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                  │                  │              │
│         ▼                  ▼                  ▼              │
│  ┌───────────────────────────────────────────────────┐      │
│  │              Cost Aggregation Service              │      │
│  │         (OpenCost / Kubecost / Custom)            │      │
│  └───────────────────────────────────────────────────┘      │
│                           │                                  │
│         ┌─────────────────┼─────────────────┐               │
│         ▼                 ▼                 ▼               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Grafana    │  │    Alerts    │  │   Reports    │      │
│  │  Dashboards  │  │  (Slack/PD)  │  │   (Weekly)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Cost Categories

### 1. Infrastructure Costs

| Category | Components | Estimated % |
|----------|------------|-------------|
| Compute | Kubernetes nodes, VMs | 40% |
| Database | PostgreSQL, Redis | 25% |
| Storage | Persistent volumes, backups | 15% |
| Network | Load balancers, egress | 10% |
| Observability | Logging, tracing, metrics | 10% |

### 2. Per-Service Cost Attribution

```yaml
# Kubernetes labels for cost allocation
metadata:
  labels:
    app: gateway-service
    cost-center: platform-core
    team: platform-engineering
    environment: production
    tenant: shared  # or specific tenant for dedicated resources
```

### 3. Per-Tenant Cost Tracking

For multi-tenant cost allocation:

```sql
-- Resource usage per tenant
SELECT
    tenant_id,
    SUM(api_calls) as total_calls,
    SUM(storage_bytes) as total_storage,
    SUM(compute_seconds) as total_compute
FROM resource_usage
WHERE date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY tenant_id;
```

## Cost Monitoring Setup

### OpenCost Deployment (Kubernetes)

```yaml
# kubernetes/cost-monitoring/opencost.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: opencost
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: opencost
  template:
    metadata:
      labels:
        app: opencost
    spec:
      containers:
        - name: opencost
          image: ghcr.io/opencost/opencost:1.108.0
          ports:
            - containerPort: 9003
          env:
            - name: PROMETHEUS_SERVER_ENDPOINT
              value: "http://prometheus:9090"
            - name: CLOUD_PROVIDER_API_KEY
              valueFrom:
                secretKeyRef:
                  name: cloud-credentials
                  key: api-key
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: 500m
              memory: 512Mi
---
apiVersion: v1
kind: Service
metadata:
  name: opencost
  namespace: monitoring
spec:
  ports:
    - port: 9003
      targetPort: 9003
  selector:
    app: opencost
```

### Prometheus Cost Metrics

```yaml
# docker/prometheus/cost-rules.yml
groups:
  - name: cost_metrics
    rules:
      # Container resource costs
      - record: hdim:container_cpu_cost_hourly
        expr: |
          sum(rate(container_cpu_usage_seconds_total[1h])) by (namespace, pod)
          * on() group_left() hdim:cpu_cost_per_core_hour

      - record: hdim:container_memory_cost_hourly
        expr: |
          sum(container_memory_usage_bytes) by (namespace, pod) / 1024 / 1024 / 1024
          * on() group_left() hdim:memory_cost_per_gb_hour

      # Service-level costs
      - record: hdim:service_cost_daily
        expr: |
          sum(hdim:container_cpu_cost_hourly + hdim:container_memory_cost_hourly)
          by (namespace, service) * 24

      # Tenant-level costs
      - record: hdim:tenant_cost_daily
        expr: |
          sum(hdim:service_cost_daily) by (tenant)

      # Cost anomaly detection
      - alert: CostAnomalyDetected
        expr: |
          hdim:service_cost_daily > (hdim:service_cost_daily offset 7d) * 1.5
        for: 6h
        labels:
          severity: warning
        annotations:
          summary: "Cost anomaly for {{ $labels.service }}"
          description: "Daily cost is 50% higher than last week"
```

## Cost Dashboards

### Grafana Dashboard Configuration

```json
{
  "title": "HDIM Cost Dashboard",
  "panels": [
    {
      "title": "Daily Infrastructure Cost",
      "type": "stat",
      "targets": [
        {
          "expr": "sum(hdim:service_cost_daily)",
          "legendFormat": "Total Daily Cost"
        }
      ]
    },
    {
      "title": "Cost by Service",
      "type": "piechart",
      "targets": [
        {
          "expr": "sum(hdim:service_cost_daily) by (service)",
          "legendFormat": "{{ service }}"
        }
      ]
    },
    {
      "title": "Cost Trend (30 days)",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum(hdim:service_cost_daily)",
          "legendFormat": "Daily Cost"
        }
      ]
    },
    {
      "title": "Cost per Tenant",
      "type": "table",
      "targets": [
        {
          "expr": "topk(10, sum(hdim:tenant_cost_daily) by (tenant))",
          "format": "table"
        }
      ]
    }
  ]
}
```

## Cost Alerts

### Budget Alerts

```yaml
# Alert when approaching budget limits
groups:
  - name: budget_alerts
    rules:
      - alert: MonthlyBudget80Percent
        expr: |
          sum(hdim:service_cost_daily) * day_of_month() / day_in_month()
          > hdim:monthly_budget * 0.8
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Approaching 80% of monthly budget"

      - alert: MonthlyBudgetExceeded
        expr: |
          sum(hdim:service_cost_daily) * day_of_month()
          > hdim:monthly_budget
        for: 1h
        labels:
          severity: critical
        annotations:
          summary: "Monthly budget exceeded"

      - alert: UnexpectedCostSpike
        expr: |
          sum(rate(hdim:service_cost_daily[1h]))
          > sum(rate(hdim:service_cost_daily[1h] offset 1d)) * 2
        for: 2h
        labels:
          severity: warning
        annotations:
          summary: "Cost spike detected - 2x normal rate"
```

## Cost Optimization Recommendations

### 1. Right-Sizing

```sql
-- Identify over-provisioned services
SELECT
    service_name,
    avg_cpu_usage,
    requested_cpu,
    (requested_cpu - avg_cpu_usage) / requested_cpu * 100 as waste_percent
FROM (
    SELECT
        service_name,
        AVG(cpu_usage) as avg_cpu_usage,
        MAX(requested_cpu) as requested_cpu
    FROM resource_metrics
    WHERE timestamp > NOW() - INTERVAL '7 days'
    GROUP BY service_name
) metrics
WHERE (requested_cpu - avg_cpu_usage) / requested_cpu > 0.5
ORDER BY waste_percent DESC;
```

### 2. Auto-Scaling Optimization

```yaml
# HPA with cost-aware scaling
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine-service
  minReplicas: 2
  maxReplicas: 10
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 min before scaling down
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 30
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70  # Higher target = fewer pods
```

### 3. Reserved Capacity

| Resource | On-Demand Cost | Reserved (1yr) | Savings |
|----------|---------------|----------------|---------|
| Compute (4 nodes) | $2,000/mo | $1,400/mo | 30% |
| Database (RDS) | $800/mo | $500/mo | 37% |
| Redis | $200/mo | $140/mo | 30% |

### 4. Storage Optimization

```yaml
# Storage class tiering
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: hdim-cold-storage
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-standard  # Cheaper than pd-ssd
reclaimPolicy: Retain
---
# Use for backups and archives
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: backup-pvc
spec:
  storageClassName: hdim-cold-storage
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 500Gi
```

## Cost Reports

### Weekly Cost Report Template

```markdown
# HDIM Weekly Cost Report
**Period**: {{ start_date }} - {{ end_date }}

## Summary
- **Total Cost**: ${{ total_cost }}
- **Budget Used**: {{ budget_percent }}%
- **Week-over-Week Change**: {{ wow_change }}%

## Cost Breakdown by Category
| Category | Cost | % of Total |
|----------|------|------------|
| Compute | ${{ compute_cost }} | {{ compute_pct }}% |
| Database | ${{ db_cost }} | {{ db_pct }}% |
| Storage | ${{ storage_cost }} | {{ storage_pct }}% |
| Network | ${{ network_cost }} | {{ network_pct }}% |

## Top 5 Cost Drivers
{{ top_5_services }}

## Anomalies Detected
{{ anomalies_list }}

## Recommendations
{{ recommendations }}
```

## Integration with Cloud Providers

### AWS Cost Explorer Integration

```python
# scripts/aws_cost_collector.py
import boto3
from datetime import datetime, timedelta

def get_aws_costs():
    client = boto3.client('ce')

    response = client.get_cost_and_usage(
        TimePeriod={
            'Start': (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d'),
            'End': datetime.now().strftime('%Y-%m-%d')
        },
        Granularity='DAILY',
        Metrics=['UnblendedCost'],
        GroupBy=[
            {'Type': 'TAG', 'Key': 'service'},
            {'Type': 'TAG', 'Key': 'environment'}
        ]
    )

    return response['ResultsByTime']
```

### GCP Billing Export

```sql
-- BigQuery query for GCP costs
SELECT
  service.description as service,
  SUM(cost) as total_cost,
  SUM(usage.amount) as usage_amount,
  usage.unit
FROM `project.dataset.gcp_billing_export`
WHERE
  _PARTITIONTIME >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  AND labels.key = 'app'
  AND labels.value LIKE 'hdim-%'
GROUP BY service, usage.unit
ORDER BY total_cost DESC;
```

## Tenant Cost Allocation

### Usage-Based Billing Metrics

```java
@Service
public class TenantUsageTracker {

    @Autowired
    private MeterRegistry meterRegistry;

    public void trackApiCall(String tenantId, String endpoint) {
        meterRegistry.counter("hdim.tenant.api.calls",
            "tenant", tenantId,
            "endpoint", endpoint
        ).increment();
    }

    public void trackDataStorage(String tenantId, long bytes) {
        meterRegistry.gauge("hdim.tenant.storage.bytes",
            Tags.of("tenant", tenantId),
            bytes
        );
    }

    public void trackComputeTime(String tenantId, long milliseconds) {
        meterRegistry.timer("hdim.tenant.compute.time",
            "tenant", tenantId
        ).record(Duration.ofMillis(milliseconds));
    }
}
```

## Action Items

1. [ ] Deploy OpenCost to Kubernetes cluster
2. [ ] Configure Prometheus cost recording rules
3. [ ] Create Grafana cost dashboard
4. [ ] Set up budget alerts in Slack
5. [ ] Implement tenant usage tracking
6. [ ] Schedule weekly cost reports
7. [ ] Review and right-size resources quarterly
