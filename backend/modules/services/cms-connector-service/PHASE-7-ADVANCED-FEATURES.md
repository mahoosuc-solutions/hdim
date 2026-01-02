# Phase 7: Advanced Features & Product Maturity

**Status**: Not Started  
**Duration**: 4+ weeks  
**Priority**: 🟡 MEDIUM  
**Team**: Backend, Frontend, Product  

## Overview

Phase 7 focuses on product maturity, developer experience, and advanced operational capabilities. This phase transforms a functioning service into a platform.

---

## Week 1: API Documentation & SDKs

### Objectives
- Complete API documentation
- Client SDKs for major languages
- Code examples and tutorials
- Developer onboarding

### OpenAPI/Swagger Specification

```java
// src/main/java/com/healthdata/cms/api/config/OpenApiConfig.java

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("CMS Connector Service API")
        .version("1.0.0")
        .description("Medicare Claim Data Integration Service")
        .contact(new Contact()
          .name("API Support")
          .url("https://healthdata.com/support")
          .email("api-support@healthdata.com"))
        .license(new License()
          .name("Apache 2.0")
          .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
      .externalDocs(new ExternalDocumentation()
        .url("https://docs.healthdata.com/cms-connector")
        .description("External documentation"));
  }
}

@RestController
@RequestMapping("/api/v1")
public class DataController {
  @Operation(summary = "Get claim data",
    description = "Retrieve Medicare claim data by ID",
    tags = {"Claims"})
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200",
      description = "Claim data found",
      content = @Content(schema = @Schema(implementation = ClaimDTO.class))),
    @ApiResponse(responseCode = "404",
      description = "Claim not found"),
    @ApiResponse(responseCode = "500",
      description = "Internal server error")
  })
  @GetMapping("/claims/{id}")
  public ResponseEntity<ClaimDTO> getClaimById(
    @PathVariable @Parameter(description = "Claim ID") String id) {
    // Implementation
  }
}
```

Generate OpenAPI spec: `mvn springdoc-openapi:generate`

### Client SDKs

**Java SDK** (`sdks/java/`)
```xml
<!-- pom.xml -->
<groupId>com.healthdata.cms</groupId>
<artifactId>cms-connector-java-client</artifactId>
<version>1.0.0</version>

<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
</dependency>
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
</dependency>
```

**Example Usage**:
```java
CmsConnectorClient client = CmsConnectorClient.builder()
  .apiKey("your-api-key")
  .baseUrl("https://cms-connector.example.com")
  .build();

// Get claim data
ClaimDTO claim = client.claims().get("claim-123");

// Search claims
List<ClaimDTO> claims = client.claims().search(
  SearchRequest.builder()
    .patientId("patient-456")
    .fromDate("2024-01-01")
    .toDate("2024-12-31")
    .build());

// Batch operations
client.claims().batch()
  .add("claim-123")
  .add("claim-456")
  .execute();
```

**Python SDK** (`sdks/python/`)
```python
# cms_connector/client.py
from cms_connector.api import ClaimsAPI
from cms_connector.auth import ApiKeyAuth

class CmsConnectorClient:
    def __init__(self, api_key: str, base_url: str = "https://cms-connector.example.com"):
        self.auth = ApiKeyAuth(api_key)
        self.base_url = base_url
        self.claims = ClaimsAPI(self.auth, self.base_url)

# Usage
client = CmsConnectorClient("your-api-key")

# Get claim
claim = client.claims.get("claim-123")

# Search claims
claims = client.claims.search(
    patient_id="patient-456",
    from_date="2024-01-01",
    to_date="2024-12-31"
)

# Batch operations
response = client.claims.batch_get(["claim-123", "claim-456"])
```

**JavaScript/TypeScript SDK** (`sdks/js/`)
```typescript
// src/CmsConnectorClient.ts
import axios, { AxiosInstance } from 'axios';

export class CmsConnectorClient {
  private axiosInstance: AxiosInstance;

  constructor(apiKey: string, baseUrl: string = 'https://cms-connector.example.com') {
    this.axiosInstance = axios.create({
      baseURL: baseUrl,
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json'
      }
    });
  }

  async getClaim(id: string): Promise<Claim> {
    const response = await this.axiosInstance.get(`/api/v1/claims/${id}`);
    return response.data;
  }

  async searchClaims(params: SearchParams): Promise<Claim[]> {
    const response = await this.axiosInstance.get('/api/v1/claims', { params });
    return response.data;
  }
}

// Usage
const client = new CmsConnectorClient('your-api-key');
const claim = await client.getClaim('claim-123');
```

### Developer Documentation

Create `docs/api-guide.md`:
```markdown
# CMS Connector Service API Guide

## Quick Start

### 1. Get API Key
1. Go to https://dashboard.healthdata.com
2. Create API key (no expiration / with expiration)
3. Store securely

### 2. Install SDK
```bash
# Java
mvn install com.healthdata.cms:cms-connector-java-client:1.0.0

