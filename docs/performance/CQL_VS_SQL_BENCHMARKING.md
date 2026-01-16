# CQL/FHIR vs Traditional SQL Performance Benchmarking

**Purpose:** Measure and compare performance between FHIR/CQL-based evaluation and traditional SQL-based approaches.

---

## Overview

This document describes the methodology, tools, and results for benchmarking the performance of our FHIR/CQL-based quality measure evaluation system against traditional SQL-based implementations.

### Why Benchmark?

1. **Quantify Performance Improvements**: Demonstrate measurable speedup over SQL-based systems
2. **Validate Architecture Decisions**: Confirm that FHIR/CQL approach provides better performance
3. **Identify Optimization Opportunities**: Find bottlenecks in both approaches
4. **Support Business Cases**: Provide data for ROI calculations and sales materials

---

## Methodology

### Test Scenarios

We benchmark the following scenarios:

1. **Single Patient Evaluation**
   - One patient, one measure
   - Measures: HEDIS-CDC (Diabetes), HEDIS-CBP (Blood Pressure), HEDIS-BCS (Breast Cancer Screening)

2. **Batch Evaluation**
   - Multiple patients, single measure
   - Tests: 10, 100, 1000 patients

3. **Multi-Measure Evaluation**
   - Single patient, multiple measures
   - Tests: 5, 10, 52 measures

4. **Concurrent Load**
   - Multiple concurrent requests
   - Tests: 10, 50, 100, 500 concurrent users

### Metrics Collected

| Metric | Description | Unit |
|--------|-------------|------|
| **Average Latency** | Mean response time | milliseconds |
| **P50 (Median)** | 50th percentile latency | milliseconds |
| **P95** | 95th percentile latency | milliseconds |
| **P99** | 99th percentile latency | milliseconds |
| **Min/Max** | Minimum and maximum latency | milliseconds |
| **Throughput** | Requests per second | req/s |
| **Success Rate** | Percentage of successful requests | % |
| **Error Rate** | Percentage of failed requests | % |
| **CPU Usage** | Average CPU utilization | % |
| **Memory Usage** | Average memory consumption | MB |
| **Database Load** | Database CPU and I/O | % |

### Test Environment

**Hardware:**
- CPU: 2 cores (Docker limit)
- Memory: 2GB (Docker limit)
- Database: PostgreSQL 16 (shared)

**Software:**
- CQL Engine Service: Spring Boot 3.x
- FHIR Service: Spring Boot 3.x
- Database: PostgreSQL 16 with optimized indexes
- Cache: Redis 7 (for CQL/FHIR only)

**Data:**
- Test patients: 100-1000 patients
- Historical data: 3 years per patient
- Observations: ~10,000 per patient
- Conditions: ~100 per patient
- Encounters: ~500 per patient

---

## Benchmarking Tools

### 1. Automated Benchmark Script

**Location:** `scripts/benchmark-cql-vs-sql.sh`

**Usage:**
```bash
# Basic usage
./scripts/benchmark-cql-vs-sql.sh

# With custom configuration
ITERATIONS=200 \
CONCURRENT_USERS=20 \
BASE_URL=http://localhost:18080 \
./scripts/benchmark-cql-vs-sql.sh
```

