# WebSocket Security & Best Practices Audit Report
## Quality Measure Service - HIPAA Healthcare Application

**Audit Date:** 2025-11-26  
**Auditor:** Security Analysis  
**Service:** Quality Measure Service (Port 8087)  
**Classification:** HIPAA-Compliant Healthcare Application - PHI Transmission

---

## Executive Summary

This audit reveals **CRITICAL SECURITY VULNERABILITIES** in the WebSocket implementation that pose significant risks for HIPAA compliance and data security. The service has **two conflicting WebSocket architectures** implemented simultaneously, causing a startup failure and creating multiple security gaps.

**Critical Findings:**
- ❌ No authentication/authorization on WebSocket connections
- ❌ Unencrypted WebSocket (WS) instead of WSS in production
- ❌ PHI transmitted without encryption over WebSocket
- ❌ Missing audit logging for PHI access
- ❌ Two conflicting WebSocket patterns causing bean injection failure
- ❌ CSRF token validation not implemented
- ❌ No session timeout configuration
- ❌ Rate limiting not implemented for WebSocket
- ❌ Origin validation bypassed by wildcard configuration

**Overall Security Rating:** 🔴 **CRITICAL - HIPAA NON-COMPLIANT**

---

## Part 1: Current Implementation Analysis

### 1.1 Architecture Overview

The service implements **TWO DIFFERENT WebSocket patterns simultaneously**:

#### Pattern 1: Raw WebSocket (TextWebSocketHandler)
- **File:** `HealthScoreWebSocketHandler.java`
- **Config:** `WebSocketConfig.java` with `@EnableWebSocket`
- **Endpoint:** `/ws/health-scores`
- **Protocol:** Raw WebSocket with custom JSON messages
- **Status:** ✅ Fully implemented and functional

#### Pattern 2: STOMP over WebSocket (SimpMessagingTemplate)
- **File:** `WebSocketNotificationChannel.java`
- **Config:** ❌ **MISSING** - No `@EnableWebSocketMessageBroker` configuration
- **Endpoint:** `/topic/alerts/{tenantId}`
- **Protocol:** STOMP with message broker
- **Status:** ❌ **BROKEN** - Bean dependency cannot be resolved

### 1.2 Dependency Analysis

**build.gradle.kts:**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-websocket")
```

**Analysis:**
- `spring-boot-starter-websocket` includes BOTH raw WebSocket AND STOMP support
- Raw WebSocket is configured via `WebSocketConfig.java`
- STOMP requires additional configuration via `@EnableWebSocketMessageBroker`
- **Missing configuration causes bean injection failure**

### 1.3 Current Endpoints

| Endpoint | Pattern | Protocol | Status |
|----------|---------|----------|--------|
| `/ws/health-scores` | Raw WebSocket | Custom JSON | ✅ Working |
| `/topic/alerts/{tenantId}` | STOMP | Message Broker | ❌ Broken |

---

## Part 2: Security Assessment

### 2.1 Authentication & Authorization

#### Finding 1: No Authentication on WebSocket Handshake
**Severity:** 🔴 **CRITICAL**

**Issue:**
```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    if (websocketEnabled) {
        registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .addInterceptors(new HttpSessionHandshakeInterceptor());  // ❌ No JWT validation
    }
}
```

**Problems:**
- `HttpSessionHandshakeInterceptor` only copies HTTP session attributes
- No JWT token validation during handshake
- No user authentication required to connect
- Anyone can connect and receive real-time PHI updates

**Impact:**
- Unauthenticated users can access patient health data
- HIPAA violation: No access controls on PHI
- Data breach risk

**Evidence:**
```java
// From HealthScoreWebSocketHandler.java line 52
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String sessionId = session.getId();
    sessions.put(sessionId, session);  // ❌ No authentication check
    
    String tenantId = extractTenantId(session);  // ❌ Tenant from query param, not validated
}
```

#### Finding 2: Tenant-Based Filtering from URL Parameter
**Severity:** 🔴 **CRITICAL**

**Issue:**
```java
private String extractTenantId(WebSocketSession session) {
    try {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("tenantId=")) {
            // ❌ No validation that user belongs to this tenant
            return param.substring("tenantId=".length());
        }
    }
}
```

**Problems:**
- Tenant ID extracted from URL query parameter
- No validation that authenticated user belongs to tenant
- User can specify any tenant ID and receive that tenant's data
- Multi-tenant isolation completely bypassed

**Attack Vector:**
```javascript
// Attacker can access any tenant's data
const ws = new WebSocket('ws://server/ws/health-scores?tenantId=VICTIM_TENANT');
// Will receive all health score updates for victim tenant
```

#### Finding 3: Security Filter Chain Disabled
**Severity:** 🔴 **CRITICAL**

**Issue:**
```java
// From QualityMeasureSecurityConfig.java line 85-86
.authorizeHttpRequests(auth -> auth
    .anyRequest().permitAll()  // ❌ TEMPORARY DEBUG: Permit ALL requests
)
// TEMPORARILY DISABLED FOR DEBUGGING
//.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

