# Performance Benchmarking Methodology

**Purpose:** Standardized methodology for measuring and comparing CQL/FHIR vs SQL performance.

---

## Overview

This document provides a comprehensive methodology for benchmarking the performance of our FHIR/CQL-based quality measure evaluation system against traditional SQL-based implementations.

---

## Benchmarking Principles

### 1. Fair Comparison

- **Same Data**: Use identical test data for both approaches
- **Same Environment**: Run on same hardware/software configuration
- **Same Indexes**: Ensure both approaches use optimized indexes
- **Same Load**: Test under equivalent system load

### 2. Statistical Validity

- **Sufficient Sample Size**: Minimum 100 iterations per test
- **Warmup Period**: Include warmup to account for JIT compilation and cache warming
- **Multiple Runs**: Run tests multiple times and average results
- **Percentiles**: Report P50, P95, P99, not just averages

### 3. Realistic Scenarios

- **Real Data Volumes**: Use production-like data sizes
- **Realistic Queries**: Test actual measure evaluation logic
- **Concurrent Load**: Test with multiple concurrent users
- **Mixed Workloads**: Test various measure types

---

## Test Scenarios

### Scenario 1: Single Patient, Single Measure

**Purpose:** Baseline performance comparison

**Test:**
- 1 patient
- 1 measure (HEDIS-CDC)
- 100 iterations
- Sequential execution

**Metrics:**
- Average latency
- P50, P95, P99 percentiles
- Success rate

**Expected Results:**
- CQL/FHIR: 50-150ms (cached), 150-300ms (uncached)
- SQL: 200-400ms

### Scenario 2: Single Patient, Multiple Measures

**Purpose:** Test parallel processing advantage

**Test:**
- 1 patient
- 5, 10, 52 measures
- 10 iterations
- Parallel execution (CQL) vs Sequential (SQL)

**Metrics:**
- Total evaluation time
- Average time per measure
- Throughput (measures/second)

**Expected Results:**
- CQL/FHIR: 1.8-2.5s for 52 measures (parallel)
- SQL: 8-12s for 52 measures (sequential)

### Scenario 3: Batch Evaluation

**Purpose:** Test scalability with multiple patients

**Test:**
- 10, 100, 1000 patients
- 1 measure (HEDIS-CDC)
- 1 iteration per batch size
- Parallel processing

**Metrics:**
- Total batch time
- Average time per patient
- Throughput (patients/second)

**Expected Results:**
- CQL/FHIR: 2-5s for 100 patients
- SQL: 8-15s for 100 patients

### Scenario 4: Concurrent Load

**Purpose:** Test system under concurrent requests

**Test:**
- 10, 50, 100, 500 concurrent users
- 1 patient, 1 measure per request
- 1000 total requests
- Load distributed over time

**Metrics:**
- Average latency under load
- P95 latency under load
- Throughput (requests/second)
- Error rate

**Expected Results:**
- CQL/FHIR: Maintains <200ms P95 up to 100 concurrent users
- SQL: Degrades to >500ms P95 at 50 concurrent users

---

## Measurement Methodology

### 1. Timing Methodology

**Start Time:**
- For CQL: When API request is sent
- For SQL: When SQL query execution begins

**End Time:**
- For CQL: When API response is received
- For SQL: When SQL query completes

**Excluded:**
- Network latency (measured separately)
- Serialization overhead (minimal, but noted)

### 2. Cache State

**Cold Cache:**
- Clear cache before test
- First request populates cache
- Measures initial performance

**Warm Cache:**
- Cache pre-populated
- Measures steady-state performance
- More representative of production

**Report Both:**
- Always report both cold and warm cache results
- Note cache hit rate during warm cache tests

### 3. Resource Monitoring

**During Tests:**
- Monitor CPU usage (per core)
- Monitor memory usage (heap and non-heap)
- Monitor database CPU and I/O
- Monitor network I/O
- Monitor cache hit rates

**Tools:**
- `docker stats` for container metrics
- `jstat` for JVM metrics
- `pg_stat_statements` for database metrics
- Prometheus/Grafana for application metrics

---

## SQL Equivalent Queries

### Requirements

1. **Functional Equivalence**: SQL must produce same results as CQL
2. **Optimized**: Use proper indexes and query plans
3. **Realistic**: Reflect how SQL would be written in practice
4. **Documented**: Include comments explaining logic

### Example: HEDIS-CDC

