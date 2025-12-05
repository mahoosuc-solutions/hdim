# EHR Connector Service

Production-ready Epic FHIR R4 connector for the HDIM backend platform.

## Overview

The EHR Connector Service provides standardized integration with Epic EHR systems using FHIR R4 APIs. It implements Epic's Backend Services JWT-based authentication (RS384) and handles Epic-specific FHIR extensions.

## Features

### Epic FHIR Connector
- **Patient Operations**
  - Search by MRN (Medical Record Number)
  - Search by name and date of birth
  - Retrieve patient demographics
  
- **Clinical Data Retrieval**
  - Encounters (inpatient, outpatient, emergency)
  - Observations/Lab Results (with category filtering)
  - Conditions (diagnoses, problem list)
  - Medication Requests (prescriptions)
  - Allergy Intolerances

### Authentication
- **Epic Backend Services OAuth2**
  - RS384 JWT assertion creation
  - Token caching (50-minute default)
  - Automatic token refresh
  - Rate limit handling (429 responses)

### Epic-Specific Features
- Epic App Orchard integration support
- MyChart patient access token handling
- Epic department/location mapping
- Epic FHIR extensions (epic-xxxx) handling
  - Legal sex extension
  - Patient class extension
  - Department extension
  - Ordering provider extension
  - Problem list status extension

## Architecture

```
src/main/java/com/healthdata/ehr/connector/
├── core/                           # Core framework interfaces
│   ├── EhrConnector.java          # Main connector interface
│   ├── AuthProvider.java          # Authentication provider interface
│   ├── DataMapper.java            # Data mapping interface
│   └── EhrConnectionException.java # Custom exception
│
├── epic/                          # Epic-specific implementation
│   ├── EpicFhirConnector.java    # Epic FHIR R4 connector
│   ├── EpicAuthProvider.java     # Epic OAuth2/JWT auth
│   ├── EpicDataMapper.java       # Epic extension mapper
│   ├── EpicConnectionConfig.java # Epic configuration
│   ├── EpicTokenResponse.java    # Token response model
│   └── EpicErrorResponse.java    # Error response model
│
└── config/                        # Spring configuration
    └── FhirClientConfig.java     # FHIR client setup
```

## Configuration

### Environment Variables

```yaml
# Epic FHIR Server
EPIC_BASE_URL=https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
EPIC_TOKEN_URL=https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token

# Epic Credentials
EPIC_CLIENT_ID=your-client-id
EPIC_PRIVATE_KEY_PATH=/path/to/privatekey.pem

# Optional Settings
EPIC_SANDBOX_MODE=false
EPIC_USE_APP_ORCHARD=false
EPIC_MYCHART_ENABLED=false
```

### Application Configuration

```yaml
epic:
  base-url: https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
  token-url: https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token
  client-id: your-client-id
  private-key-path: /path/to/privatekey.pem
  sandbox-mode: false
  token-cache-duration-minutes: 50
  max-retries: 3
  request-timeout-seconds: 30
  rate-limit-per-second: 10
```

## Private Key Setup

### Generate RSA Key Pair (for testing)

```bash
# Generate private key
openssl genrsa -out privatekey.pem 2048

# Extract public key
openssl rsa -in privatekey.pem -pubout -out publickey.pem
```

### For Epic Production
1. Register your application in Epic App Orchard
2. Generate RSA key pair (2048 or 4096 bit)
3. Upload public key to Epic
4. Store private key securely
5. Configure `EPIC_PRIVATE_KEY_PATH`

## Testing

### Test Coverage

Total: **53 comprehensive tests** (exceeds 45+ requirement)

- **EpicAuthProviderTest**: 13 tests
  - JWT assertion creation
  - Token exchange
  - Token caching
  - Token refresh on expiry
  - Rate limit retry logic
  - Error handling

- **EpicDataMapperTest**: 16 tests
  - Patient mapping (demographics, MRN, identifiers)
  - Epic extensions mapping
  - Encounter mapping
  - Observation/Lab results mapping
  - Condition mapping
  - Reference ranges and interpretations

- **EpicFhirConnectorTest**: 24 tests
  - Patient search by MRN
  - Patient search by name/DOB
  - Patient retrieval
  - Encounter queries
  - Lab results retrieval
  - Condition queries
  - Medication requests
  - Allergy queries
  - Pagination handling
  - Error handling
  - Retry logic
  - Connection testing