**Problems:**
- JWT authentication filter is commented out
- All requests permitted without authentication
- WebSocket endpoints not protected by Spring Security
- Authentication completely disabled in non-test profiles

### 2.2 Data Security & Encryption

#### Finding 4: Unencrypted WebSocket (WS vs WSS)
**Severity:** 🔴 **CRITICAL - HIPAA VIOLATION**

**Issue:**
```java
// From documentation and code comments
// ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001
```

**Problems:**
- All documentation and examples use `ws://` (unencrypted)
- No WSS (WebSocket Secure) configuration
- PHI transmitted in clear text over network
- Man-in-the-middle attacks possible
- HIPAA requires encryption in transit for PHI

**HIPAA Requirement:**
> § 164.312(e)(1) - Transmission Security: Implement technical security measures to guard against unauthorized access to electronic protected health information that is being transmitted over an electronic communications network.

#### Finding 5: PHI Transmitted Without Additional Encryption
**Severity:** 🔴 **CRITICAL - HIPAA VIOLATION**

**PHI Data Transmitted:**
```java
// From ClinicalAlertDTO.java
private String patientId;           // ✓ Identifiable
private String title;               // ✓ May contain patient info
private String message;             // ✓ Contains clinical information
private String sourceEventType;     // ✓ Health status
private String alertType;           // ✓ "MENTAL_HEALTH_CRISIS" - diagnosis

// From HealthScoreDTO.java
private String patientId;           // ✓ Identifiable
private Double mentalHealthScore;   // ✓ Mental health information
private Double physicalHealthScore; // ✓ Health status
private String changeReason;        // ✓ May contain clinical details
```

**Problems:**
- Patient identifiers transmitted
- Mental health information (extra protected under 42 CFR Part 2)
- Clinical assessment data
- No additional encryption layer beyond transport
- No data masking or tokenization

#### Finding 6: No Audit Logging for WebSocket PHI Access
**Severity:** 🔴 **CRITICAL - HIPAA VIOLATION**

**Issue:**
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // ❌ No audit log of PHI access
    logger.info("Health Score WebSocket connection established: sessionId={}, tenantId={}", 
                sessionId, tenantId);
}

private void broadcastToSessions(Map<String, Object> message, String tenantId) {
    sessions.forEach((sessionId, session) -> {
        try {
            sendMessage(session, message);  // ❌ No audit trail of PHI transmission
        }
    });
}
```

**Problems:**
- No audit logging when PHI is accessed via WebSocket
- Cannot track who accessed what patient data
- Cannot detect unauthorized access
- HIPAA requires comprehensive audit trails

**HIPAA Requirement:**
> § 164.312(b) - Audit controls: Implement hardware, software, and/or procedural mechanisms that record and examine activity in information systems that contain or use electronic protected health information.

### 2.3 HIPAA Compliance Gaps

| Requirement | Status | Finding |
|-------------|--------|---------|
| Access Controls (§164.312(a)(1)) | ❌ FAIL | No authentication required |
| Transmission Security (§164.312(e)) | ❌ FAIL | WS instead of WSS |
| Audit Controls (§164.312(b)) | ❌ FAIL | No PHI access logging |
| Person/Entity Authentication (§164.312(d)) | ❌ FAIL | No user verification |
| Integrity Controls (§164.312(c)) | ⚠️ PARTIAL | No message signing |
| Automatic Logoff (§164.312(a)(2)(iii)) | ❌ FAIL | No session timeout |

### 2.4 WebSocket-Specific Vulnerabilities

#### Finding 7: Cross-Site WebSocket Hijacking (CSWSH)
**Severity:** 🔴 **HIGH**

**Issue:**
```java
registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
        .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
        .addInterceptors(new HttpSessionHandshakeInterceptor());