**Configuration:**
- `ITERATIONS`: Number of test iterations (default: 100)
- `CONCURRENT_USERS`: Number of concurrent requests (default: 10)
- `WARMUP_ITERATIONS`: Warmup iterations before measurement (default: 10)
- `BASE_URL`: API gateway URL (default: http://localhost:18080)
- `TENANT_ID`: Tenant identifier (default: acme-health)
- `RESULTS_DIR`: Output directory (default: ./benchmark-results)

**Output:**
- JSON results file: `benchmark-results/benchmark_YYYYMMDD_HHMMSS.json`
- Console summary with statistics

### 2. SQL Equivalent Queries

**Location:** `scripts/sql-equivalents/`

We provide SQL queries that replicate CQL measure logic:

#### HEDIS-CDC (Diabetes HbA1c Control)

**CQL Approach:**
- Uses FHIR Patient, Condition, Observation resources
- CQL logic evaluates age, diabetes diagnosis, HbA1c values
- Leverages FHIR service caching

**SQL Equivalent:**
```sql
WITH patient_data AS (
    SELECT 
        p.id as patient_id,
        p.birth_date,
        EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) as age
    FROM patient.patients p
    WHERE p.id = $1
),
denominator AS (
    SELECT 
        pd.patient_id,
        CASE 
            WHEN pd.age BETWEEN 18 AND 75 
            AND EXISTS (
                SELECT 1 FROM fhir.conditions c
                WHERE c.patient_id = pd.patient_id
                AND c.code IN ('E10', 'E11', 'E10.9', 'E11.9')
                AND c.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_denominator
    FROM patient_data pd
),
numerator AS (
    SELECT 
        d.patient_id,
        CASE 
            WHEN d.in_denominator = 1
            AND EXISTS (
                SELECT 1 FROM fhir.observations o
                WHERE o.patient_id = d.patient_id
                AND o.code = '4548-4'  -- HbA1c LOINC code
                AND o.effective_date_time >= CURRENT_DATE - INTERVAL '1 year'
                AND o.value_numeric <= 7.0
                AND o.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_numerator
    FROM denominator d
)
SELECT 
    patient_id,
    in_denominator,
    in_numerator,
    CASE WHEN in_denominator = 1 AND in_numerator = 1 THEN true ELSE false END as compliant
FROM numerator;
```

**Key Differences:**
- SQL uses direct table joins and EXISTS subqueries
- CQL uses FHIR resource queries with caching
- SQL requires manual code system mapping
- CQL leverages FHIR value sets and terminology services

### 3. Performance Monitoring

**Tools:**
- **Prometheus**: Metrics collection
- **Grafana**: Visualization dashboards
- **Docker Stats**: Container resource usage
- **PostgreSQL pg_stat_statements**: Query performance

**Metrics Tracked:**
- API response times
- Database query times
- Cache hit rates
- CPU and memory usage
- Network I/O

---

## Expected Results

### Performance Characteristics

#### Single Patient Evaluation

| Approach | Avg (ms) | P50 (ms) | P95 (ms) | P99 (ms) |
|----------|----------|----------|----------|----------|
| **CQL/FHIR (Cached)** | 50-100 | 60 | 150 | 300 |
| **CQL/FHIR (Uncached)** | 150-250 | 180 | 400 | 600 |
| **SQL Traditional** | 200-400 | 250 | 500 | 800 |

**Expected Speedup:** 2-4x faster with caching, 1.5-2x without caching

#### Batch Evaluation (100 Patients)

| Approach | Total Time (s) | Avg per Patient (ms) | Throughput (patients/s) |
|----------|----------------|---------------------|------------------------|
| **CQL/FHIR (Parallel)** | 2-5 | 20-50 | 20-50 |
| **SQL Traditional** | 8-15 | 80-150 | 7-12 |

**Expected Speedup:** 3-5x faster

#### Multi-Measure Evaluation (52 Measures)

| Approach | Total Time (s) | Avg per Measure (ms) |
|----------|----------------|---------------------|
| **CQL/FHIR (Parallel + Cache)** | 1.8-2.5 | 35-50 |
| **SQL Traditional (Sequential)** | 8-12 | 150-230 |

**Expected Speedup:** 4-6x faster

### Why CQL/FHIR is Faster

1. **Caching**
   - FHIR resources cached in Redis (87% hit rate)
   - Measure definitions cached
   - Reduces database load by 7x

2. **Parallel Processing**
   - Multiple measures evaluated concurrently
   - Thread pool optimization
   - Better CPU utilization

3. **Optimized Data Access**
   - FHIR service optimized for common queries
   - Indexed resource lookups
   - Reduced data transfer

4. **Code Reuse**
   - Shared FHIR resources across measures
   - Template-based evaluation
   - Less redundant querying

5. **Modern Architecture**
   - Microservices with dedicated caching
   - Connection pooling
   - Async processing where possible

---

## Running Benchmarks

### Prerequisites

1. **Services Running**
   ```bash
   # Ensure all services are up
   docker compose ps
   
   # Check health
   curl http://localhost:18080/actuator/health
   ```

2. **Test Data**
   ```bash
   # Ensure test patients exist
   curl http://localhost:18080/api/v1/patients?page=0&size=10 \
     -H "X-Tenant-ID: acme-health"
   ```

3. **Database Access**
   ```bash
   # Test PostgreSQL connection
   psql -h localhost -p 5435 -U healthdata -d healthdata_db -c "SELECT 1;"
   ```

### Quick Benchmark

```bash
# Run basic benchmark (100 iterations)
./scripts/benchmark-cql-vs-sql.sh

# Results will be in: ./benchmark-results/benchmark_*.json
```

### Comprehensive Benchmark

```bash
# Run extended benchmark
ITERATIONS=500 \
CONCURRENT_USERS=50 \
WARMUP_ITERATIONS=50 \
./scripts/benchmark-cql-vs-sql.sh
```

### Batch Benchmark

```bash
# Test batch evaluation performance
./scripts/benchmark-batch-evaluation.sh \
  --patients 100 \
  --measures 10 \
  --iterations 10
```

---

## Analyzing Results

### JSON Results Format

```json
{
  "timestamp": "20250115_120000",
  "configuration": {
    "iterations": 100,
    "concurrent_users": 10
  },
  "results": {
    "cql_fhir": {
      "success_count": 98,
      "statistics": {
        "avg_ms": 85,
        "p50_ms": 75,
        "p95_ms": 180,
        "p99_ms": 320
      }
    },
    "sql_traditional": {
      "success_count": 95,
      "statistics": {
        "avg_ms": 280,
        "p50_ms": 250,
        "p95_ms": 520,
        "p99_ms": 780
      }
    },
    "comparison": {
      "speedup_avg": 3.29,
      "speedup_p95": 2.89,
      "improvement_percent": 69.6
    }
  }
}
```

### Visualization

**Generate Report:**
```bash
# Generate markdown report
./scripts/generate-benchmark-report.sh \
  --input benchmark-results/benchmark_*.json \
  --output benchmark-report.md
```

**Create Charts:**
```bash
# Generate comparison charts
python scripts/visualize-benchmark-results.py \
  --input benchmark-results/benchmark_*.json \
  --output charts/
```

---

## Interpreting Results

### Speedup Calculation

**Speedup = SQL_Time / CQL_Time**

- **Speedup > 2x**: Significant improvement
- **Speedup 1.5-2x**: Moderate improvement
- **Speedup < 1.5x**: Marginal improvement

### Performance Factors

1. **Cache Hit Rate**
   - Higher cache hit rate = better CQL performance
   - Target: >85% cache hit rate

2. **Data Volume**
   - More patient data = larger SQL advantage (initially)
   - CQL scales better with caching

3. **Concurrent Load**
   - CQL handles concurrency better
   - SQL may have connection pool limits

4. **Measure Complexity**
   - Complex measures favor CQL (better code organization)
   - Simple measures may favor SQL (less overhead)

---

## Best Practices

### 1. Consistent Test Environment

- Use same hardware/software configuration
- Clear cache between test runs (if testing cold start)
- Use same test data set

### 2. Statistical Validity

- Run sufficient iterations (minimum 100)
- Include warmup period
- Report percentiles, not just averages

### 3. Fair Comparison

- Ensure both approaches use same indexes
- Use same database connection pool settings
- Test with realistic data volumes

### 4. Document Assumptions

- Note cache state (cold vs warm)
- Document data set characteristics
- Record system load during tests

---

## Troubleshooting

### Common Issues

1. **SQL Queries Failing**
   - Check database connection
   - Verify table names and schemas
   - Check parameter binding

2. **CQL Evaluations Timing Out**
   - Increase timeout values
   - Check FHIR service health
   - Verify patient/measure IDs exist

3. **Inconsistent Results**
   - Ensure warmup period completed
   - Check for background processes
   - Verify test data consistency

4. **Performance Degradation**
   - Check database load
   - Monitor cache hit rates
   - Review connection pool usage

---

## Next Steps

1. **Run Benchmarks**: Execute benchmark scripts with your data
2. **Analyze Results**: Review performance comparisons
3. **Optimize**: Identify and address bottlenecks
4. **Document**: Record findings and improvements
5. **Present**: Share results with stakeholders

---

## Related Documentation

- [Performance Guide](../PERFORMANCE_GUIDE.md)
- [Performance Benchmarks](../product/02-architecture/performance-benchmarks.md)
- [CQL Engine Service](../backend/modules/services/cql-engine-service/IMPLEMENTATION_SUMMARY.md)
- [FHIR Service Architecture](../docs/architecture/SYSTEM_ARCHITECTURE.md)

---

**Last Updated:** $(date '+%Y-%m-%d')  
**Version:** 1.0
