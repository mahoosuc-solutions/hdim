# Data Flow Tracking Implementation

## Overview

The Data Flow Tracking system enables CQL Engine Service to capture and record step-by-step data flow during CQL evaluations, answering the critical question: **"How is data flowing and why?"**

This capability is essential for:
- **Healthcare Compliance** - HIPAA audit requirements
- **Clinical Decision Support** - Understanding why specific health decisions were made
- **Debugging** - Troubleshooting CQL evaluation issues
- **User Trust** - Showing patients and providers how their data influences health decisions

## Architecture

### Components

1. **DataFlowTracker** (`/audit/DataFlowTracker.java`)
   - ThreadLocal-based step tracking
   - Automatic context management
   - Configurable max steps limit
   - Zero performance impact

2. **CqlEvaluationAuditAspect** (updated)
   - Automatically starts/stops tracking
   - Captures data flow steps in audit events
   - Handles both success and failure cases

3. **DataFlowStep** (part of `CqlEvaluationAuditEvent`)
   - Immutable step representation
   - Rich metadata (resources, decisions, reasoning)
   - Duration tracking

### How It Works

```
CQL Evaluation Starts
       ↓
AuditAspect starts DataFlowTracker
       ↓
Business Logic calls dataFlowTracker.recordStep()
       ↓
Steps accumulated in ThreadLocal
       ↓
Evaluation completes/fails
       ↓
AuditAspect retrieves steps
       ↓
Steps included in audit event → Kafka
       ↓
DataFlowTracker.clearTracking()
```

## Usage

### Basic Usage in Service Code

```java
@Service
public class CqlEvaluationService {

    private final DataFlowTracker dataFlowTracker;

    public CqlEvaluation executeEvaluation(UUID evaluationId, String tenantId) {
        // DataFlowTracker is already started by AuditAspect

        // Step 1: Fetch patient data
        dataFlowTracker.recordStep(
            "Fetch Patient Demographics",
            "DATA_FETCH",
            List.of("Patient"),
            null,
            patientData,
            "Patient age: 45",
            "Retrieved patient birth date for age calculation"
        );

        // Step 2: Evaluate CQL
        dataFlowTracker.recordStep(
            "Evaluate Diabetes Criteria",
            "LOGIC_DECISION",
            "Patient IN Numerator",
            "HbA1c < 7% AND last measurement within 12 months"
        );

        // Steps are automatically captured by AuditAspect
        return evaluation;
    }
}
```

### Step Types

- **DATA_FETCH** - Fetching data from FHIR server or database
- **EXPRESSION_EVAL** - Evaluating a CQL expression
- **LOGIC_DECISION** - Making a logic decision (IF/THEN, numerator/denominator)
- **CQL_EXECUTION** - Executing CQL library code
- **DATA_TRANSFORM** - Transforming data between formats
- **CACHE_LOOKUP** - Looking up cached data

### Recording Steps with Duration

```java
// Start step
dataFlowTracker.recordStep("Evaluate CQL Expression", "EXPRESSION_EVAL",
    null, "Expression: AgeInYears()");

// ... perform operation ...

// End step and record duration
dataFlowTracker.endCurrentStep();
```

## Configuration

```yaml
# application.yml
audit:
  data-flow-tracking:
    enabled: ${AUDIT_DATA_FLOW_ENABLED:true}
    max-steps: 50  # Maximum steps per evaluation
```

### Environment Variables

- `AUDIT_DATA_FLOW_ENABLED` - Enable/disable data flow tracking (default: true)
- `AUDIT_ENABLED` - Master switch for all auditing (default: true)

## Example Data Flow Output

```json
{
  "evaluationId": "550e8400-e29b-41d4-a716-446655440000",
  "dataFlowSteps": [
    {
      "stepNumber": 1,
      "stepName": "Fetch Patient Demographics",
      "stepType": "DATA_FETCH",
      "timestamp": "2025-01-15T10:30:00Z",
      "resourcesAccessed": ["Patient"],
      "decision": "Patient age: 45",
      "reasoning": "Retrieved patient birth date for age calculation",
      "durationMs": 23
    },
    {
      "stepNumber": 2,
      "stepName": "Fetch HbA1c Observations",
      "stepType": "DATA_FETCH",
      "timestamp": "2025-01-15T10:30:00.050Z",
      "resourcesAccessed": ["Observation"],
      "decision": "Found 3 HbA1c measurements",
      "reasoning": "Querying Observation resources with code 4548-4 (LOINC)",
      "durationMs": 45
    },
    {
      "stepNumber": 3,
      "stepName": "Evaluate Diabetes Control Criteria",
      "stepType": "LOGIC_DECISION",
      "timestamp": "2025-01-15T10:30:00.120Z",
      "decision": "Patient IN Numerator",
      "reasoning": "Latest HbA1c 6.5% < 7% (target) AND measured within 12 months",
      "durationMs": 12
    }
  ]
}
```