```

**Problems:**
- No CSRF token validation on WebSocket handshake
- Cookie-based sessions could be hijacked
- Attacker can create malicious page that connects to WebSocket using victim's cookies

**Attack Scenario:**
```html
<!-- Attacker's malicious page -->
<script>
  // Victim visits attacker's site while logged into healthcare app
  const ws = new WebSocket('ws://healthcare-app.com/ws/health-scores?tenantId=TENANT001');
  ws.onmessage = (event) => {
    // Attacker receives victim's patient data
    sendToAttackerServer(event.data);
  };
</script>
```

#### Finding 8: Origin Validation Bypassed
**Severity:** 🟠 **MEDIUM**

**Issue:**
```yaml
# application.yml
websocket:
  enabled: true
  allowed-origins: http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:3000,http://localhost:8082
```

**Problems:**
- Development origins allowed in configuration
- No production-specific origin configuration
- Multiple ports allowed increases attack surface
- Localhost origins should not be in production

#### Finding 9: No Rate Limiting on WebSocket Connections
**Severity:** 🟠 **MEDIUM**

**Issue:**
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.put(sessionId, session);  // ❌ No connection limit
}
```

**Problems:**
- No limit on concurrent connections per user/tenant
- No limit on connection rate
- DoS attack vector: attacker can exhaust server resources
- No backpressure mechanism

**Attack Scenario:**
```javascript
// DoS attack: Open 10,000 connections
for (let i = 0; i < 10000; i++) {
  new WebSocket('ws://server/ws/health-scores?tenantId=TENANT001');
}
```

#### Finding 10: No Session Timeout Configuration
**Severity:** 🟠 **MEDIUM - HIPAA VIOLATION**

**Issue:**
- No maximum WebSocket session duration configured
- Sessions remain open indefinitely until client disconnects
- HIPAA requires automatic logoff after inactivity

**HIPAA Requirement:**
> § 164.312(a)(2)(iii) - Automatic Logoff: Implement electronic procedures that terminate an electronic session after a predetermined time of inactivity.

#### Finding 11: Message Injection Vulnerability
**Severity:** 🟡 **LOW-MEDIUM**

**Issue:**
```java
private void sendMessage(WebSocketSession session, Object message) {
    if (session.isOpen()) {
        try {
            String json = objectMapper.writeValueAsString(message);  // ❌ No sanitization
            session.sendMessage(new TextMessage(json));
        }
    }
}
```

**Problems:**
- No input validation on message content
- Relies solely on Jackson serialization
- Potential for injection if message contains user-controlled data

---

## Part 3: Root Cause Analysis - Bean Injection Failure

### 3.1 The Error

```
Parameter 0 of constructor in com.healthdata.quality.service.notification.WebSocketNotificationChannel 
required a bean of type 'org.springframework.messaging.simp.SimpMessagingTemplate' 
that could not be found.
```

### 3.2 Root Cause

The service has **two incompatible WebSocket implementations**:

#### Implementation 1: Raw WebSocket (Working)
```java
@Configuration
@EnableWebSocket  // ✅ Configured
public class WebSocketConfig implements WebSocketConfigurer {
    // Registers HealthScoreWebSocketHandler
}
```

#### Implementation 2: STOMP over WebSocket (Broken)
```java
@Component
@RequiredArgsConstructor
public class WebSocketNotificationChannel {
    private final SimpMessagingTemplate messagingTemplate;  // ❌ Bean not found
    
    public boolean send(String tenantId, ClinicalAlertDTO alert) {
        messagingTemplate.convertAndSend(destination, alert);  // Requires STOMP
    }
}
```

