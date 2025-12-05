# Payer Workflows Service

Comprehensive payer-specific workflows service for Medicare Advantage Star Ratings and Medicaid compliance reporting.

## Overview

This microservice provides specialized functionality for health insurance payers including:

- **Medicare Advantage Star Ratings**: CMS 2024 methodology implementation for calculating and reporting Star Ratings (1-5 stars)
- **Medicaid State Compliance**: State-specific quality measure compliance tracking and reporting
- **Payer Dashboards**: Aggregated metrics and analytics for plan performance
- **Improvement Opportunities**: Data-driven recommendations for quality improvement

## Key Features

### Medicare Advantage Star Ratings

- ✅ 50+ HEDIS measures mapped to CMS Star Rating categories
- ✅ 6 Star Rating domains (Staying Healthy, Managing Chronic Conditions, etc.)
- ✅ CMS 2024 cut point methodology
- ✅ Weighted domain and overall calculations
- ✅ Quality Bonus Payment (QBP) eligibility determination
- ✅ Year-over-year improvement tracking
- ✅ ROI-based improvement opportunity identification

### Medicaid Compliance

- ✅ State-specific configurations (NY, CA, TX, FL, and more)
- ✅ Quality threshold and goal tracking
- ✅ Compliance status determination (Compliant, Substantially Compliant, Partially Compliant, Non-Compliant)
- ✅ Penalty and bonus calculations
- ✅ Corrective action identification
- ✅ NCQA accreditation tracking

### Payer Dashboards

- ✅ Medicare Advantage plan-level metrics
- ✅ Medicaid MCO performance metrics
- ✅ Top performing and underperforming measures
- ✅ Financial impact projections
- ✅ Multi-line payer support

## API Endpoints

### Medicare Advantage Star Ratings

```
GET  /api/v1/payer/medicare/star-rating/{planId}
     - Get complete Star Rating report for a plan

GET  /api/v1/payer/medicare/star-rating/{planId}/measures
     - Get measure-level breakdown

GET  /api/v1/payer/medicare/star-rating/{planId}/improvement
     - Get improvement opportunities ranked by ROI

POST /api/v1/payer/medicare/star-rating/calculate
     - Calculate Star Rating from provided measure data
```

### Medicaid Compliance

```
GET  /api/v1/payer/medicaid/{state}/compliance?mcoId={mcoId}
     - Get state-specific compliance report

POST /api/v1/payer/medicaid/compliance/calculate
     - Calculate compliance from provided data
```

### Payer Dashboards

```
GET /api/v1/payer/dashboard/overview?payerId={payerId}
    - Get comprehensive dashboard overview

GET /api/v1/payer/dashboard/medicare?payerId={payerId}
    - Get Medicare-specific metrics

GET /api/v1/payer/dashboard/medicaid?payerId={payerId}
    - Get Medicaid-specific metrics

GET /api/v1/payer/dashboard/financial?payerId={payerId}
    - Get financial impact summary
```

## Domain Models

### Star Rating Models

- **StarRatingMeasure**: Enum of 50+ HEDIS measures affecting Star Ratings
- **StarRatingDomain**: 6 CMS domains for measure grouping
- **MeasureScore**: Individual measure performance and star rating
- **DomainScore**: Aggregated domain-level scores
- **StarRatingReport**: Comprehensive plan-level report
- **ImprovementOpportunity**: Actionable improvement recommendations

### Medicaid Models

- **MedicaidStateConfig**: State-specific requirements and thresholds
- **MedicaidComplianceReport**: MCO compliance status and metrics
- **MedicaidMeasureResult**: Individual measure compliance details
- **PenaltyAssessment**: Financial penalty calculations

### Dashboard Models

- **PayerDashboardMetrics**: Aggregated payer-level metrics
- **MedicareAdvantageMetrics**: MA plan performance summary
- **MedicaidMcoMetrics**: MCO performance summary
- **FinancialImpactSummary**: Revenue impact projections

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **Kafka**: Event streaming
- **OpenAPI/Swagger**: API documentation
- **JUnit 5**: Testing framework

## Test Coverage

The service includes comprehensive TDD tests:

- **StarRatingCalculatorTest**: 27 tests covering measure scoring, domain calculations, overall ratings, and improvement opportunities
- **MedicaidComplianceServiceTest**: 16 tests for state-specific compliance, thresholds, status determination, and penalties
- **PayerDashboardServiceTest**: 11 tests for dashboard aggregation and metrics
- **PayerWorkflowsControllerTest**: 15 tests for REST API endpoints

**Total: 69 comprehensive tests** ensuring production-ready quality.

## Configuration

### Application Properties

```yaml
payer:
  star-ratings:
    cut-point-year: 2024
    enable-improvement-tracking: true
    cache-ttl-minutes: 60

  medicaid:
    supported-states: NY,CA,TX,FL,PA,OH,MI,GA,NC,NJ
    compliance-cache-ttl-minutes: 30

  dashboard:
    metrics-refresh-minutes: 15
    enable-financial-projections: true
```

### Supported States

- New York (NY) - New York Medicaid Managed Care
- California (CA) - Medi-Cal Managed Care
- Texas (TX) - Texas STAR Medicaid
- Florida (FL) - Florida Medicaid Managed Care
- Additional states easily configurable

## Integration

### Quality Measure Service Integration

This service integrates with the `quality-measure-service` to:
- Retrieve HEDIS measure calculations
- Map quality data to Star Rating measures
- Aggregate performance across plans

### Event-Driven Architecture

Publishes events for:
- Star Rating calculations completed
- Compliance status changes
- Quality improvement opportunities identified

## Running the Service

### Local Development

```bash
./gradlew :modules:services:payer-workflows-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:payer-workflows-service:test
```

### Building

```bash
./gradlew :modules:services:payer-workflows-service:build
```

## API Documentation

Swagger UI available at:
```
http://localhost:8089/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8089/api-docs
```

## Star Rating Methodology

### CMS 2024 Star Rating Calculation

1. **Measure Scoring**: Each HEDIS measure scored 1-5 stars based on CMS cut points
2. **Domain Aggregation**: Measures grouped into 6 domains with weighted averaging
3. **Overall Rating**: Domain scores weighted to produce overall plan rating
4. **Rounding**: Final rating rounded to nearest half-star

### Quality Bonus Payment (QBP)

- **4+ stars**: 3% bonus payment
- **5 stars**: 5% bonus payment
- Significant revenue impact for high-performing plans

### Improvement Opportunities

- **High Priority**: High-weight measures close to next threshold
- **ROI Scoring**: Impact divided by effort
- **Patient Needs**: Specific number of patients to improve

## Medicaid Compliance Methodology

### Compliance Status Levels

- **Compliant**: 100% of measures meet state thresholds
- **Substantially Compliant**: >80% of measures meet thresholds
- **Partially Compliant**: 50-80% of measures meet thresholds
- **Non-Compliant**: <50% of measures meet thresholds

### Financial Implications

- **Penalties**: Applied for non-compliant and partially compliant MCOs
- **Bonuses**: Available for MCOs exceeding state goals
- **Corrective Action**: Required for measures significantly below threshold

## Future Enhancements

- [ ] Machine learning for Star Rating predictions
- [ ] Provider-level attribution and scoring
- [ ] Real-time quality gap alerts
- [ ] Additional state Medicaid configurations
- [ ] Commercial plan quality tracking
- [ ] Advanced financial modeling
- [ ] Integration with CMS data submission

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
