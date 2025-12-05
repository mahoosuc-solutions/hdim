# WebSocket Security Fixes - Implementation Checklist

**Priority:** 🔴 CRITICAL  
**Target Completion:** Week of 2025-12-03

---

## Phase 1: Critical Fixes (3-5 days)

### [ ] Fix 1: Resolve Bean Injection Error (2 hours)

**Files to Delete:**
- [ ] `src/main/java/com/healthdata/quality/service/notification/WebSocketNotificationChannel.java`

**Files to Modify:**
- [ ] `src/main/java/com/healthdata/quality/service/NotificationService.java`
  ```java
  // Change from:
  private final WebSocketNotificationChannel webSocketChannel;
  
  // To:
  private final HealthScoreWebSocketHandler webSocketHandler;
  
  // Update method:
  public void sendNotification(String tenantId, ClinicalAlertDTO alert) {
      Map<String, Object> alertData = Map.of(
          "type", "CLINICAL_ALERT",
          "severity", alert.getSeverity(),
          "alert", alert
      );
      webSocketHandler.broadcastHealthScoreUpdate(alertData, tenantId);
  }
  ```

**Test:**
- [ ] Service starts without errors
- [ ] No "SimpMessagingTemplate bean not found" error

---

### [ ] Fix 2: Implement JWT Authentication (2 days)

**Files to Create:**
- [ ] `src/main/java/com/healthdata/quality/config/JwtWebSocketHandshakeInterceptor.java`

**Implementation Checklist:**
- [ ] Extract JWT token from query parameter `?token=xxx`
- [ ] Validate JWT using `JwtTokenService`
- [ ] Extract username, roles, tenantIds from token
- [ ] Verify user has access to requested tenant
- [ ] Store user info in WebSocket session attributes
- [ ] Return `false` (reject) if authentication fails
- [ ] Return `true` (allow) if authentication succeeds

**Files to Modify:**
- [ ] `src/main/java/com/healthdata/quality/config/WebSocketConfig.java`
  ```java
  private final JwtWebSocketHandshakeInterceptor jwtHandshakeInterceptor;
  
  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      if (websocketEnabled) {
          registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
                  .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                  .addInterceptors(jwtHandshakeInterceptor);  // ✅ Add this
      }
  }
  ```

**Test:**
- [ ] Connection rejected without token
- [ ] Connection rejected with invalid token
- [ ] Connection rejected with expired token
- [ ] Connection allowed with valid token
- [ ] Connection rejected if user not in tenant
- [ ] User info available in session attributes

---

### [ ] Fix 3: Add Audit Logging (1 day)

**Files to Modify:**
- [ ] `src/main/java/com/healthdata/quality/websocket/HealthScoreWebSocketHandler.java`

**Implementation Checklist:**
- [ ] Add `AuditService` dependency
- [ ] Log WEBSOCKET_CONNECT event in `afterConnectionEstablished()`
  - [ ] Include: username, tenantId, IP address, timestamp
- [ ] Log WEBSOCKET_DISCONNECT event in `afterConnectionClosed()`
  - [ ] Include: username, tenantId, close status, duration
- [ ] Log WEBSOCKET_PHI_SENT event in `sendMessage()` for PHI data
  - [ ] Include: username, tenantId, patientId, message type
- [ ] Implement `isPhiMessage()` to detect PHI-containing messages
- [ ] Implement `extractPatientId()` to get patient ID from message

**Test:**
- [ ] Audit log created on connection
- [ ] Audit log created on disconnection
- [ ] Audit log created when PHI sent
- [ ] All required fields present in audit logs
- [ ] Audit logs searchable by username, tenantId, patientId

---

### [ ] Fix 4: Re-enable Security Filters (1 hour)

**Files to Modify:**
- [ ] `src/main/java/com/healthdata/quality/config/QualityMeasureSecurityConfig.java`

**Changes:**
```java
@Bean
@Profile("!test")
public SecurityFilterChain securityFilterChain(HttpSecurity http, 
                                               JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            
            // WebSocket endpoints - MUST BE AUTHENTICATED
            .requestMatchers("/ws/**").authenticated()
            
            // API endpoints - MUST BE AUTHENTICATED
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/patient-health/**").authenticated()
            .requestMatchers("/mental-health/**").authenticated()
            .requestMatchers("/care-gaps/**").authenticated()
            
            // All other requests require authentication
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

**Test:**
- [ ] Unauthenticated requests to /api/** return 401
- [ ] Valid JWT allows access to /api/**
- [ ] JWT filter is executing (check logs)

---

### [ ] Fix 5: Enable WSS (WebSocket Secure) (1 day)

**Files to Modify:**
- [ ] `src/main/resources/application-production.yml`

**Changes:**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: healthdata

websocket:
  enabled: true
  allowed-origins: https://clinical-portal.healthdata.com,https://admin-portal.healthdata.com
  secure: true
```

**Infrastructure:**
- [ ] Generate SSL certificate for production
- [ ] Configure Nginx reverse proxy for WebSocket
- [ ] Add SSL termination at Nginx
- [ ] Configure WebSocket upgrade headers
- [ ] Set connection timeout to 15 minutes

**Nginx Configuration:**
```nginx
location /quality-measure/ws/ {
    proxy_pass http://localhost:8087;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    
    proxy_read_timeout 900s;  # 15 minutes
    proxy_send_timeout 900s;
}
```

**Test:**
- [ ] wss:// connections work in production
- [ ] ws:// connections rejected in production
- [ ] SSL certificate valid
- [ ] No certificate warnings in browser

---