**Missing Configuration:**
```java
// THIS IS MISSING - Required for SimpMessagingTemplate
@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

### 3.3 Why Both Patterns Exist

1. **HealthScoreWebSocketHandler** - Custom implementation for real-time health scores
   - Uses raw WebSocket for performance
   - Manual session management
   - Custom JSON protocol

2. **WebSocketNotificationChannel** - Alert notification system
   - Uses STOMP for pub/sub pattern
   - Message broker for routing
   - Standard STOMP protocol

### 3.4 Resolution Options

#### Option A: Remove STOMP (Recommended)
- Delete `WebSocketNotificationChannel.java`
- Use only `HealthScoreWebSocketHandler` for all WebSocket needs
- Simplifies architecture
- Better performance

#### Option B: Configure STOMP
- Add `@EnableWebSocketMessageBroker` configuration
- Maintain both implementations
- More complex but supports different use cases

#### Option C: Migrate to STOMP Only
- Remove raw WebSocket handler
- Implement everything with STOMP
- Standard protocol but less flexible

---

## Part 4: Security Vulnerabilities Summary

### Critical Vulnerabilities (🔴)

| ID | Vulnerability | HIPAA Impact | CVE Risk |
|----|---------------|--------------|----------|
| V1 | No authentication on WebSocket | § 164.312(d) | HIGH |
| V2 | Unvalidated tenant isolation | § 164.312(a)(1) | CRITICAL |
| V3 | WS instead of WSS | § 164.312(e)(1) | CRITICAL |
| V4 | PHI transmitted unencrypted | § 164.312(e)(1) | CRITICAL |
| V5 | No audit logging for PHI | § 164.312(b) | HIGH |
| V6 | Security filters disabled | § 164.312(a)(1) | CRITICAL |
| V7 | CSWSH vulnerability | N/A | HIGH |

### High Vulnerabilities (🟠)

| ID | Vulnerability | Impact | CVE Risk |
|----|---------------|--------|----------|
| V8 | No session timeout | § 164.312(a)(2)(iii) | MEDIUM |
| V9 | No rate limiting | N/A | MEDIUM |
| V10 | Origin validation issues | N/A | MEDIUM |

### Medium Vulnerabilities (🟡)

| ID | Vulnerability | Impact | CVE Risk |
|----|---------------|--------|----------|
| V11 | Message injection | N/A | LOW |

---

## Part 5: Recommended Architecture

### 5.1 Recommended Pattern: Raw WebSocket with Security

**Reasoning:**
- Better performance for real-time health scores
- Simpler architecture
- Easier to secure
- More control over messages

### 5.2 Security Requirements

```java
@Configuration
@EnableWebSocket
public class SecureWebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
                .setAllowedOrigins(getAllowedOrigins())  // Production origins only
                .addInterceptors(
                    new JwtHandshakeInterceptor(),       // ✅ JWT validation
                    new TenantAccessInterceptor(),       // ✅ Tenant authorization
                    new AuditLoggingInterceptor()        // ✅ PHI access logging
                );
    }
}
```

### 5.3 Required Security Components

#### Component 1: JWT Handshake Interceptor
```java
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler, 
                                    Map<String, Object> attributes) {
        // Extract JWT from query param or header
        String token = extractToken(request);
        
        // Validate JWT
        if (!jwtTokenService.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;  // Reject handshake
        }
        
        // Store user info in session attributes
        String username = jwtTokenService.extractUsername(token);
        Set<String> tenantIds = jwtTokenService.extractTenantIds(token);
        
        attributes.put("username", username);
        attributes.put("tenantIds", tenantIds);
        
        return true;  // Allow handshake
    }
}
```

#### Component 2: Tenant Access Interceptor
```java
public class TenantAccessInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(...) {
        String requestedTenant = extractTenantFromUrl(request);
        Set<String> userTenants = (Set<String>) attributes.get("tenantIds");
        
        // Verify user has access to requested tenant
        if (!userTenants.contains(requestedTenant)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        
        return true;
    }
}
```

#### Component 3: Audit Logging Interceptor
```java
public class AuditLoggingInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(...) {
        String username = (String) attributes.get("username");
        String tenantId = extractTenantFromUrl(request);
        
        // Log PHI access
        auditService.logPhiAccess(
            username,
            "WEBSOCKET_CONNECT",
            tenantId,
            request.getRemoteAddress()
        );
        
        return true;
    }
}
```

---

## Part 6: Prioritized Security Fixes

### Phase 1: Critical Fixes (Week 1)

#### Fix 1: Resolve Bean Injection Error
**Priority:** P0 - Service won't start

**Action:**
```bash
# Option A: Remove STOMP (Recommended)
rm backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/WebSocketNotificationChannel.java

