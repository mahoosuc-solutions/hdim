# Dependency Management Standards

All dependency versions are managed in `backend/gradle/libs.versions.toml`.

## Usage

✅ **DO** use version catalog:
```kotlin
dependencies {
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bundles.resilience4j.common)
}
```

❌ **DO NOT** hardcode versions:
```kotlin
// WRONG
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
```

## Current Versions

| Library | Version | Notes |
|---------|---------|-------|
| Spring Boot | 3.3.6 | LTS |
| Spring Cloud | 2023.0.6 | Compatible with Boot 3.3.x |
| Jackson | 2.20.1 | Latest stable |
| Resilience4j | 2.2.0 | Latest stable |
| springdoc-openapi | 2.6.0 | OpenAPI 3.1 support |
| PostgreSQL | 42.7.7 | Latest JDBC driver |
| JJWT | 0.12.6 | JWT tokens |

## Validation

Run before committing:
```bash
cd backend
./scripts/validate-dependency-versions.sh
```

Pre-commit hook enforces compliance automatically.

## Resilience4j Bundles

Use bundles for common dependency groups:

```kotlin
// For services with synchronous operations
implementation(libs.bundles.resilience4j.common)
// Includes: spring-boot3, circuitbreaker, retry, ratelimiter

// For services with reactive operations
implementation(libs.bundles.resilience4j.reactive)
// Includes: spring-boot3, circuitbreaker, retry, reactor
```

## Adding New Dependencies

1. Add version to `libs.versions.toml`:
```toml
[versions]
my-library = "1.0.0"
```

2. Add library reference:
```toml
[libraries]
my-library = { module = "com.example:my-library", version.ref = "my-library" }
```

3. Use in service build file:
```kotlin
implementation(libs.my.library)
```

## Updating Versions

1. Update version in `backend/gradle/libs.versions.toml`
2. Run validation: `./scripts/validate-dependency-versions.sh`
3. Test all services: `./gradlew clean build test`
4. Commit with descriptive message: `refactor(deps): Update library-name to x.y.z`

## Troubleshooting

**Pre-commit hook rejects my commit:**
- Check if you have hardcoded versions: `grep -r 'implementation(".*:[0-9]' modules/services/YOUR-SERVICE/build.gradle.kts`
- Replace with catalog reference: `implementation(libs.library.name)`

**Service fails to resolve dependency:**
- Verify library exists in `libs.versions.toml`
- Check spelling: `libs.my.library` (dots replace hyphens)
- Run `./gradlew dependencies` to inspect resolution

**Version conflict:**
- Check `./gradlew :modules:services:YOUR-SERVICE:dependencies --configuration runtimeClasspath`
- Look for version conflicts and add explicit resolution to catalog if needed
