# Deployment Health Check Summary

**Date:** 2025-11-28
**Status:** ⚠️  ISSUE FOUND - Quality Measure Service Failing to Start

---

## Health Check Results

### ✅ Services Passing Health Checks

1. **FHIR Service** (port 8083)
   - Status: Healthy
   - Uptime: 2 days

2. **Care Gap Service** (port 8085)
   - Status: Healthy
   - Uptime: 2 days

3. **Patient Service** (port 8084)
   - Status: Healthy
   - Uptime: 2 days

4. **Event Router Service** (port 8089)
   - Status: Healthy
   - Uptime: 2 days

5. **Gateway Service** (port 9000)
   - Status: Healthy
   - Uptime: 3 days

6. **Infrastructure Services**
   - Postgres: Healthy (2 days)
   - Kafka: Healthy (3 days)
   - Zookeeper: Healthy (3 days)
   - Redis: Healthy (3 days)

### ❌ Services Failing

1. **Quality Measure Service** (port 8087)
   - Status: FAILING TO START (crash loop)
   - Last restart: 8 seconds ago
   - Health: starting (never reaches healthy)

2. **CQL Engine Service** (port 8081)
   - Status: Unhealthy
   - Uptime: 2 days
   - Issue: Separate from quality-measure issue

---

## Critical Issue: Quality Measure Service

### Error Details

```
org.springframework.beans.factory.UnsatisfiedDependencyException:
Error creating bean with name 'webSocketNotificationChannel':
Unsatisfied dependency expressed through constructor parameter 0:
No qualifying bean of type 'org.springframework.messaging.simp.SimpMessagingTemplate' available
```

### Root Cause

The quality-measure-service has two conflicting WebSocket configurations:

1. **Existing WebSocket (Working)**
   - Location: `WebSocketConfig.java`
   - Uses: `@EnableWebSocket` (plain WebSocket)
   - Purpose: Real-time health score updates
   - Handler: `HealthScoreWebSocketHandler`
   - Status: ✅ Properly configured with HIPAA security

2. **New WebSocket Notification Channel (Broken)**
   - Location: `WebSocketNotificationChannel.java`
   - Requires: `SimpMessagingTemplate` (STOMP messaging)
   - Purpose: Multi-channel notifications
   - Status: ❌ Missing required STOMP configuration

### Technical Explanation

Spring provides two WebSocket approaches:

1. **Plain WebSocket** (`@EnableWebSocket`)
   - Lower-level WebSocket protocol
   - Uses `WebSocketHandler`
   - Currently implemented and working

2. **STOMP Messaging** (`@EnableWebSocketMessageBroker`)
   - Higher-level messaging protocol over WebSocket
   - Uses `SimpMessagingTemplate`
   - **NOT currently configured**

The `WebSocketNotificationChannel` (added for E2E testing) assumes STOMP messaging is available, but the service only has plain WebSocket configured.

---

## Impact Assessment

### What's Affected

- ❌ Quality Measure Service cannot start
- ❌ All quality measure calculations blocked
- ❌ Mental health assessments cannot be processed
- ❌ Care gap creation blocked
- ❌ Clinical alerts not generated
- ❌ Notification system not functional

### What Still Works

- ✅ FHIR data ingestion (FHIR service healthy)
- ✅ Patient management (Patient service healthy)
- ✅ Gateway routing (Gateway healthy)
- ✅ Event processing (Event Router healthy)
- ✅ E2E tests (use test profile which mocks SimpMessagingTemplate)

---

## Resolution Options

### Option 1: Add STOMP Support (Recommended for Full Features)

Add STOMP configuration to enable `SimpMessagingTemplate`:

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompMessagingConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Pros:**
- Enables full notification system functionality
- Supports both plain WebSocket AND STOMP messaging
- Future-proof for additional STOMP features

**Cons:**
- Requires testing to ensure no conflicts
- Adds additional WebSocket endpoint
- Slightly more complex configuration

### Option 2: Make WebSocketNotificationChannel Optional (Quick Fix)

Make the notification channel conditional on STOMP being available:

```java
@Service
@ConditionalOnBean(SimpMessagingTemplate.class)
public class WebSocketNotificationChannel implements NotificationChannel {
    // existing code
}
```

**Pros:**
- Service starts immediately
- No configuration changes needed
- Notifications work via other channels (Email, SMS)

