# ADR-0009: Spring Boot 3.x for Backend Services

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Backend Engineering Team
**Technical Story**: Need enterprise Java framework for healthcare microservices

---

## Context and Problem Statement

HDIM requires a backend framework for 28 microservices that provides:

- Enterprise-grade security (OAuth2, JWT, RBAC)
- Production-ready features (health checks, metrics, distributed tracing)
- FHIR integration (HAPI FHIR is Java-based)
- Database abstraction (JPA/Hibernate)
- Event-driven architecture support (Kafka integration)
- Testability (unit, integration, contract testing)
- Observability (logging, metrics, tracing)

---

## Decision Drivers

* **Healthcare ecosystem alignment** - HAPI FHIR, CQL Engine are Java libraries
* **Enterprise security** - Spring Security is industry standard for Java
* **Production readiness** - Spring Boot Actuator provides health/metrics out of box
* **Developer productivity** - Convention over configuration reduces boilerplate
* **Testing ecosystem** - JUnit, Mockito, TestContainers integration
* **Team expertise** - Java/Spring skills widely available
* **Long-term support** - Spring Boot 3.x has multi-year LTS

---

## Considered Options

1. **Spring Boot 3.x** - Enterprise Java framework
2. **Quarkus** - Cloud-native Java framework
3. **Micronaut** - Lightweight Java framework
4. **Node.js (NestJS)** - JavaScript/TypeScript framework
5. **Go (Gin/Echo)** - Go-based microservices

---

## Decision Outcome

**Chosen option**: "Spring Boot 3.x"

**Rationale**: Spring Boot 3.x provides:
- Native integration with HAPI FHIR (both Spring-based)
- Industry-standard security model (Spring Security)
- Mature ecosystem for healthcare workloads
- Excellent observability (Actuator, Micrometer)
- Strong testing support (Spring Test, TestContainers)
- Java 21 LTS support for modern language features
- Active community and long-term support

---

## Consequences

### Positive

* **HAPI FHIR integration**: Seamless integration with Spring Boot starters
* **Security**: Spring Security provides comprehensive OAuth2/JWT support
* **Observability**: Actuator endpoints for health, metrics, info
* **Database**: Spring Data JPA simplifies PostgreSQL integration
* **Messaging**: Spring Kafka for event streaming
* **Testing**: Rich testing ecosystem (Spring Test, MockMvc, TestContainers)
* **Documentation**: Extensive documentation and community resources

### Negative

* **Memory footprint**: JVM-based, higher memory than Go/Rust
* **Startup time**: Slower cold start than native compiled options
* **Complexity**: Spring's magic can obscure behavior
* **Learning curve**: Spring ecosystem is large and complex

**Mitigations**:
- Use GraalVM native image for startup-critical services (optional)
- Document Spring patterns in service READMEs
- Provide team training on Spring Boot best practices

### Neutral

* Requires Java 17+ (we use Java 21 LTS)
* Gradle build system aligns with Spring Boot recommendations

---

## Pros and Cons of Options

### Option 1: Spring Boot 3.x

Enterprise Java framework from VMware/Pivotal.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Integration | **Good** - Native Spring Boot starters |
| Security | **Good** - Spring Security is industry standard |
| Observability | **Good** - Actuator provides comprehensive metrics |
| Database | **Good** - Spring Data JPA for PostgreSQL |
| Messaging | **Good** - Spring Kafka integration |
| Healthcare Adoption | **Good** - Dominant in healthcare Java |
| Performance | **Neutral** - JVM overhead, but optimized |

**Summary**: Best fit for healthcare Java microservices with HAPI FHIR.

---

### Option 2: Quarkus

Cloud-native Java framework from Red Hat.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Integration | **Neutral** - Requires manual integration |
| Security | **Good** - Quarkus Security available |
| Startup Time | **Good** - Native compilation option |
| Memory | **Good** - Lower than Spring Boot |
| Healthcare Adoption | **Neutral** - Less common in healthcare |
| Ecosystem Size | **Neutral** - Smaller than Spring |

**Summary**: Good for cloud-native but HAPI FHIR integration less mature.

---

### Option 3: Micronaut

Lightweight Java framework.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Integration | **Neutral** - Requires manual integration |
| Security | **Good** - Micronaut Security available |
| Startup Time | **Good** - AOT compilation |
| Memory | **Good** - Lower than Spring Boot |
| Healthcare Adoption | **Neutral** - Limited healthcare deployments |
| Documentation | **Neutral** - Less extensive than Spring |

**Summary**: Lightweight alternative but smaller ecosystem.

---

### Option 4: Node.js (NestJS)

JavaScript/TypeScript framework.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Integration | **Bad** - HAPI FHIR is Java only |
| Security | **Neutral** - Passport.js available |
| Performance | **Neutral** - Good for I/O, less for compute |
| Healthcare Adoption | **Bad** - Rare in healthcare backend |
| CQL Engine | **Bad** - CQL Engine is Java only |

**Summary**: Not viable due to Java library dependencies (HAPI FHIR, CQL).

---

### Option 5: Go (Gin/Echo)

Go-based microservices.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Integration | **Bad** - HAPI FHIR is Java only |
| Performance | **Good** - Excellent performance and memory |
| Startup Time | **Good** - Fast startup |
| Healthcare Adoption | **Neutral** - Growing but Java dominant |
| CQL Engine | **Bad** - CQL Engine is Java only |

**Summary**: Not viable due to Java library dependencies.

---

## Implementation Notes

### Version Selected

**Spring Boot 3.2.x** with **Java 21 LTS**

### Key Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // HAPI FHIR
    implementation("ca.uhn.hapi.fhir:hapi-fhir-spring-boot-starter:7.0.2")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
}
```

### Standard Service Structure

```
com.healthdata.{service}/
├── api/
│   └── v1/                  # REST controllers
├── application/             # Application services
├── domain/
│   ├── model/              # Domain entities
│   └── repository/         # Repository interfaces
├── infrastructure/
│   ├── persistence/        # JPA implementations
│   ├── messaging/          # Kafka producers/consumers
│   └── external/           # External service clients
└── config/                 # Spring configuration
```

### Standard Configuration

```yaml
# application.yml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${SERVICE_NAME:service}

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/${POSTGRES_DB:healthdata_qm}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false  # Performance best practice

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

### Security Configuration Pattern

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .addFilterBefore(trustedHeaderAuthFilter,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter,
                TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

### Testing Pattern

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{id}", "123")
                .header("X-Tenant-ID", "tenant1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"));
    }
}
```

### Build Commands

```bash
# Build all services
./gradlew build

# Build specific service
./gradlew :modules:services:quality-measure-service:build

# Run tests
./gradlew test

# Create bootable JAR
./gradlew bootJar

# Run with profile
java -jar service.jar --spring.profiles.active=prod
```

---

## Links

* [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
* [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
* [HAPI FHIR Spring Boot Starter](https://hapifhir.io/hapi-fhir/docs/server_jpa/spring_boot.html)
* [Project CLAUDE.md](/CLAUDE.md)
* Related: [ADR-0005 - HAPI FHIR Selection](ADR-0005-hapi-fhir-selection.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added code examples, build commands |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