# Python
pip install cms-connector

# JavaScript
npm install @healthdata/cms-connector
```

### 3. Make Your First API Call
[Examples for each language]

## Authentication
- API Key: `Authorization: Bearer YOUR_API_KEY`
- OAuth 2.0: Client credentials flow
- Scope-based permissions

## Rate Limiting
- 100 requests/minute per API key
- 10,000 requests/day
- Burst allowance: 20 requests/second

## Pagination
```
GET /api/v1/claims?page=1&limit=100
Response:
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 100,
    "total": 5000,
    "pages": 50
  }
}
```
```

### Success Criteria
- [ ] OpenAPI spec complete and accurate
- [ ] Java SDK published to Maven Central
- [ ] Python SDK published to PyPI
- [ ] JavaScript SDK published to NPM
- [ ] Code examples for all major operations
- [ ] Developer onboarding < 1 hour
- [ ] SDK test coverage > 80%

---

## Week 2: Rate Limiting & Quota Management

### Rate Limiting Implementation

```java
// src/main/java/com/healthdata/cms/ratelimit/RateLimitingAspect.java

@Component
@Aspect
public class RateLimitingAspect {
  private final RateLimitService rateLimitService;

  @Before("@annotation(rateLimit)")
  public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
    String clientId = getClientId();
    
    if (!rateLimitService.allowRequest(clientId, rateLimit.limit())) {
      throw new RateLimitExceededException(
        "Rate limit exceeded: " + rateLimit.limit() + " requests per " + rateLimit.window());
    }
  }

  private String getClientId() {
    // Extract from JWT or API key
    return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
  }
}

// Usage
@RestController
@RequestMapping("/api/v1")
public class DataController {
  @GetMapping("/claims/{id}")
  @RateLimit(limit = 100, window = "MINUTE")
  public ResponseEntity<ClaimDTO> getClaimById(@PathVariable String id) {
    // Implementation
  }
}
```

### Quota Management

```java
// src/main/java/com/healthdata/cms/quota/QuotaService.java

@Service
public class QuotaService {
  private final QuotaRepository quotaRepository;

  public QuotaStatus getQuotaStatus(String apiKey) {
    Quota quota = quotaRepository.findByApiKey(apiKey);
    
    return QuotaStatus.builder()
      .requestsUsed(quota.getRequestsUsedThisMonth())
      .requestsLimit(quota.getMonthlyRequestLimit())
      .percentageUsed(quota.getPercentageUsed())
      .resetDate(quota.getResetDate())
      .build();
  }

  public void warnIfNearLimit(String apiKey) {
    QuotaStatus status = getQuotaStatus(apiKey);
    
    if (status.getPercentageUsed() > 80) {
      notificationService.sendWarning(apiKey, 
        "You have used " + status.getPercentageUsed() + "% of your monthly quota");
    }
  }
}
```

### Success Criteria
- [ ] Rate limiting working and tested
- [ ] Quota tracking accurate
- [ ] Warning notifications sent
- [ ] Rate limit headers in responses
- [ ] Graceful degradation when limits exceeded

---

## Week 3: Advanced Caching & Performance

### Multi-Level Caching Strategy

```java
// src/main/java/com/healthdata/cms/cache/CacheManager.java

@Service
public class CachingService {
  private final LocalCache localCache;
  private final RedisTemplate<String, Object> redisTemplate;

  public ClaimDTO getClaim(String id) {
    // Level 1: Local cache (JVM)
    ClaimDTO cached = localCache.get("claim:" + id);
    if (cached != null) {
      metrics.increment("cache.hit.local");
      return cached;
    }

    // Level 2: Distributed cache (Redis)
    cached = redisTemplate.opsForValue().get("claim:" + id);
    if (cached != null) {
      localCache.put("claim:" + id, cached);  // Warm local cache
      metrics.increment("cache.hit.redis");
      return cached;
    }

    // Level 3: Database
    ClaimDTO claim = claimRepository.findById(id);
    
    // Populate caches
    redisTemplate.opsForValue().set("claim:" + id, claim, Duration.ofMinutes(30));
    localCache.put("claim:" + id, claim);
    
    metrics.increment("cache.miss");
    return claim;
  }
}
```

### Cache Warming

```java
@Component
public class CacheWarmer {
  @Scheduled(cron = "0 0 * * * *")  // Hourly
  public void warmCaches() {
    List<String> hotClaimIds = claimRepository.findMostAccessedClaims(1000);
    
    for (String claimId : hotClaimIds) {
      cachingService.getClaim(claimId);  // Loads into cache
    }
  }
}
```