# Update NotificationService.java to use HealthScoreWebSocketHandler instead
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final HealthScoreWebSocketHandler webSocketHandler;  // Changed from WebSocketNotificationChannel
    
    public void sendNotification(String tenantId, ClinicalAlertDTO alert) {
        // Convert alert to Map
        Map<String, Object> alertData = Map.of(
            "type", "CLINICAL_ALERT",
            "severity", alert.getSeverity(),
            "alert", alert
        );
        
        // Broadcast via existing WebSocket handler
        webSocketHandler.broadcastHealthScoreUpdate(alertData, tenantId);
    }
}
```

#### Fix 2: Enable WSS (WebSocket Secure)
**Priority:** P0 - HIPAA Violation

**Action:**
```yaml
# application-production.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: healthdata

websocket:
  enabled: true
  allowed-origins: https://clinical-portal.healthdata.com
  secure: true  # Force WSS
```

**Nginx Proxy Configuration:**
```nginx
server {
    listen 443 ssl http2;
    server_name api.healthdata.com;
    
    ssl_certificate /etc/ssl/certs/healthdata.crt;
    ssl_certificate_key /etc/ssl/private/healthdata.key;
    
    # WebSocket upgrade
    location /quality-measure/ws/ {
        proxy_pass http://localhost:8087;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        
        # Security headers
        proxy_set_header X-Content-Type-Options nosniff;
        proxy_set_header X-Frame-Options DENY;
        
        # WebSocket timeout
        proxy_read_timeout 900s;  # 15 minutes (HIPAA compliant)
        proxy_send_timeout 900s;
    }
}
```

#### Fix 3: Implement JWT Authentication on WebSocket
**Priority:** P0 - HIPAA Violation

**Implementation:**
```java
@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtTokenService jwtTokenService;
    private final AuditService auditService;
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        
        // Extract JWT token from query parameter
        String token = extractTokenFromQuery(request);
        
        if (token == null || token.isBlank()) {
            logger.warn("WebSocket handshake rejected: No JWT token provided from {}",
                       request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        
        // Validate JWT token
        try {
            if (!jwtTokenService.validateToken(token)) {
                logger.warn("WebSocket handshake rejected: Invalid JWT token");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            // Extract user information
            String username = jwtTokenService.extractUsername(token);
            Set<String> roles = jwtTokenService.extractRoles(token);
            Set<String> tenantIds = jwtTokenService.extractTenantIds(token);
            
            // Validate tenant access
            String requestedTenant = extractTenantFromUrl(request);
            if (requestedTenant != null && !tenantIds.contains(requestedTenant)) {
                logger.warn("WebSocket handshake rejected: User {} does not have access to tenant {}",
                           username, requestedTenant);
                auditService.logUnauthorizedAccess(username, "WEBSOCKET", requestedTenant);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            
            // Store authenticated user info in WebSocket session attributes
            attributes.put("username", username);
            attributes.put("roles", roles);
            attributes.put("tenantIds", tenantIds);
            attributes.put("authenticatedAt", Instant.now());
            
            // Audit log
            auditService.logPhiAccess(
                username,
                "WEBSOCKET_CONNECT",
                requestedTenant,
                request.getRemoteAddress().toString(),
                "WebSocket connection established"
            );
            
            logger.info("WebSocket handshake approved for user: {} (tenant: {})",
                       username, requestedTenant);
            
            return true;
            
        } catch (Exception e) {
            logger.error("WebSocket handshake error: {}", e.getMessage());
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Nothing to do after handshake
    }
    
    private String extractTokenFromQuery(ServerHttpRequest request) {
        try {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        return param.substring(6);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract token from query: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractTenantFromUrl(ServerHttpRequest request) {
        try {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("tenantId=")) {
                        return param.substring(9);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract tenantId from query: {}", e.getMessage());
        }
        return null;
    }
}
```

**Updated WebSocketConfig:**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final HealthScoreWebSocketHandler healthScoreWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${websocket.enabled:true}")
    private boolean websocketEnabled;

    @Value("#{'${websocket.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (websocketEnabled) {
            registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
                    .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                    .addInterceptors(jwtHandshakeInterceptor);  // ✅ JWT validation
        }
    }
}
```

#### Fix 4: Add Audit Logging for PHI Access
**Priority:** P0 - HIPAA Violation

**Implementation:**
```java
@Component
public class HealthScoreWebSocketHandler extends TextWebSocketHandler {
    
    private final AuditService auditService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Get authenticated user from session attributes
        String username = (String) session.getAttributes().get("username");
        String tenantId = extractTenantId(session);
        
        sessions.put(session.getId(), session);
        sessionTenants.put(session.getId(), tenantId);
        
        // Audit log - connection established
        auditService.logPhiAccess(
            username,
            "WEBSOCKET_CONNECT",
            tenantId,
            session.getRemoteAddress().toString(),
            "Real-time health score monitoring started"
        );
        
        logger.info("WebSocket connection established - User: {}, Tenant: {}, Session: {}",
                   username, tenantId, session.getId());
        
        // Send welcome message
        sendMessage(session, createWelcomeMessage(session, tenantId));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        String tenantId = sessionTenants.get(session.getId());
        
        sessions.remove(session.getId());
        sessionTenants.remove(session.getId());
        
        // Audit log - connection closed
        auditService.logPhiAccess(
            username,
            "WEBSOCKET_DISCONNECT",
            tenantId,
            session.getRemoteAddress().toString(),
            "Real-time monitoring ended - Status: " + status
        );
        
        logger.info("WebSocket connection closed - User: {}, Status: {}", username, status);
    }
    
    private void sendMessage(WebSocketSession session, Object message) {
        if (session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                
                // Audit log - PHI transmitted (if message contains patient data)
                if (isPhiMessage(message)) {
                    String username = (String) session.getAttributes().get("username");
                    String tenantId = sessionTenants.get(session.getId());
                    
                    auditService.logPhiAccess(
                        username,
                        "WEBSOCKET_PHI_SENT",
                        tenantId,
                        extractPatientId(message),
                        "Health score update transmitted"
                    );
                }
            } catch (IOException e) {
                logger.error("Failed to send WebSocket message: {}", e.getMessage());
            }
        }
    }
    
    private boolean isPhiMessage(Object message) {
        if (message instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) message;
            String type = (String) map.get("type");
            return "HEALTH_SCORE_UPDATE".equals(type) || 
                   "SIGNIFICANT_CHANGE".equals(type) ||
                   "CLINICAL_ALERT".equals(type);
        }
        return false;
    }
    
    private String extractPatientId(Object message) {
        if (message instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) message;
            Object data = map.get("data");
            if (data instanceof Map) {
                return (String) ((Map<?, ?>) data).get("patientId");
            }
        }
        return "UNKNOWN";
    }
}
```

#### Fix 5: Re-enable Security Filter Chain
**Priority:** P0 - CRITICAL

**Action:**
```java
// QualityMeasureSecurityConfig.java
@Bean
@Profile("!test")
@Order(2)
public SecurityFilterChain securityFilterChain(HttpSecurity http, 
                                               JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            
            // WebSocket endpoints - require authentication
            .requestMatchers("/ws/**").authenticated()
            
            // API endpoints - require authentication
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/patient-health/**").authenticated()
            .requestMatchers("/mental-health/**").authenticated()
            .requestMatchers("/care-gaps/**").authenticated()
            .requestMatchers("/risk-stratification/**").authenticated()
            
            // All other requests require authentication
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);  // ✅ Re-enabled

    return http.build();
}
```

### Phase 2: High Priority Fixes (Week 2)

#### Fix 6: Implement Session Timeout
**Priority:** P1 - HIPAA Compliance

**Implementation:**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Value("${websocket.session.timeout:900000}")  // 15 minutes default
    private long sessionTimeout;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (websocketEnabled) {
            registry.addHandler(
                new SessionTimeoutWebSocketHandler(healthScoreWebSocketHandler, sessionTimeout),
                "/ws/health-scores"
            )
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
            .addInterceptors(jwtHandshakeInterceptor);
        }
    }
}

public class SessionTimeoutWebSocketHandler extends WebSocketHandlerDecorator {
    
    private final long sessionTimeout;
    private final Map<String, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        scheduleTimeout(session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Reset timeout on activity
        resetTimeout(session);
        super.handleTextMessage(session, message);
    }
    
    private void scheduleTimeout(WebSocketSession session) {
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            try {
                logger.info("WebSocket session timeout - closing session: {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Session timeout"));
            } catch (IOException e) {
                logger.error("Error closing timed-out session", e);
            }
        }, sessionTimeout, TimeUnit.MILLISECONDS);
        
        timeoutTasks.put(session.getId(), task);
    }
    
    private void resetTimeout(WebSocketSession session) {
        ScheduledFuture<?> oldTask = timeoutTasks.remove(session.getId());
        if (oldTask != null) {
            oldTask.cancel(false);
        }
        scheduleTimeout(session);
    }
}
```

#### Fix 7: Implement Rate Limiting
**Priority:** P1 - DoS Protection

**Implementation:**
```java
@Component
public class RateLimitingWebSocketInterceptor implements HandshakeInterceptor {
    
