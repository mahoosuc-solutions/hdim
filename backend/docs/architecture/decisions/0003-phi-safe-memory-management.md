# ADR-0003: PHI-Safe Memory Management for AI Agents

## Status

Accepted

## Date

2024-12-06

## Context

AI agents in healthcare require conversation memory to maintain context across multi-turn interactions. However, this creates significant HIPAA compliance challenges:

1. **PHI in Context** - Patient identifiers, diagnoses, medications naturally appear in clinical conversations
2. **Memory Persistence** - Conversation history must not persist indefinitely
3. **Encryption Requirements** - PHI at rest must be encrypted (HIPAA Security Rule)
4. **Access Controls** - Memory must be scoped to authorized users and sessions
5. **Audit Trail** - All PHI access must be logged for compliance
6. **Breach Risk** - Leaked memory could expose patient information

Standard LLM memory solutions (vector databases, conversation stores) are not designed for PHI protection.

## Decision

We will implement a **tiered memory architecture** with PHI encryption, automatic expiration, and comprehensive access controls.

### Memory Tiers

```
┌────────────────────────────────────────────────────────────────────┐
│                     MEMORY ARCHITECTURE                             │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              TIER 1: SESSION MEMORY (Redis)                  │  │
│  │  TTL: 15 minutes │ Encrypted: AES-256-GCM │ Scoped: Session  │  │
│  │                                                               │  │
│  │  • Active conversation turns                                  │  │
│  │  • Tool call results                                          │  │
│  │  • Working context                                            │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                      │
│                              ▼ (session end or timeout)            │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              TIER 2: TASK MEMORY (PostgreSQL)                │  │
│  │  TTL: 7 days │ Encrypted: Column-level │ Scoped: User/Task   │  │
│  │                                                               │  │
│  │  • Task summaries (PHI-scrubbed)                              │  │
│  │  • Decision audit trail                                       │  │
│  │  • Outcome tracking                                           │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                      │
│                              ▼ (compliance retention)              │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              TIER 3: AUDIT MEMORY (Immutable)                │  │
│  │  TTL: 7 years │ Encrypted: Full │ Scoped: Compliance         │  │
│  │                                                               │  │
│  │  • Access logs (who, what, when)                              │  │
│  │  • PHI access events                                          │  │
│  │  • Agent decision points                                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

### PHI Detection and Handling

```java
public class PHIDetector {
    // HIPAA 18 Safe Harbor Identifiers
    private static final List<Pattern> PHI_PATTERNS = List.of(
        // Direct identifiers
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),           // SSN
        Pattern.compile("\\b\\d{10}\\b"),                         // MRN (configurable)
        Pattern.compile("\\b[A-Z]{2}\\d{6,8}\\b"),               // Driver's license

        // Contact information
        Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"),   // Phone
        Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+"),              // Email

        // Dates (contextual - only birth/death/service dates)
        Pattern.compile("\\b(DOB|date of birth|born)\\s*:?\\s*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", Pattern.CASE_INSENSITIVE),

        // Geographic subdivisions smaller than state
        Pattern.compile("\\b\\d{5}(-\\d{4})?\\b"),               // ZIP code

        // Medical identifiers
        Pattern.compile("\\b[A-Z]\\d{2}[A-Z]?\\d{2}[A-Z]?\\d{3,4}\\b"),  // ICD codes (log but don't redact)
        Pattern.compile("\\bMRN\\s*:?\\s*[A-Z0-9-]+\\b", Pattern.CASE_INSENSITIVE)
    );

    public PHIDetectionResult detect(String text) {
        List<PHIMatch> matches = new ArrayList<>();
        for (Pattern pattern : PHI_PATTERNS) {
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                matches.add(new PHIMatch(m.start(), m.end(), m.group(), pattern));
            }
        }
        return new PHIDetectionResult(matches, calculateRiskScore(matches));
    }

    public String redact(String text) {
        PHIDetectionResult result = detect(text);
        String redacted = text;
        for (PHIMatch match : result.matches().reversed()) {
            redacted = redacted.substring(0, match.start())
                + "[REDACTED]"
                + redacted.substring(match.end());
        }
        return redacted;
    }
}
```

### Redis Session Memory Implementation

```java
@Service
public class RedisConversationMemory {

    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, String> redisTemplate;
    private final EncryptionService encryption;
    private final PHIDetector phiDetector;

    public void saveMessage(String sessionId, ConversationMessage message) {
        // Detect PHI for audit logging
        PHIDetectionResult phiResult = phiDetector.detect(message.getContent());
        if (phiResult.containsPHI()) {
            auditLogger.logPHIAccess(sessionId, message, phiResult);
        }

        // Encrypt before storage
        String encrypted = encryption.encrypt(objectMapper.writeValueAsString(message));

        // Store with TTL
        String key = "agent:session:" + sessionId + ":messages";
        redisTemplate.opsForList().rightPush(key, encrypted);
        redisTemplate.expire(key, SESSION_TTL);
    }

