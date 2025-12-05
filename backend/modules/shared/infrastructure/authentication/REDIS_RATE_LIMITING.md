# Redis-Based Distributed Rate Limiting

## Overview

This document describes the Redis-backed distributed rate limiting implementation for the authentication infrastructure. This system replaces the in-memory rate limiting with a distributed solution that works across multiple service instances.

## Architecture

### Why Redis vs In-Memory?

**In-Memory Limitations:**
- Rate limits are per-instance, not shared across multiple service instances
- Rate limits are lost when application restarts
- Cannot scale horizontally while maintaining consistent rate limiting
- No centralized visibility into rate limiting status

**Redis Benefits:**
- Rate limits shared across all service instances (horizontal scaling)
- Rate limits persist across application restarts
- Centralized management and monitoring
- Real-time visibility into rate limiting status
- Atomic operations ensure consistency

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Request                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              RateLimitingFilter                              │
│  • Intercepts all requests                                   │
│  • Attempts Redis-backed rate limiting                       │
│  • Falls back to in-memory if Redis unavailable              │
│  • Records metrics for monitoring                            │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│ Redis Available  │    │ Redis Unavailable│
│                  │    │                  │
│ RedisBucket      │    │ In-Memory Cache  │
│ ProviderService  │    │ (ConcurrentMap)  │
└──────────────────┘    └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Redis                                   │
│  • Stores Bucket4j token buckets                            │
│  • Key format: rate-limit:{ip}:{endpoint}                   │
│  • Automatic expiration prevents memory leaks               │
│  • Shared across all service instances                      │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

1. **RedisRateLimitConfig**: Configures Redisson client and Bucket4j ProxyManager
2. **RedisBucketProviderService**: Manages Redis-backed rate limiting buckets
3. **RateLimitStatsService**: Provides statistics and admin operations
4. **RateLimitController**: REST API for admin management (SUPER_ADMIN only)
5. **RateLimitMetrics**: Prometheus metrics for monitoring
6. **RateLimitCleanupScheduler**: Periodic cleanup and health checks
7. **RateLimitingFilter**: Main filter that enforces rate limits

## Configuration

### Required Properties

Add to `application.yml`:

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0

rate-limiting:
  enabled: true

  redis:
    enabled: true              # Enable Redis-backed rate limiting
    database: 1                # Separate Redis database for rate limiting
    key-prefix: "rate-limit:"  # Prefix for all rate limit keys
    connection-pool-size: 64   # Redisson connection pool size
    connection-minimum-idle-size: 10
    connect-timeout: 10000     # 10 seconds
    timeout: 3000              # 3 seconds

  login:
    per-minute: 5   # Max login attempts per minute per IP
    per-hour: 20    # Max login attempts per hour per IP

  register:
    per-hour: 3     # Max registration attempts per hour per IP

  api:
    per-minute: 100 # Max API requests per minute per IP

  cleanup:
    cron: "0 0 * * * *"        # Hourly cleanup (every hour at minute 0)
    daily-cron: "0 0 0 * * *"  # Daily cleanup (midnight)

  health-check:
    cron: "0 */5 * * * *"      # Health check every 5 minutes
```

### Test Configuration

Add to `application-test.yml`:

```yaml
spring:
  redis:
    host: localhost
    port: 6379  # Embedded Redis will start on this port

rate-limiting:
  enabled: true
  redis:
    enabled: true
    database: 15  # Separate database for tests