## Best Practices

### 1. Record All Significant Steps

```java
// ✅ Good - Records important steps
dataFlowTracker.recordStep("Fetch Patient Data", "DATA_FETCH", ...);
dataFlowTracker.recordStep("Evaluate Inclusion Criteria", "LOGIC_DECISION", ...);

// ❌ Bad - Missing critical steps
// ... performs evaluation without recording steps
```

### 2. Provide Clear Decisions and Reasoning

```java
// ✅ Good - Clear decision and reasoning
dataFlowTracker.recordStep(
    "Check Age Eligibility",
    "LOGIC_DECISION",
    "Patient EXCLUDED",
    "Patient age 17 < 18 (minimum age for measure)"
);

// ❌ Bad - Vague decision
dataFlowTracker.recordStep(
    "Check Age",
    "LOGIC_DECISION",
    "No",
    "Age check failed"
);
```

### 3. Record Resources Accessed

```java
// ✅ Good - Specifies which resources
dataFlowTracker.recordStep(
    "Fetch Medications",
    "DATA_FETCH",
    List.of("MedicationRequest", "MedicationStatement"),
    ...
);

// ❌ Bad - Missing resource information
dataFlowTracker.recordStep("Fetch Data", "DATA_FETCH", null, ...);
```

### 4. Don't Exceed Max Steps

```java
// ✅ Good - Summarizes loops
dataFlowTracker.recordStep(
    "Evaluate Medications",
    "LOGIC_DECISION",
    "Evaluated 15 medications, found 3 matches",
    "Checking for statins in medication list"
);

// ❌ Bad - Records step for each iteration
for (Medication med : medications) {
    dataFlowTracker.recordStep(...); // Could exceed max-steps
}
```

## Thread Safety

DataFlowTracker uses ThreadLocal storage, making it:
- **Thread-safe** - Each thread has its own tracking context
- **Async-safe** - Works with async execution (as long as evaluation completes in same thread)
- **Clean** - Automatically cleared after evaluation

**Important**: If you use async/parallel execution within an evaluation, the data flow steps will only be captured on the thread that has the tracking context. Consider using MDC propagation for distributed tracing.

## Performance Considerations

### Zero Impact Design

- **ThreadLocal storage** - No synchronization overhead
- **Conditional execution** - Disabled via `enabled` flag skips all processing
- **Truncation** - Long strings automatically truncated to prevent memory issues
- **Max steps limit** - Prevents unbounded memory growth

### Memory Usage

- Each DataFlowStep: ~500-1000 bytes
- Max 50 steps (configurable): ~25-50 KB per evaluation
- Cleared immediately after evaluation completes

## Troubleshooting

### Steps Not Appearing in Audit Events

1. Check if tracking is enabled:
   ```yaml
   audit:
     data-flow-tracking:
       enabled: true
   ```

2. Verify audit aspect is working:
   ```
   DEBUG CqlEvaluationAuditAspect - Auditing CQL evaluation: evaluationId=...
   DEBUG DataFlowTracker - Started data flow tracking for evaluation: ...
   ```

3. Check if steps are being recorded:
   ```
   DEBUG DataFlowTracker - Recorded step 1: Fetch Patient Demographics
   ```

### Max Steps Exceeded

If you see: `WARN DataFlowTracker - Max steps (50) reached for evaluation: ...`

**Solutions**:
- Increase `audit.data-flow-tracking.max-steps` in configuration
- Reduce granularity of step recording
- Summarize repetitive operations

### Steps Lost After Async Call

DataFlowTracker uses ThreadLocal. If you switch threads:

```java
// ❌ Bad - Steps lost after async
CompletableFuture.supplyAsync(() -> {
    dataFlowTracker.recordStep(...); // Won't work - different thread
});

// ✅ Good - Stay on same thread
dataFlowTracker.recordStep(...); // Works - same thread
```

## Future Enhancements

1. **Distributed Tracing Integration** - Integrate with OpenTelemetry/Zipkin
2. **Visual Data Flow UI** - Frontend visualization of data flow steps
3. **Step Templates** - Predefined step templates for common operations
4. **Performance Profiling** - Automatic slow step detection
5. **Data Lineage** - Track data transformations across multiple evaluations

## See Also

- [ASYNC_AUDIT_ARCHITECTURE.md](ASYNC_AUDIT_ARCHITECTURE.md) - Overall audit architecture
- [IMPLEMENTATION_SUMMARY.md](/IMPLEMENTATION_SUMMARY.md) - Implementation details
- `/audit/DataFlowTracker.java` - Source code
- `/aspect/CqlEvaluationAuditAspect.java` - Integration point