### Running Tests

```bash
# Run all tests
./gradlew :modules:services:ehr-connector-service:test

# Run with coverage
./gradlew :modules:services:ehr-connector-service:test jacocoTestReport

# Run specific test class
./gradlew :modules:services:ehr-connector-service:test --tests EpicFhirConnectorTest
```

## Usage Examples

### Search Patient by MRN

```java
@Autowired
private EpicFhirConnector epicConnector;

List<Patient> patients = epicConnector.searchPatientByMrn("E12345");
```

### Retrieve Lab Results

```java
// Get all observations
List<Observation> observations = epicConnector.getObservations("patient-123", null);

// Get only laboratory results
List<Observation> labResults = epicConnector.getObservations("patient-123", "laboratory");

// Get vital signs
List<Observation> vitals = epicConnector.getObservations("patient-123", "vital-signs");
```

### Handle Epic Extensions

```java
@Autowired
private EpicDataMapper mapper;

Patient patient = epicConnector.getPatient("patient-123").get();
Map<String, Object> mappedData = mapper.mapPatient(patient);

// Access Epic-specific data
String legalSex = (String) mappedData.get("epicLegalSex");
String patientClass = (String) mappedData.get("epicPatientClass");
```

## Rate Limiting

The connector implements automatic retry logic for Epic rate limits (HTTP 429):
- Maximum 3 retry attempts
- Exponential backoff (1s, 2s, 3s)
- Logs rate limit warnings

## Error Handling

```java
try {
    List<Patient> patients = epicConnector.searchPatientByMrn("E12345");
} catch (EhrConnectionException e) {
    logger.error("Epic connection failed: {}", e.getMessage());
    logger.error("EHR System: {}", e.getEhrSystem());
    logger.error("Status Code: {}", e.getStatusCode());
}
```

## Building

```bash
# Build JAR
./gradlew :modules:services:ehr-connector-service:build

# Build Docker image
docker build -t hdim/ehr-connector-service:latest \
  ./modules/services/ehr-connector-service
```

## Running

### Local Development

```bash
# Set environment variables
export EPIC_CLIENT_ID=your-client-id
export EPIC_PRIVATE_KEY_PATH=/path/to/privatekey.pem

# Run application
./gradlew :modules:services:ehr-connector-service:bootRun
```

### Docker

```bash
docker run -p 8095:8095 \
  -e EPIC_CLIENT_ID=your-client-id \
  -e EPIC_PRIVATE_KEY_PATH=/keys/privatekey.pem \
  -v /path/to/keys:/keys \
  hdim/ehr-connector-service:latest
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Metrics
```
GET /actuator/metrics
GET /actuator/prometheus
```

## Dependencies

- **HAPI FHIR**: R4 FHIR client and resource models
- **JJWT**: JWT creation and signing (RS384)
- **Spring Boot**: Web framework, OAuth2 client
- **Spring Cache**: Token caching (Redis)

## Security Considerations

1. **Private Key Storage**
   - Never commit private keys to version control
   - Use secure key management (AWS KMS, Azure Key Vault, etc.)
   - Rotate keys periodically

2. **Token Security**
   - Tokens cached in-memory or Redis
   - Automatic expiration (50 minutes)
   - Bearer token authentication

3. **TLS/HTTPS**
   - All Epic API calls use HTTPS
   - Validate SSL certificates in production

## Epic Sandbox Testing

For testing with Epic sandbox:

```yaml
epic:
  base-url: https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
  sandbox-mode: true
```

Epic provides test patients and data in their sandbox environment.

## Troubleshooting

### Authentication Failures
- Verify client ID matches Epic registration
- Ensure private key is valid and matches public key uploaded to Epic
- Check JWT expiration (default 5 minutes)

### Rate Limiting
- Epic enforces rate limits (typically 1000 requests/hour)
- Connector implements automatic retry with backoff
- Consider caching frequently accessed data

### FHIR Errors
- Check Epic CapabilityStatement for supported resources
- Verify search parameters are supported
- Review Epic-specific FHIR implementation guide

## License

Copyright (c) 2024 Mahoosuc Solutions

## Support

For issues or questions:
- GitHub Issues: [hdim-master/issues]
- Documentation: [Epic FHIR Documentation](https://fhir.epic.com/)
- Epic App Orchard: [apporchard.epic.com](https://apporchard.epic.com/)