## Phase 2: High Priority Fixes (5-7 days)

### [ ] Fix 6: Implement Session Timeout (2 days)

**Files to Create:**
- [ ] `src/main/java/com/healthdata/quality/websocket/SessionTimeoutWebSocketHandler.java`

**Implementation:**
- [ ] Wrap existing handler with timeout decorator
- [ ] Schedule timeout task on connection
- [ ] Reset timeout on any message activity
- [ ] Close session after 15 minutes of inactivity
- [ ] Clean up timeout tasks on disconnect

**Configuration:**
```yaml
websocket:
  session:
    timeout: 900000  # 15 minutes in milliseconds
```

**Test:**
- [ ] Session auto-closes after 15 minutes of no activity
- [ ] Timeout resets when client sends message
- [ ] Multiple concurrent sessions each have own timeout
- [ ] Timeout tasks cleaned up on manual disconnect

---

### [ ] Fix 7: Implement Rate Limiting (2 days)

**Files to Create:**
- [ ] `src/main/java/com/healthdata/quality/config/RateLimitingWebSocketInterceptor.java`

**Implementation:**
- [ ] Track concurrent connections per user
- [ ] Track connection rate per minute per user
- [ ] Reject if user exceeds max concurrent connections (5)
- [ ] Reject if user exceeds connection rate (10/min)
- [ ] Use Guava LoadingCache with TTL

**Configuration:**
```yaml
websocket:
  rate-limit:
    max-connections-per-user: 5
    max-connections-per-minute: 10
```

**Test:**
- [ ] 6th concurrent connection rejected
- [ ] 11th connection in 1 minute rejected
- [ ] Rate limit resets after 1 minute
- [ ] Different users have separate limits

---

### [ ] Fix 8: Restrict Production Origins (1 hour)

**Files to Modify:**
- [ ] `src/main/resources/application-production.yml`

**Changes:**
```yaml
websocket:
  allowed-origins: https://clinical-portal.healthdata.com,https://admin-portal.healthdata.com
  # Remove all localhost origins
```

**Test:**
- [ ] Connection from allowed origin succeeds
- [ ] Connection from localhost rejected in production
- [ ] Connection from unknown domain rejected
- [ ] CORS headers properly set

---

## Phase 3: Additional Hardening (3-5 days)

### [ ] Fix 9: Add Message Validation (2 days)

**Implementation:**
- [ ] Validate message size (max 10KB)
- [ ] Validate JSON format
- [ ] Validate message type against whitelist
- [ ] Sanitize string fields
- [ ] Reject invalid messages

**Test:**
- [ ] Oversized messages rejected
- [ ] Invalid JSON rejected
- [ ] Unknown message types rejected
- [ ] XSS payloads sanitized

---

### [ ] Fix 10: CSRF Protection (1 day)

**Files to Create:**
- [ ] `src/main/java/com/healthdata/quality/config/CsrfWebSocketInterceptor.java`

**Implementation:**
- [ ] Extract CSRF token from query parameter
- [ ] Validate against expected token from session
- [ ] Reject if tokens don't match

**Test:**
- [ ] Connection rejected without CSRF token
- [ ] Connection rejected with invalid CSRF token
- [ ] Connection allowed with valid CSRF token

---

## Testing Checklist

### Unit Tests
- [ ] JwtWebSocketHandshakeInterceptor tests
- [ ] SessionTimeoutWebSocketHandler tests
- [ ] RateLimitingWebSocketInterceptor tests
- [ ] Audit logging tests

### Integration Tests
- [ ] End-to-end WebSocket connection with JWT
- [ ] Multi-tenant isolation
- [ ] Session timeout
- [ ] Rate limiting
- [ ] Audit log verification

### Security Tests
- [ ] Penetration test: Unauthenticated access attempt
- [ ] Penetration test: Cross-tenant access attempt
- [ ] Penetration test: CSWSH attack
- [ ] Penetration test: DoS via connection flooding
- [ ] SSL/TLS verification

### HIPAA Compliance Tests
- [ ] All PHI access logged
- [ ] Sessions auto-terminate after 15 minutes
- [ ] Only authenticated users can connect
- [ ] All data transmitted over WSS
- [ ] Audit logs contain required fields

---

## Documentation Updates

- [ ] Update WebSocket connection examples to use wss:// and token
- [ ] Document JWT token format for WebSocket
- [ ] Update security documentation
- [ ] Create runbook for WebSocket monitoring
- [ ] Document audit log fields and retention

---

## Deployment Checklist

### Pre-Deployment
- [ ] All Phase 1 fixes implemented
- [ ] All tests passing
- [ ] Code reviewed by security team
- [ ] SSL certificates ready for production
- [ ] Nginx configuration tested

### Deployment
- [ ] Deploy to staging environment
- [ ] Run smoke tests
- [ ] Verify audit logs working
- [ ] Load test WebSocket connections
- [ ] Deploy to production
- [ ] Monitor for errors

### Post-Deployment
- [ ] Verify WebSocket connections working
- [ ] Check audit logs in production
- [ ] Monitor connection counts
- [ ] Review security logs
- [ ] Schedule security audit

---

## Sign-off

- [ ] Development Lead
- [ ] Security Team
- [ ] Compliance Officer
- [ ] QA Lead
- [ ] DevOps Lead

---

**Status Tracking:**
- **Phase 1 Progress:** ___% complete
- **Phase 2 Progress:** ___% complete
- **Phase 3 Progress:** ___% complete
- **Overall Progress:** ___% complete

**Blockers:**
1. _____________________
2. _____________________

**Next Review Date:** _____________________
