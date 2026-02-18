# HDIM k6 Load Test Suite

Performance load tests targeting the 4 core pilot services with a P95 < 200ms SLO at 100 concurrent users.

---

## Prerequisites

### Install k6

```bash
# macOS
brew install k6

# Ubuntu / WSL2
sudo snap install k6

# Or via apt (Ubuntu 20.04+)
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Verify
k6 version
```

k6 documentation: https://k6.io/docs/

### Start the services

```bash
# Start the minimal clinical services stack
docker compose -f docker-compose.minimal-clinical.yml up -d

# Verify all services are healthy before running load tests
docker compose -f docker-compose.minimal-clinical.yml ps
```

---

## Directory Structure

```
load-tests/
  config/
    options.js          Shared k6 options (stages, thresholds, tags)
    auth.js             Auth header helpers + token configuration
  scenarios/
    patient-service.js          Patient demographics + health record + risk
    care-gap-service.js         Care gap list + count
    quality-measure-service.js  Measure results + quality score
    full-pipeline.js            End-to-end 4-step clinical workflow
  results/                      JSON output from runs (git-ignored)
  run-load-tests.sh             Runner script for all scenarios
  README.md                     This file
```

---

## Running the Tests

### Quick smoke test (1 VU — sanity check, ~30 seconds)

```bash
./load-tests/run-load-tests.sh --smoke
```

Use this before a full load run to confirm all services are reachable and returning 200s.

### Full load test (100 VUs, ~6 minutes per scenario)

```bash
./load-tests/run-load-tests.sh
```

This runs all 4 scenarios sequentially and writes results to `load-tests/results/`.

### Run a specific scenario only

```bash
./load-tests/run-load-tests.sh --scenario patient
./load-tests/run-load-tests.sh --scenario care-gap
./load-tests/run-load-tests.sh --scenario quality
./load-tests/run-load-tests.sh --scenario pipeline
```

### Run a single scenario directly with k6

```bash
# Patient service — smoke test
k6 run -e TEST_TYPE=smoke load-tests/scenarios/patient-service.js

# Care gap service — full load
k6 run load-tests/scenarios/care-gap-service.js

# Full pipeline with a real JWT token
k6 run \
  -e AUTH_TOKEN="eyJhbGci..." \
  -e TENANT_ID="your-tenant-id" \
  load-tests/scenarios/full-pipeline.js
```

---

## Load Profile

Each scenario uses the same ramp pattern (defined in `config/options.js`):

| Phase      | Duration | VUs    | Purpose            |
|------------|----------|--------|--------------------|
| Warm-up    | 30s      | 0→10   | Gentle ramp, JVM warm |
| Baseline   | 60s      | 10     | Stable baseline    |
| Ramp-up    | 60s      | 10→100 | Scale to peak load |
| Peak       | 180s     | 100    | Sustained peak     |
| Cool-down  | 30s      | 100→0  | Graceful wind-down |
| **Total**  | ~6 min   |        |                    |

---

## SLO Thresholds

| Metric                  | Threshold   | Meaning                          |
|-------------------------|-------------|----------------------------------|
| `http_req_duration p95` | < 200ms     | 95% of requests under 200ms      |
| `http_req_failed`       | < 1%        | Less than 1 in 100 requests fail |
| `pipeline_total_duration p95` | < 1500ms | End-to-end pipeline under 1.5s |

A k6 run exits with a non-zero status code if any threshold is breached, making it CI-friendly.

---

## Environment Variables

| Variable                  | Default                                          | Description                      |
|---------------------------|--------------------------------------------------|----------------------------------|
| `AUTH_TOKEN`              | placeholder (causes 401s in auth environments)   | JWT Bearer token                 |
| `TENANT_ID`               | `test-tenant-perf`                               | Tenant ID header value           |
| `PATIENT_ID`              | `f47ac10b-58cc-4372-a567-0e02b2c3d479`           | Test patient UUID                |
| `BASE_URL_PATIENT`        | `http://localhost:8084`                          | patient-service base URL         |
| `BASE_URL_CARE_GAP`       | `http://localhost:8086`                          | care-gap-service base URL        |
| `BASE_URL_QUALITY_MEASURE`| `http://localhost:8087`                          | quality-measure-service base URL |
| `ENVIRONMENT`             | `local`                                          | Tag applied to all metrics       |
| `TEST_TYPE`               | `load`                                           | `smoke` or `load`                |

---

## Interpreting Results

k6 prints a summary table at the end of each run. Key metrics to review:

```
http_req_duration..............: avg=45ms  min=12ms  med=38ms  p(90)=78ms  p(95)=95ms  p(99)=180ms  max=340ms
http_req_failed................: 0.00%     0 out of 18420
```

- **p(95) < 200ms** — SLO met
- **http_req_failed: 0.00%** — no errors, SLO met
- If p(95) > 200ms, check service logs: `docker compose logs -f patient-service`

### JSON results

Each run writes a timestamped JSON file to `load-tests/results/`. These can be imported into Grafana using the [k6 Grafana dashboard](https://grafana.com/grafana/dashboards/2587-k6-load-testing-results/) or analyzed with `jq`:

```bash
# Find the slowest requests in the last patient-service run
jq 'select(.type=="Point" and .metric=="http_req_duration") | .data.value' \
  load-tests/results/patient-service_*.json | sort -n | tail -20
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| All requests return 401 | Placeholder token rejected | Pass `-e AUTH_TOKEN=<real-jwt>` |
| All requests return 503 | Service not running | `docker compose -f docker-compose.minimal-clinical.yml up -d` |
| p95 > 200ms consistently | Service under-provisioned | Increase memory limits or reduce `maxVUs` |
| `dial tcp: connection refused` | Wrong port or service down | Verify port with `docker compose ps` |
| k6 not found | Not installed | `brew install k6` or `sudo snap install k6` |

---

## Related SLO Documentation

- Observable SLO commitments: `docs/architecture/OBSERVABLE_SLO_CONTRACTS.md`
- Distributed tracing: `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`
- Jaeger UI (traces): http://localhost:16686
- Prometheus metrics: http://localhost:9090
- Grafana dashboards: http://localhost:3001