    private final LoadingCache<String, AtomicInteger> connectionCounts;
    private final LoadingCache<String, AtomicInteger> connectionRates;
    
    @Value("${websocket.rate-limit.max-connections-per-user:5}")
    private int maxConnectionsPerUser;
    
    @Value("${websocket.rate-limit.max-connections-per-minute:10}")
    private int maxConnectionsPerMinute;
    
    public RateLimitingWebSocketInterceptor() {
        // Cache for tracking concurrent connections per user
        this.connectionCounts = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
        
        // Cache for tracking connection rate per user (sliding window)
        this.connectionRates = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) throws Exception {
        
        String username = (String) attributes.get("username");
        if (username == null) {
            return true;  // Let authentication interceptor handle
        }
        
        // Check concurrent connection limit
        int currentConnections = connectionCounts.get(username).get();
        if (currentConnections >= maxConnectionsPerUser) {
            logger.warn("Rate limit exceeded: User {} has {} concurrent connections (max: {})",
                       username, currentConnections, maxConnectionsPerUser);
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }
        
        // Check connection rate limit
        int connectionsThisMinute = connectionRates.get(username).incrementAndGet();
        if (connectionsThisMinute > maxConnectionsPerMinute) {
            logger.warn("Rate limit exceeded: User {} opened {} connections in last minute (max: {})",
                       username, connectionsThisMinute, maxConnectionsPerMinute);
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }
        
        // Increment connection count
        connectionCounts.get(username).incrementAndGet();
        
        return true;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Decrement connection count on disconnect
        // (Handled in WebSocketHandler's afterConnectionClosed)
    }
}
```

#### Fix 8: Restrict Production Origins
**Priority:** P1 - Security

**Action:**
```yaml
# application-production.yml
websocket:
  enabled: true
  allowed-origins: https://clinical-portal.healthdata.com,https://admin-portal.healthdata.com
  # NO localhost origins in production