**CQL Logic:**
- Initial Population: Age 18-75
- Denominator: Has diabetes diagnosis
- Numerator: HbA1c <= 7.0% in measurement period

**SQL Equivalent:**
- See `scripts/sql-equivalents/hedis-cdc-sql.sql`

**Key Differences:**
- SQL uses direct table joins
- CQL uses FHIR resource queries
- SQL requires manual code mapping
- CQL leverages value sets

---

## Data Requirements

### Test Data Set

**Patients:**
- Minimum: 100 patients
- Recommended: 1000 patients
- Distribution: Mix of ages, conditions, data volumes

**Historical Data:**
- Observations: 3 years per patient (~10,000 per patient)
- Conditions: ~100 per patient
- Encounters: ~500 per patient
- Medications: ~200 per patient

**Data Quality:**
- Realistic code values (SNOMED, LOINC, ICD-10)
- Proper date ranges
- Valid relationships between resources

### Data Generation

**Options:**
1. Use production data (anonymized)
2. Use synthetic data generator
3. Use demo seeding service

**Recommendation:** Use demo seeding service for consistency

---

## Execution Process

### 1. Preparation

```bash
# Ensure services are running
docker compose ps

# Verify test data exists
curl http://localhost:18080/api/v1/patients?page=0&size=10

# Clear cache (for cold cache tests)
redis-cli FLUSHALL
```

### 2. Warmup

```bash
# Run warmup iterations
./scripts/benchmark-cql-vs-sql.sh --warmup-only
```

### 3. Execution

```bash
# Run benchmark
./scripts/benchmark-cql-vs-sql.sh \
  --iterations 500 \
  --concurrent-users 50
```

### 4. Analysis

```bash
# Generate report
./scripts/generate-benchmark-report.sh \
  benchmark-results/benchmark_*.json \
  benchmark-report.md
```

---

## Reporting Standards

### Required Metrics

1. **Latency Statistics**
   - Average
   - P50 (median)
   - P95
   - P99
   - Min/Max

2. **Throughput**
   - Requests per second
   - Patients per second
   - Measures per second

3. **Success Metrics**
   - Success rate
   - Error rate
   - Error types

4. **Resource Usage**
   - CPU usage
   - Memory usage
   - Database load
   - Cache hit rate

### Report Format

1. **Executive Summary**
   - Key findings
   - Speedup metrics
   - Recommendations

2. **Detailed Results**
   - Per-scenario breakdown
   - Statistical analysis
   - Charts and graphs

3. **Methodology**
   - Test configuration
   - Data characteristics
   - Assumptions

4. **Raw Data**
   - JSON results
   - Log files
   - Query plans

---

## Validation

### Result Validation

1. **Functional Correctness**
   - Verify both approaches produce same results
   - Check for edge cases
   - Validate against known test cases

2. **Statistical Validity**
   - Check for outliers
   - Verify sample size is sufficient
   - Confirm normal distribution (or note if not)

3. **Reproducibility**
   - Document exact configuration
   - Note any environmental factors
   - Provide scripts for reproduction

---

## Best Practices

### Do's

✅ Run warmup before measurement  
✅ Use sufficient iterations (100+)  
✅ Report percentiles, not just averages  
✅ Test both cold and warm cache  
✅ Monitor resource usage  
✅ Document assumptions  
✅ Use realistic data volumes  
✅ Test multiple scenarios  

### Don'ts

❌ Don't test with empty cache only  
❌ Don't use too few iterations  
❌ Don't ignore outliers  
❌ Don't test on different hardware  
❌ Don't skip warmup  
❌ Don't test with unrealistic data  
❌ Don't compare different data sets  

---

## Troubleshooting

### Common Issues

1. **Inconsistent Results**
   - Check for background processes
   - Verify cache state
   - Check system load

2. **SQL Queries Slow**
   - Verify indexes exist
   - Check query plans
   - Review connection pool settings

3. **CQL Evaluations Failing**
   - Check FHIR service health
   - Verify patient/measure IDs
   - Review error logs

4. **Performance Degradation**
   - Check database load
   - Monitor cache hit rates
   - Review connection pool usage

---

## Next Steps

1. **Run Initial Benchmarks**: Execute with default configuration
2. **Analyze Results**: Review performance comparisons
3. **Optimize**: Address any bottlenecks found
4. **Re-run**: Validate improvements
5. **Document**: Update this methodology based on learnings

---

**Last Updated:** $(date '+%Y-%m-%d')  
**Version:** 1.0
