# Decision Replay Service & QA Per-Agent Statistics - Implementation Summary

**Date**: January 13, 2026  
**Status**: ✅ Complete

---

## Overview

This document summarizes the implementation of two high-priority features:

1. **Decision Replay Service - AI Agent Integration**: Full integration with agent runtime service to re-execute AI decisions
2. **QA Per-Agent Statistics**: Comprehensive per-agent type metrics and trends for QA review system

---

## 1. Decision Replay Service - AI Agent Integration

### Files Created/Modified

#### New Files
- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/client/AgentRuntimeClient.java`
  - HTTP client for communicating with agent runtime service
  - Handles agent execution requests and response parsing
  - Optional component (only active if RestTemplate bean is available)

- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/config/AuditClientConfig.java`
  - Configuration class providing RestTemplate and ObjectMapper beans
  - Optional beans (only created if not already provided by service)

#### Modified Files
- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/ai/DecisionReplayService.java`
  - Enhanced `executeReplay()` method with full agent service integration
  - Added methods:
    - `canReplayViaAgentService()` - Checks if agent replay is possible
    - `replayViaAgentService()` - Executes actual agent service call
    - `determineAgentSlug()` - Maps AgentType enum to agent runtime slugs
    - `reconstructAgentRequest()` - Builds request from stored event data
    - `buildMessageFromInputMetrics()` - Constructs message from input metrics
    - `extractRecommendedValue()` - Parses recommended value from agent response
    - `extractConfidenceScore()` - Extracts confidence score from response

### Key Features

#### Agent Service Integration
- **Automatic Agent Replay**: When original decision data is available, the service automatically calls the agent runtime service to get a fresh decision
- **Request Reconstruction**: Rebuilds the original agent request from stored event data:
  - User query (primary)
  - Input metrics (fallback)
  - Metadata and context
  - Patient ID (if applicable)

#### Fallback Behavior
- **Validation Replay**: If agent service is unavailable or request cannot be reconstructed, falls back to validation replay mode
- **Graceful Degradation**: Service continues to function even if agent runtime client is not configured

#### Drift Detection
- **Comparison Logic**: Compares original vs replayed decisions:
  - Recommendation match (exact)
  - Confidence score similarity (5% tolerance for non-determinism)
  - Identifies differences and logs them

#### Agent Type Mapping
- Maps `AgentType` enum values to agent runtime service slugs:
  - `CLINICAL_DECISION_AGENT` → `"clinical-decision"`
  - `CARE_GAP_OPTIMIZER` → `"care-gap-optimizer"`
  - `DOCUMENTATION_ASSISTANT` → `"documentation-assistant"`
  - `REPORT_GENERATOR` → `"report-generator"`
  - Custom agents: Requires agent ID lookup (falls back to validation)

### Configuration

#### Required (Optional)
- `hdim.agent-runtime.url` - Agent runtime service URL (default: `http://agent-runtime-service:8080`)
- `RestTemplate` bean - Provided by `AuditClientConfig` or service configuration
- `ObjectMapper` bean - Provided by `AuditClientConfig` or service configuration

#### Usage Example
```java
// Replay a single decision
ReplayResult result = decisionReplayService.replayDecision(eventId);

// Replay multiple decisions
List<ReplayResult> results = decisionReplayService.replayDecisionBatch(eventIds);

// Replay decision chain
ChainReplayResult chainResult = decisionReplayService.replayDecisionChain(correlationId);
```

---

## 2. QA Per-Agent Statistics

### Files Modified

- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/qa/QAReviewService.java`
  - Enhanced `getMetrics()` method with per-agent statistics
  - Enhanced `getAccuracyTrends()` method with per-agent trends
  - Added methods:
    - `calculatePerAgentStatistics()` - Groups events by agent type and calculates metrics
    - `calculatePerAgentTrends()` - Groups events by agent type and calculates daily trends

### Key Features

#### Per-Agent Metrics
For each agent type, calculates:
- **Total Decisions**: Number of decisions made by this agent
- **Approved/Rejected Counts**: Review outcomes
- **Approval Rate**: Percentage of approved decisions
- **Average Confidence**: Mean confidence score across all decisions
- **Accuracy**: 1 - (false positive rate + false negative rate)

#### Per-Agent Trends
For each agent type, provides daily trend data:
- **Daily Decision Counts**: Total, approved, rejected
- **Daily Accuracy**: Calculated from false positives/negatives
- **Daily Average Confidence**: Mean confidence per day

#### Filtering Support
- Respects `agentType` filter parameter
- If filter is provided, only calculates stats for that agent type
- If null, calculates stats for all agent types

### Data Structure

#### AgentStats
```java
{
  "totalDecisions": 150,
  "approved": 120,
  "rejected": 20,
  "approvalRate": 0.80,
  "averageConfidence": 0.85,
  "accuracy": 0.93
}
```

#### AgentPerformance
```java
{
  "byAgentType": {
    "CLINICAL_DECISION_AGENT": { ... },
    "CARE_GAP_OPTIMIZER": { ... },
    "DOCUMENTATION_ASSISTANT": { ... }
  }
}
```

#### Per-Agent Trends
```java
{
  "byAgentType": {
    "CLINICAL_DECISION_AGENT": [
      {
        "date": "2026-01-10",
        "totalDecisions": 15,
        "approved": 12,
        "rejected": 2,
        "accuracy": 0.93,
        "averageConfidence": 0.87
      },
      ...
    ]
  }
}
```

### Usage Example
```java
// Get metrics with per-agent breakdown
QAMetrics metrics = qaReviewService.getMetrics(tenantId, null, startDate, endDate);
Map<String, AgentStats> agentStats = metrics.getAgentPerformance().getByAgentType();

// Get trends with per-agent breakdown
QATrendData trends = qaReviewService.getAccuracyTrends(tenantId, null, 30);
Map<String, List<DailyTrendPoint>> agentTrends = trends.getByAgentType();
```

---

## Testing Recommendations

### Decision Replay Service
1. **Unit Tests**:
   - Test request reconstruction from various event data structures
   - Test agent slug mapping for all agent types
   - Test fallback to validation replay when agent service unavailable

2. **Integration Tests**:
   - Test actual agent service calls (with mock agent runtime service)
   - Test drift detection with known decision differences
   - Test batch and chain replay operations

### QA Per-Agent Statistics
1. **Unit Tests**:
   - Test statistics calculation with various event/review combinations
   - Test filtering by agent type
   - Test trend calculation with date grouping

2. **Integration Tests**:
   - Test with real database data
   - Verify accuracy calculations
   - Test with multiple agent types

---

## Dependencies

### Decision Replay Service
- `spring-boot-starter-web` (already included in audit module)
- `jackson-databind` (already included)
- Agent Runtime Service must be accessible

### QA Per-Agent Statistics
- No additional dependencies
- Uses existing repository and entity classes

---

## Configuration

### Application Properties
```yaml
# Optional: Agent runtime service URL
hdim:
  agent-runtime:
    url: http://agent-runtime-service:8080
```

### Bean Configuration
If using the audit module in a service, you can either:
1. **Use provided beans**: `AuditClientConfig` provides RestTemplate and ObjectMapper
2. **Provide your own**: Service can provide its own RestTemplate bean (will be used instead)

---

## Performance Considerations

### Decision Replay Service
- **Agent Service Calls**: Each replay makes an HTTP call to agent runtime service
- **Timeout Configuration**: RestTemplate configured with 10s connect, 30s read timeouts
- **Fallback Performance**: Validation replay is fast (no network calls)

### QA Per-Agent Statistics
- **In-Memory Processing**: All statistics calculated in memory after fetching data
- **Efficient Grouping**: Uses Java Streams for efficient grouping and aggregation
- **Scalability**: For very large datasets, consider adding database-level aggregation

---

## Future Enhancements

### Decision Replay Service
1. **Caching**: Cache agent responses for identical requests
2. **Async Replay**: Support async replay for batch operations
3. **Custom Agent Support**: Better support for custom agent types
4. **Response Parsing**: More sophisticated parsing of structured agent responses

### QA Per-Agent Statistics
1. **Database Aggregation**: Move some calculations to database level for better performance
2. **Caching**: Cache calculated statistics with TTL
3. **Real-time Updates**: WebSocket support for real-time metric updates
4. **Advanced Metrics**: Add more sophisticated metrics (precision, recall, F1-score)

---

## Status

✅ **Complete**: Both features fully implemented and ready for testing

- Decision Replay Service: ✅ Complete with agent integration
- QA Per-Agent Statistics: ✅ Complete with trends support
- Configuration: ✅ Optional beans provided
- Documentation: ✅ This document

---

## Notes

- Both features are **backward compatible** - existing functionality continues to work
- Agent runtime client is **optional** - services without it will use validation replay
- Per-agent statistics are **always calculated** - no configuration needed
- All code follows existing patterns and conventions in the audit module