```

### Phase 3: Additional Security Hardening (Week 3)

#### Fix 9: Implement CSRF Protection for WebSocket
**Priority:** P2

**Implementation:**
```java
@Component
public class CsrfWebSocketInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) throws Exception {
        
        // Extract CSRF token from query parameter
        String csrfToken = extractCsrfToken(request);
        
        // Get expected CSRF token from session
        String expectedCsrfToken = getExpectedCsrfToken(request);
        
        // Validate CSRF token
        if (csrfToken == null || !csrfToken.equals(expectedCsrfToken)) {
            logger.warn("CSRF token validation failed for WebSocket handshake");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        
        return true;
    }
}
```

#### Fix 10: Add Message Validation & Sanitization
**Priority:** P2

**Implementation:**
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // Validate message size
    if (message.getPayloadLength() > MAX_MESSAGE_SIZE) {
        logger.warn("Message size exceeds limit: {} bytes", message.getPayloadLength());
        session.close(CloseStatus.TOO_BIG);
        return;
    }
    
    // Parse and validate JSON
    try {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        
        // Validate message type
        String messageType = (String) payload.get("type");
        if (!ALLOWED_MESSAGE_TYPES.contains(messageType)) {
            logger.warn("Invalid message type: {}", messageType);
            return;
        }
        
        // Sanitize message content
        sanitizeMessage(payload);
        
        // Process message
        processClientMessage(session, payload);
        
    } catch (JsonProcessingException e) {
        logger.warn("Invalid JSON message received: {}", e.getMessage());
        session.close(CloseStatus.BAD_DATA);
    }
}
```

---

## Part 7: Code Changes Required

### Files to Modify

1. **DELETE:**
   - `WebSocketNotificationChannel.java` (conflicts with raw WebSocket)

2. **MODIFY:**
   - `WebSocketConfig.java` - Add JWT interceptor, rate limiting
   - `HealthScoreWebSocketHandler.java` - Add audit logging, session timeout
   - `NotificationService.java` - Use HealthScoreWebSocketHandler instead
   - `QualityMeasureSecurityConfig.java` - Re-enable security filters
   - `application.yml` - Add WebSocket security settings
   - `application-production.yml` - Configure WSS and production origins

