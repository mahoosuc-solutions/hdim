# Quality Measure Service - Integration Tests

## Overview

This directory contains comprehensive integration tests for the Quality Measure Service. These tests validate the service's API endpoints, business logic, external service integrations, and data persistence.

## Test Coverage

### Test Files (11 files, 134 test methods)

1. **MeasureCalculationApiIntegrationTest.java** (10 tests)
   - Tests the `/quality-measure/calculate` endpoint
   - Validates measure calculations via CQL Engine
   - Tests different measure categories (HEDIS, CMS, custom)
   - Verifies error handling and data persistence
   - Tests request validation and parameter handling

2. **ResultsApiIntegrationTest.java** (11 tests)
   - Tests the `/quality-measure/results` endpoint
   - Validates patient result retrieval
   - Tests multi-tenant isolation
   - Verifies filtering by patient and tenant
   - Tests empty results and different compliance statuses

3. **QualityScoreApiIntegrationTest.java** (13 tests)
   - Tests the `/quality-measure/score` endpoint
   - Validates quality score calculations
   - Tests various compliance percentages
   - Verifies precision and edge cases
   - Tests tenant and patient isolation

4. **PatientReportApiIntegrationTest.java** (12 tests)
   - Tests the `/quality-measure/report/patient` endpoint
   - Validates comprehensive patient reports
   - Tests measure grouping by category
   - Verifies care gap integration
   - Tests error handling for external services

5. **PopulationReportApiIntegrationTest.java** (13 tests)
   - Tests the `/quality-measure/report/population` endpoint
   - Validates population-level quality reporting
   - Tests year-based filtering
   - Verifies unique patient counting
   - Tests large population handling

6. **CqlEngineIntegrationTest.java** (14 tests)
   - Tests integration with CQL Engine Service
   - Validates CQL request/response handling
   - Tests error scenarios (timeouts, malformed responses)
   - Verifies measure name extraction
   - Tests tenant ID propagation

7. **CachingBehaviorIntegrationTest.java** (9 tests)
   - Tests Redis caching functionality
   - Validates cache isolation by tenant/patient
   - Tests cache hit/miss scenarios
   - Verifies different cache keys
   - Tests cache graceful degradation

8. **MultiTenantIsolationIntegrationTest.java** (11 tests)
   - Tests data isolation between tenants
   - Validates tenant-specific queries
   - Tests cross-tenant access prevention
   - Verifies tenant ID enforcement
   - Tests complex multi-tenant scenarios

9. **HealthEndpointIntegrationTest.java** (8 tests)
   - Tests the `/quality-measure/_health` endpoint
   - Validates health check responses
   - Tests availability and performance
   - Verifies public access without authentication

10. **DtoMappingIntegrationTest.java** (12 tests)
    - Tests DTO-Entity mapping functionality
    - Validates bidirectional mapping
    - Tests null handling and data type preservation
    - Verifies field exclusions (e.g., cqlResult)
    - Tests list mapping operations

11. **ErrorHandlingIntegrationTest.java** (21 tests)
    - Tests error handling across all endpoints
    - Validates input validation
    - Tests edge cases and boundary conditions
    - Verifies graceful degradation
    - Tests concurrent request handling

## Test Categories

### API Endpoint Tests
- Measure calculation (`/calculate`)
- Results retrieval (`/results`)
- Quality score (`/score`)
- Patient reports (`/report/patient`)
- Population reports (`/report/population`)
- Health check (`/_health`)

### Integration Tests
- CQL Engine Service integration
- Care Gap Service integration (mocked)
- FHIR server integration (mocked)
- Kafka event publishing (mocked)

### Data Layer Tests
- Database persistence
- Repository queries
- Transaction management
- DTO mapping

### Cross-Cutting Concerns
- Multi-tenant isolation
- Redis caching
- Error handling
- Input validation
- Concurrent request handling

## Running the Tests

### Run all integration tests:
```bash
./gradlew test --tests "*Integration*"
```

### Run specific test class:
```bash
./gradlew test --tests "MeasureCalculationApiIntegrationTest"
```

### Run with coverage:
```bash
./gradlew test jacocoTestReport
```

## Test Configuration

Tests use a separate configuration file: `src/test/resources/application-test.yml`

Key test configurations:
- H2 in-memory database (PostgreSQL mode)
- Simple cache (instead of Redis)
- Mocked external services (CQL Engine, Care Gap, Patient Service)
- Disabled Liquibase migrations
- Random test port

## Test Patterns

### Test Structure
All tests follow the AAA pattern:
1. **Arrange**: Setup test data and mocks
2. **Act**: Execute the operation
3. **Assert**: Verify the results

### Mocking Strategy
- External services are mocked using `@MockBean`
- Database operations use real H2 database
- Tests are transactional (rollback after each test)

### Test Data
- Helper methods create test data consistently
- UUIDs are used for realistic IDs
- Tests use descriptive measure IDs for clarity

## Coverage Goals

- **Current Target**: 40%+ overall coverage
- **Ultimate Goal**: 80% coverage
- **Focus Areas**:
  - All API endpoints (100%)
  - Service layer business logic (80%+)
  - Error handling paths (70%+)
  - DTO mapping (100%)

## Key Test Scenarios

### Success Scenarios
- Valid measure calculations
- Successful data retrieval
- Proper caching behavior
- Correct tenant isolation

### Error Scenarios
- Missing required parameters
- Invalid input formats
- External service failures
- Database errors
- Concurrent access

### Edge Cases
- Empty result sets
- Null values
- Special characters in inputs
- Large data sets
- Boundary values (years, scores)

## Best Practices

1. **Isolation**: Each test is independent and can run in any order
2. **Cleanup**: Tests use `@BeforeEach` to clear data
3. **Transactions**: Tests are `@Transactional` for automatic rollback
4. **Descriptive Names**: Test names clearly describe what is being tested
5. **Mock Management**: Mocks are reset between tests
6. **Assertions**: Multiple assertions verify complete behavior
7. **Documentation**: Tests include `@DisplayName` for clarity

## Continuous Integration

These tests are designed to run in CI/CD pipelines:
- No external dependencies required
- Fast execution (< 2 minutes for full suite)
- Deterministic results
- Detailed failure reporting

## Maintenance

When adding new features:
1. Add corresponding integration tests
2. Follow existing test patterns
3. Update this README with new coverage
4. Ensure tests are independent
5. Use appropriate mocking strategy

## Troubleshooting

### Common Issues

**Tests fail with "Connection refused"**
- Check that mocked services are properly configured
- Verify `@MockBean` annotations are present

**Tests intermittently fail**
- Check for test isolation issues
- Verify `@BeforeEach` cleanup is working
- Look for shared state between tests

**Slow test execution**
- Check for unnecessary database operations
- Verify caching is configured correctly
- Consider using test slices for unit tests

## Additional Resources

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