### Success Criteria
- [ ] Cache hit ratio > 80%
- [ ] Local cache reduces latency
- [ ] Redis handles distributed caching
- [ ] Cache invalidation working
- [ ] Cache warming improving performance

---

## Week 4: Multi-Tenancy & Advanced Features (Optional)

### Data Isolation Per Tenant

```java
// src/main/java/com/healthdata/cms/tenancy/TenantContext.java

public class TenantContext {
  private static final ThreadLocal<String> tenantId = new ThreadLocal<>();

  public static void setTenantId(String id) {
    tenantId.set(id);
  }

  public static String getTenantId() {
    return tenantId.get();
  }

  public static void clear() {
    tenantId.remove();
  }
}

// Database schema per tenant or shared with row-level security
@RestController
@RequestMapping("/api/v1")
public class DataController {
  @GetMapping("/claims/{id}")
  public ResponseEntity<ClaimDTO> getClaim(@PathVariable String id) {
    String tenantId = extractTenantFromJWT();
    TenantContext.setTenantId(tenantId);
    
    try {
      ClaimDTO claim = claimRepository.findById(id);  // Auto-filtered by tenant
      return ResponseEntity.ok(claim);
    } finally {
      TenantContext.clear();
    }
  }
}
```

### Billing & Usage Tracking

```java
@Service
public class UsageTrackingService {
  private final UsageRepository usageRepository;

  @Async
  public void trackUsage(String tenantId, String operation, long duration) {
    UsageRecord record = UsageRecord.builder()
      .tenantId(tenantId)
      .operation(operation)
      .duration(duration)
      .timestamp(LocalDateTime.now())
      .build();
    
    usageRepository.save(record);
  }

  public BillingReport generateMonthlyReport(String tenantId, YearMonth month) {
    List<UsageRecord> usage = usageRepository.findByTenantAndMonth(tenantId, month);
    
    return BillingReport.builder()
      .tenantId(tenantId)
      .period(month)
      .totalRequests(usage.size())
      .totalDataProcessed(usage.stream().mapToLong(u -> u.getDataSize()).sum())
      .cost(calculateCost(usage))
      .build();
  }
}
```

### Success Criteria
- [ ] Multi-tenancy architecture working
- [ ] Data isolation guaranteed
- [ ] Billing calculations accurate
- [ ] Usage analytics available
- [ ] Tenant configuration flexible

---

## Deliverables Summary

### Week 1: APIs & Documentation
- [ ] OpenAPI specification complete
- [ ] 3 SDKs (Java, Python, JavaScript)
- [ ] Developer documentation
- [ ] Code examples for all operations
- [ ] API playground/explorer

### Week 2: Rate Limiting & Quotas
- [ ] Rate limiting working
- [ ] Quota enforcement
- [ ] Usage dashboards
- [ ] Billing integration
- [ ] Customer notifications

### Week 3: Performance
- [ ] Multi-level caching
- [ ] Cache hit ratio > 80%
- [ ] Response latency < 100ms (p95)
- [ ] Database query optimization
- [ ] Cache warming procedures

### Week 4: Advanced Features (Optional)
- [ ] Multi-tenancy
- [ ] Usage tracking
- [ ] Billing system
- [ ] Tenant-specific configuration
- [ ] Advanced analytics

---

## Key Files

```
sdks/
├── java/
│   ├── pom.xml
│   ├── src/main/java/com/healthdata/cms/
│   │   ├── CmsConnectorClient.java
│   │   ├── api/ClaimsAPI.java
│   │   └── model/*.java
│   └── README.md
├── python/
│   ├── setup.py
│   ├── cms_connector/
│   │   ├── client.py
│   │   ├── api/
│   │   └── models/
│   └── README.md
└── js/
    ├── package.json
    ├── src/
    │   ├── CmsConnectorClient.ts
    │   ├── api/
    │   └── models/
    └── README.md

src/main/java/com/healthdata/cms/
├── ratelimit/
│   ├── RateLimitService.java
│   └── RateLimitingAspect.java
├── quota/
│   ├── QuotaService.java
│   └── QuotaRepository.java
├── cache/
│   ├── CachingService.java
│   └── CacheWarmer.java
└── tenancy/
    ├── TenantContext.java
    └── TenantInterceptor.java

docs/
├── api-guide.md
├── sdk-guide.md
├── rate-limiting.md
├── quota-management.md
└── examples/
    ├── java/
    ├── python/
    └── javascript/
```

---

## Success Metrics

- Developer onboarding time: < 1 hour
- SDK adoption rate: > 80% of users
- API availability: 99.9%+
- Response latency p95: < 100ms
- Cache hit ratio: > 80%
- Error rate: < 0.1%

---

## Budget Estimate
- Additional infrastructure: $500-1,000/month
- Development effort: 120-200 hours
- Tools & services: $0-500/month