**Cons:**
- WebSocket notifications disabled in production
- E2E tests may need adjustment
- Feature incomplete

### Option 3: Provide Mock Bean for Production (Temporary Fix)

Create a configuration that provides a no-op `SimpMessagingTemplate`:

```java
@Configuration
@ConditionalOnMissingBean(SimpMessagingTemplate.class)
public class WebSocketFallbackConfig {

    @Bean
    public SimpMessagingTemplate simpMessagingTemplate() {
        // Return mock or no-op implementation
        return new SimpMessagingTemplate(mock(MessageChannel.class));
    }
}
```

**Pros:**
- Service starts immediately
- Minimal code changes
- Easy rollback

**Cons:**
- WebSocket notifications silently fail
- Not a proper long-term solution
- May mask other issues

---

## Recommended Action Plan

### Immediate (Deploy Now)

1. **Option 2**: Make `WebSocketNotificationChannel` conditional
   - Add `@ConditionalOnBean(SimpMessagingTemplate.class)` annotation
   - Service will start successfully
   - Notifications work via Email/SMS channels
   - WebSocket notifications disabled until STOMP configured

2. **Redeploy Quality Measure Service**
   - Build with conditional annotation
   - Deploy to staging/production
   - Verify service starts and health check passes

3. **Document Known Limitation**
   - WebSocket notifications temporarily disabled
   - Email and SMS notifications fully functional
   - Plan STOMP configuration for next release

### Short-term (Next Sprint)

1. **Option 1**: Implement STOMP configuration
   - Add `@EnableWebSocketMessageBroker` configuration
   - Configure message broker and STOMP endpoints
   - Test integration with existing plain WebSocket
   - Verify no security regressions

2. **Update E2E Tests**
   - Test both plain WebSocket (health scores)
   - Test STOMP messaging (notifications)
   - Verify multi-channel notification delivery

3. **Security Review**
   - Ensure STOMP endpoints have same HIPAA controls
   - JWT authentication for STOMP connections
   - Tenant isolation for STOMP subscriptions
   - Audit logging for STOMP messages

---

## Testing Checklist

Before deploying the fix:

- [ ] Service starts without errors
- [ ] Health endpoint returns UP
- [ ] Database connections successful
- [ ] Kafka consumers start
- [ ] Mental health assessment submission works
- [ ] Care gap creation works
- [ ] Email notifications work
- [ ] SMS notifications work (if configured)
- [ ] Existing WebSocket health scores still work
- [ ] No regression in other services

---

## Files Requiring Changes

### For Option 2 (Quick Fix - Recommended Now):

1. `WebSocketNotificationChannel.java`
   - Add: `@ConditionalOnBean(SimpMessagingTemplate.class)`
   - Location: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/`

### For Option 1 (Full Solution - Next Sprint):

1. `StompMessagingConfig.java` (NEW FILE)
   - Create: STOMP WebSocket configuration
   - Location: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/`

2. `WebSocketSecurityConfig.java` (NEW FILE - Optional)
   - Create: Security configuration for STOMP endpoints
   - Location: Same as above

---

## Deployment Commands

### Build with Quick Fix
```bash
cd backend/modules/services/quality-measure-service
# Apply Option 2 fix first
../../../gradlew build -x test --no-daemon
```

### Rebuild Docker Image
```bash
cd backend
docker-compose build quality-measure-service
```

### Restart Service
```bash
docker-compose up -d --force-recreate quality-measure-service
```

### Verify Health
```bash
# Wait 30 seconds for startup
sleep 30

# Check health
curl -s http://localhost:8087/actuator/health | jq .

# Should return: {"status":"UP"}
```

---

## Summary

**Current Status:** Quality Measure Service is in crash loop due to missing STOMP messaging configuration

**Root Cause:** `WebSocketNotificationChannel` requires `SimpMessagingTemplate` but service only has plain WebSocket configured

**Quick Fix:** Add `@ConditionalOnBean(SimpMessagingTemplate.class)` to make WebSocket notifications optional

**Proper Fix:** Implement STOMP messaging configuration in next sprint

**Impact:** Email/SMS notifications work; WebSocket notifications temporarily disabled

**Timeline:**
- Quick fix: 15 minutes
- Full fix: 2-4 hours (next sprint)