    public List<ConversationMessage> getHistory(String sessionId) {
        String key = "agent:session:" + sessionId + ":messages";
        List<String> encrypted = redisTemplate.opsForList().range(key, 0, -1);

        return encrypted.stream()
            .map(encryption::decrypt)
            .map(json -> objectMapper.readValue(json, ConversationMessage.class))
            .toList();
    }

    public void clearSession(String sessionId) {
        redisTemplate.delete("agent:session:" + sessionId + ":*");
        auditLogger.logSessionClear(sessionId);
    }
}
```

### Encryption Configuration

```yaml
hdim:
  security:
    memory:
      encryption:
        algorithm: AES/GCM/NoPadding
        key-size: 256
        key-rotation-days: 90
        key-source: aws-kms  # or vault, azure-keyvault

      redis:
        session-ttl-minutes: 15
        max-messages-per-session: 50
        cluster-mode: true
        tls-enabled: true

      postgres:
        task-retention-days: 7
        column-encryption: true
        audit-retention-years: 7
```

### Session Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│                     SESSION LIFECYCLE                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  [User Login] ─────► [Session Created] ─────► [Agent Interaction]│
│                            │                         │            │
│                            │                         ▼            │
│                            │            ┌─────────────────────┐  │
│                            │            │   PHI Detected?     │  │
│                            │            └──────────┬──────────┘  │
│                            │                       │              │
│                            │        ┌──────────────┼──────────┐  │
│                            │        ▼              ▼          │  │
│                            │    [Log to      [Encrypt &       │  │
│                            │     Audit]       Store in Redis] │  │
│                            │                                   │  │
│                            ▼                                   │  │
│             ┌─────────────────────────────────┐               │  │
│             │         SESSION ENDS            │               │  │
│             │  (timeout OR logout OR close)   │               │  │
│             └─────────────────────────────────┘               │  │
│                            │                                   │  │
│              ┌─────────────┴─────────────┐                    │  │
│              ▼                           ▼                    │  │
│     [Immediate Redis Purge]    [PHI-Scrubbed Summary          │  │
│                                 to Task Memory]               │  │
│                                                               │  │
└───────────────────────────────────────────────────────────────┘
```

### Access Control Matrix

| Memory Tier | User Access | Admin Access | Audit Access | Retention |
|-------------|-------------|--------------|--------------|-----------|
| Session (Redis) | Own session only | None | Read-only | 15 min |
| Task (Postgres) | Own tasks | Tenant tasks | Read-only | 7 days |
| Audit (Immutable) | None | None | Read-only | 7 years |

### Breach Detection

```java
@Component
public class MemoryBreachDetector {

    @Scheduled(fixedRate = 60000) // Every minute
    public void detectAnomalies() {
        // Unusual access patterns
        if (getSessionAccessCount(last5Minutes) > threshold) {
            alertSecurityTeam("Unusual session access volume");
        }

        // Cross-tenant access attempts
        List<AccessViolation> violations = findCrossTenantAttempts();
        violations.forEach(this::logAndAlert);

        // PHI exfiltration patterns
        if (detectBulkPHIAccess()) {
            alertSecurityTeam("Potential PHI exfiltration detected");
        }
    }
}
```

## Consequences

### Positive

- **HIPAA Compliant** - PHI encrypted at rest with automatic expiration
- **Minimal Exposure** - 15-minute session window limits breach impact
- **Full Audit Trail** - Every PHI access logged for 7 years
- **Automatic Cleanup** - No manual purging required
- **Breach Detection** - Anomaly monitoring reduces risk
- **Tenant Isolation** - Memory strictly scoped to tenant/user

### Negative

- **Context Loss** - Short session TTL means agents "forget" after 15 minutes
- **Performance Overhead** - Encryption/decryption adds latency (~5-10ms)
- **Operational Complexity** - Key rotation and management required
- **Cost** - Redis cluster and encryption infrastructure costs
- **Limited History** - Can't reference conversations from days ago

### Neutral

- Agents must be designed for short-context interactions
- Long-running tasks need explicit state management
- Users expect session-based memory model

## Implementation Notes

1. **Encryption keys** managed via AWS KMS (or Vault for on-prem)
2. **Redis Cluster** required for HA - single node creates SPOF
3. **PHI detection** is heuristic - false negatives possible
4. **Session affinity** required at load balancer for consistent access

## References

- HIPAA Security Rule: 45 CFR 164.312
- NIST SP 800-111: Guide to Storage Encryption Technologies
- HDIM Security Module: `backend/modules/shared/infrastructure/security/`
- Related ADR: ADR-0001 Multi-Provider LLM Architecture