```

## Rate Limit Policies

### Login Endpoint (`/api/v1/auth/login`)
- **5 requests per minute** - Prevents rapid brute force attacks
- **20 requests per hour** - Prevents distributed brute force attacks
- Two-tier protection provides defense in depth

### Register Endpoint (`/api/v1/auth/register`)
- **3 requests per hour** - Prevents automated account creation
- Very restrictive to prevent abuse

### General API Endpoints
- **100 requests per minute** - Allows normal application usage
- More lenient for legitimate API operations

## Admin API

All admin endpoints require `SUPER_ADMIN` role and are available at `/api/v1/admin/rate-limits`.

### Get Aggregated Statistics
```bash
GET /api/v1/admin/rate-limits/stats
```

Response:
```json
{
  "totalBuckets": 156,
  "blockedIPCount": 3,
  "blockedIPs": ["192.168.1.100", "203.0.113.45", "198.51.100.23"],
  "redisAvailable": true,
  "keyPrefix": "rate-limit:",
  "redisDatabase": 1
}
```

### Get Blocked IPs
```bash
GET /api/v1/admin/rate-limits/blocked
```

Response:
```json
["192.168.1.100", "203.0.113.45", "198.51.100.23"]
```

### Get Rate Limits for Specific IP
```bash
GET /api/v1/admin/rate-limits/192.168.1.100
```

Response:
```json
[
  {
    "ip": "192.168.1.100",
    "endpoint": "/api/v1/auth/login",
    "exists": true,
    "lastAccessed": "2025-11-06T10:30:00Z"
  },
  {
    "ip": "192.168.1.100",
    "endpoint": "/api/v1/auth/register",
    "exists": true,
    "lastAccessed": "2025-11-06T10:25:00Z"
  }
]
```

### Reset Rate Limits for Specific IP
```bash
DELETE /api/v1/admin/rate-limits/192.168.1.100
```

Response:
```json
{
  "ip": "192.168.1.100",
  "rateLimitsReset": 2,
  "message": "Rate limits reset successfully"
}
```

### Reset Specific Rate Limit
```bash
DELETE /api/v1/admin/rate-limits/192.168.1.100/endpoint?endpoint=/api/v1/auth/login
```

### Get Top Rate-Limited IPs
```bash
GET /api/v1/admin/rate-limits/top-limited?limit=10
```

Response:
```json
{
  "192.168.1.100": 156,
  "203.0.113.45": 89,
  "198.51.100.23": 67
}
```

### Reset ALL Rate Limits (DANGEROUS!)
```bash
DELETE /api/v1/admin/rate-limits?confirm=YES_I_AM_SURE
```

**WARNING**: This resets ALL rate limits across all IPs and endpoints. Use with extreme caution!

## Monitoring with Prometheus

### Available Metrics

**rate_limit_requests_total{endpoint, result}**
- Counter tracking total requests (allowed/blocked)
- Labels: endpoint, result (allowed/blocked)

**rate_limit_blocks_total{endpoint, ip}**
- Counter tracking times an IP was blocked
- Labels: endpoint, ip (anonymized)

**rate_limit_redis_errors_total**
- Counter tracking Redis connection errors
- Helps detect Redis availability issues

**rate_limit_fallback_total**
- Counter tracking fallbacks to in-memory rate limiting
- Indicates Redis unavailability

**rate_limit_blocked_ips**
- Gauge showing current number of blocked IPs
- Updated hourly by cleanup scheduler

### Example Prometheus Queries

**Block rate by endpoint:**
```promql
rate(rate_limit_requests_total{result="blocked"}[5m]) / rate(rate_limit_requests_total[5m])
```

**Top blocked IPs:**
```promql
topk(10, rate_limit_blocks_total)
```

**Redis error rate:**
```promql
rate(rate_limit_redis_errors_total[5m])
```

**Alert on high block rate:**
```yaml
- alert: HighRateLimitBlockRate
  expr: |
    rate(rate_limit_requests_total{result="blocked"}[5m]) /
    rate(rate_limit_requests_total[5m]) > 0.3
  for: 10m
  annotations:
    summary: "High rate of blocked requests (>30%)"
    description: "Possible attack or misconfigured rate limits"
```

## Failover and Fallback Behavior

### Redis Unavailable Scenario

1. **Detection**: First Redis error is encountered
2. **Fallback**: Filter switches to in-memory cache (ConcurrentHashMap)
3. **Metrics**: Records `redis_error` and `fallback` metrics
4. **Logging**: Warns that distributed rate limiting is disabled
5. **Recovery**: Health check attempts reconnection every 5 minutes

### Impact of Fallback

**During Fallback:**
- Rate limits are per-instance (not shared)
- Each service instance has independent limits
- Effective rate limit is multiplied by number of instances
- Example: 5 req/min × 3 instances = 15 req/min total

**When Redis Returns:**
- System automatically resumes Redis-backed rate limiting
- In-memory cache is abandoned
- Distributed rate limiting is restored

## Redis Setup Requirements

### Production Setup

**Recommended Configuration:**
```redis
# Redis configuration (redis.conf)

# Persistence
save 900 1       # Save after 900 seconds if 1 key changed
save 300 10      # Save after 300 seconds if 10 keys changed
save 60 10000    # Save after 60 seconds if 10000 keys changed

# Memory management
maxmemory 256mb
maxmemory-policy allkeys-lru  # Evict least recently used keys

# Security
requirepass your_secure_password_here
bind 0.0.0.0  # Or specific interface
protected-mode yes

# Performance
tcp-backlog 511
timeout 300
tcp-keepalive 300
```

### Docker Compose Example

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

volumes:
  redis-data:
```

## Performance Characteristics

### Latency

- **Redis Operation**: 1-3ms (LAN)
- **Bucket Token Consumption**: ~2-5ms total
- **In-Memory Fallback**: <1ms

### Throughput

- **Single Redis Instance**: 100,000+ ops/sec
- **With Connection Pooling**: Scales with pool size
- **Bucket4j Overhead**: Minimal (<1% CPU)