3. **CREATE:**
   - `JwtWebSocketHandshakeInterceptor.java`
   - `RateLimitingWebSocketInterceptor.java`
   - `SessionTimeoutWebSocketHandler.java`
   - `CsrfWebSocketInterceptor.java` (optional)

### Configuration Changes

```yaml
# application-production.yml
websocket:
  enabled: true
  allowed-origins: https://clinical-portal.healthdata.com
  secure: true
  session:
    timeout: 900000  # 15 minutes (HIPAA compliant)
  rate-limit:
    max-connections-per-user: 3
    max-connections-per-minute: 10
```

---

## Part 8: Testing Recommendations

### Security Testing Checklist

- [ ] Verify JWT token required for WebSocket connection
- [ ] Test invalid/expired token rejection
- [ ] Verify tenant isolation (user cannot access other tenant's data)
- [ ] Test session timeout after 15 minutes of inactivity
- [ ] Verify rate limiting (max connections per user)
- [ ] Test CSRF protection
- [ ] Verify WSS (encrypted) in production
- [ ] Check audit logs for all PHI access
- [ ] Test origin validation (reject unauthorized origins)
- [ ] Penetration testing for CSWSH vulnerability

### HIPAA Compliance Testing

- [ ] Access Controls: Only authenticated users can connect
- [ ] Transmission Security: All data sent over WSS
- [ ] Audit Controls: All PHI access logged with user, time, patient ID
- [ ] Automatic Logoff: Sessions terminated after 15 minutes
- [ ] Person Authentication: JWT validates user identity
- [ ] Integrity Controls: Message tampering detection

---

## Part 9: Summary & Recommendations

### Immediate Actions (This Week)

1. ✅ **Remove WebSocketNotificationChannel.java** to fix startup error
2. ✅ **Implement JWT authentication** on WebSocket handshake
3. ✅ **Add audit logging** for all PHI access via WebSocket
4. ✅ **Enable WSS** (WebSocket Secure) in production
5. ✅ **Re-enable security filter chain** in QualityMeasureSecurityConfig

### Short-Term Actions (2-4 Weeks)

1. ✅ Implement session timeout (15 minutes HIPAA compliant)
2. ✅ Add rate limiting for WebSocket connections
3. ✅ Restrict origins to production domains only
4. ✅ Add CSRF protection for WebSocket handshake
5. ✅ Implement message validation and sanitization

### Long-Term Actions (1-3 Months)

1. Conduct security penetration testing
2. Implement end-to-end encryption for ultra-sensitive data
3. Add intrusion detection for abnormal WebSocket patterns
4. Implement message replay protection
5. Add WebSocket connection monitoring dashboard

### HIPAA Compliance Roadmap

| Week | Action | HIPAA § | Status |
|------|--------|---------|--------|
| 1 | JWT Authentication | 164.312(d) | 🔴 Critical |
| 1 | Enable WSS | 164.312(e)(1) | 🔴 Critical |
| 1 | Audit Logging | 164.312(b) | 🔴 Critical |
| 2 | Session Timeout | 164.312(a)(2)(iii) | 🟠 High |
| 2 | Re-enable Security Filters | 164.312(a)(1) | 🔴 Critical |
| 3 | Rate Limiting | N/A | 🟠 Medium |
| 3 | CSRF Protection | N/A | 🟡 Low |
| 4 | Security Audit | 164.308(a)(8) | 🟠 High |

---

## Part 10: Conclusion

The WebSocket implementation has **critical security vulnerabilities** that make it **non-compliant with HIPAA** and pose significant risks for unauthorized PHI access. The most urgent issue is the **bean injection failure** preventing service startup, caused by incompatible WebSocket implementations.

**Recommended Immediate Action:**
1. Remove STOMP implementation (WebSocketNotificationChannel.java)
2. Implement JWT authentication on WebSocket handshake
3. Enable WSS (WebSocket Secure) in production
4. Add comprehensive audit logging for PHI access
5. Re-enable security filter chain

**Estimated Implementation Time:**
- Phase 1 (Critical): 3-5 days
- Phase 2 (High): 5-7 days
- Phase 3 (Medium): 3-5 days
- **Total:** 2-3 weeks for full HIPAA compliance

**Risk Assessment:**
- **Current Risk Level:** 🔴 **CRITICAL**
- **Post-Phase 1 Risk Level:** 🟠 **MEDIUM**
- **Post-All-Phases Risk Level:** 🟢 **LOW**

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-26  
**Next Review:** After Phase 1 completion