### Memory Usage

- **Per Bucket**: ~100-200 bytes in Redis
- **1000 Buckets**: ~100-200 KB
- **10,000 Buckets**: ~1-2 MB
- **Automatic Expiration**: Prevents memory leaks

## Troubleshooting

### Redis Connection Failures

**Symptom**: Logs show "Redis unavailable for rate limiting"

**Check:**
```bash
# Test Redis connectivity
redis-cli -h localhost -p 6379 ping

# Check authentication
redis-cli -h localhost -p 6379 -a your_password ping

# View rate limit keys
redis-cli -h localhost -p 6379 -a your_password --scan --pattern "rate-limit:*"
```

**Resolution:**
1. Verify Redis is running
2. Check network connectivity
3. Verify password/authentication
4. Check Redis logs for errors

### High Block Rate

**Symptom**: Many requests being blocked

**Check:**
```bash
# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep rate_limit

# Check admin API
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/admin/rate-limits/stats
```

**Possible Causes:**
1. Legitimate traffic spike (increase limits)
2. Actual attack (investigate IPs)
3. Misconfigured rate limits (review config)
4. Test/development traffic (whitelist IPs)

### Rate Limits Not Shared Across Instances

**Symptom**: Each instance has independent limits

**Check:**
1. Verify Redis is configured: `rate-limiting.redis.enabled=true`
2. Check all instances connect to same Redis: `spring.redis.host`
3. Verify same database: `rate-limiting.redis.database`
4. Check Redis logs for connection errors

### Memory Leaks

**Symptom**: Redis memory grows continuously

**Check:**
```bash
# Check Redis memory
redis-cli -h localhost -p 6379 -a password INFO memory

# Count rate limit keys
redis-cli -h localhost -p 6379 -a password --scan --pattern "rate-limit:*" | wc -l

# Check key expiration
redis-cli -h localhost -p 6379 -a password TTL "rate-limit:some-key"
```

**Resolution:**
1. Verify maxmemory policy is set: `maxmemory-policy allkeys-lru`
2. Check Bucket4j expiration strategy is configured
3. Ensure cleanup scheduler is running

## Testing

### Unit Tests

Existing unit tests (`RateLimitingFilterTest`) still pass and test:
- In-memory fallback behavior
- Rate limit enforcement
- Multiple endpoints
- IP isolation
- X-Forwarded-For handling

### Integration Tests

New integration tests (`RedisRateLimitingIntegrationTest`) test:
- Redis storage (17 tests)
- Distributed behavior
- Admin operations
- Statistics tracking
- Bucket management
- Failover scenarios

### Running Tests

```bash
# Run all tests
./gradlew :modules:shared:infrastructure:authentication:test

# Run only integration tests
./gradlew :modules:shared:infrastructure:authentication:test --tests "*IntegrationTest"

# Run with specific profile
./gradlew :modules:shared:infrastructure:authentication:test -Dspring.profiles.active=test
```

## Migration from In-Memory

### Steps

1. **Add Dependencies**: Already done in build.gradle.kts
2. **Configure Redis**: Add Redis connection properties
3. **Enable Redis Rate Limiting**: Set `rate-limiting.redis.enabled=true`
4. **Deploy**: Rolling deployment recommended
5. **Monitor**: Watch metrics for Redis errors
6. **Verify**: Check admin API shows distributed limits

### Rollback Plan

If issues occur:
```yaml
rate-limiting:
  redis:
    enabled: false  # Disable Redis, use in-memory
```

System automatically falls back to in-memory implementation.

## Best Practices

### Security

1. **Protect Admin API**: Only SUPER_ADMIN should access
2. **Monitor Block Rate**: Alert on unusual patterns
3. **Review Top IPs**: Investigate heavily blocked IPs
4. **Secure Redis**: Use authentication and TLS in production
5. **Regular Audits**: Review rate limit logs periodically

### Performance

1. **Connection Pooling**: Configure adequate pool size
2. **Redis Proximity**: Keep Redis close to application (low latency)
3. **Monitor Metrics**: Watch Redis error rate
4. **Tune Limits**: Adjust based on actual usage patterns

### Operations

1. **Health Checks**: Monitor Redis availability
2. **Backup Config**: Document rate limit policies
3. **Incident Response**: Have runbook for high block rates
4. **Capacity Planning**: Monitor Redis memory usage

## Conclusion

Redis-backed distributed rate limiting provides:
- Horizontal scalability
- Consistent rate limiting across instances
- Centralized management and monitoring
- Graceful fallback to in-memory
- Production-ready observability

The system is designed for high availability with automatic failover, comprehensive monitoring, and easy administration through REST API and Prometheus metrics.
