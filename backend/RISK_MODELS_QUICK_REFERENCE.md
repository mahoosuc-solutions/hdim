# Risk Models Quick Reference Guide

## Module Overview

**Location**: `modules:shared:domain:risk-models`

**Package**: `com.hdim.riskmodels`

**Total Tests**: 154 TDD tests across 6 test suites

---

## Risk Indices

### Charlson Comorbidity Index

**Purpose**: Predicts 10-year mortality

**Usage**:
```java
CharlsonComorbidityIndex charlson = new CharlsonComorbidityIndex();
RiskIndexResult result = charlson.calculate(icd10Codes, patientAge);
```

**Scoring**:
- 0-1: Low Risk
- 2: Medium Risk
- 3-4: High Risk
- 5+: Very High Risk

**Key Features**:
- Age adjustment (1 pt per decade over 40)
- 19 comorbidity categories
- Hierarchical resolution (diabetes, liver, cancer)

---

### Elixhauser Comorbidity Index

**Purpose**: Hospital mortality prediction

**Usage**:
```java
ElixhauserComorbidityIndex elixhauser = new ElixhauserComorbidityIndex();
RiskIndexResult result = elixhauser.calculate(icd10Codes);
```

**Scoring**:
- <0: Low Risk
- 0-10: Medium Risk
- 10-25: High Risk
- 25+: Very High Risk

**Key Features**:
- 31 comorbidity categories
- AHRQ mortality weights
- Negative weights for protective factors

---

### LACE Index

**Purpose**: 30-day readmission prediction

**Usage**:
```java
LACEIndex lace = new LACEIndex();
RiskIndexResult result = lace.calculate(
    lengthOfStayDays,
    isAcuteAdmission,
    icd10Codes,
    edVisitsIn6Months
);
```

**Components**:
- **L**ength of stay: 0-7 points
- **A**cuity of admission: 0 or 3 points
- **C**omorbidities (Charlson): 0-5 points
- **E**D visits: 0-4 points

**Scoring**:
- <5: Low Risk
- 5-9: Medium Risk
- 10+: High Risk (Max: 19)

---

### HCC Risk Score

**Purpose**: CMS risk adjustment & cost prediction

**Usage**:
```java
HCCRiskScore hcc = new HCCRiskScore();
RiskIndexResult result = hcc.calculate(
    icd10Codes,
    age,
    isMale,
    isDisabled,
    isDualEligible
);
```

**Scoring**:
- <1.0: Below Average
- 1.0-1.5: Average
- 1.5-2.5: Above Average
- 2.5+: High Risk

**Key Features**:
- CMS-HCC Model V28
- Age-sex demographics
- Disease interactions
- Disability & dual eligibility adjustments

---

### Frailty Index

**Purpose**: Frailty assessment

**Usage**:
```java
FrailtyIndex frailty = new FrailtyIndex();
RiskIndexResult result = frailty.calculate(
    hasWeightLoss, hasLowActivity, hasExhaustion,
    hasWeakness, hasSlowWalking, hasCognitiveDecline,
    hasMultipleFalls, hasADLDependency,
    hasPolypharmacy, hasMultiMorbidity
);
```

**Scoring**:
- <0.2: Robust
- 0.2-0.4: Pre-Frail
- 0.4-0.6: Frail
- 0.6+: Severely Frail

---

## Diagnosis Groupers

### ICD10Grouper

**Purpose**: Group ICD-10 codes into disease categories

**Usage**:
```java
ICD10Grouper grouper = new ICD10Grouper();
String category = grouper.getCategory("I21.0");
// Returns: "Circulatory System Diseases"

String subcategory = grouper.getSubcategory("E11.9");
// Returns: "Diabetes Mellitus"

Map<String, String> results = grouper.groupMultiple(icd10Codes);
```

**Categories**: 21 ICD-10 chapters (A00-Z99)

---

### HCCGrouper

**Purpose**: Map ICD-10 to HCC categories

**Usage**:
```java
HCCGrouper hccGrouper = new HCCGrouper();
Integer hcc = hccGrouper.getHCC("E11.21");
// Returns: 18 (Diabetes with complications)

Map<String, Integer> results = hccGrouper.groupMultiple(icd10Codes);
```

**Key HCCs**:
- HCC 8: Metastatic Cancer (2.659)
- HCC 18: Diabetes w/ complications (0.318)
- HCC 85: CHF (0.323)
- HCC 111: COPD (0.328)
- HCC 135: CKD Stage 5 (0.415)

---

### CCSGrouper

**Purpose**: Clinical Classifications Software grouping

**Usage**:
```java
CCSGrouper ccs = new CCSGrouper();
String category = ccs.getCCSCategory("I50.1");
// Returns: "Congestive Heart Failure"
```

**Common Categories**:
- Coronary Artery Disease
- CHF
- COPD
- Diabetes Mellitus
- CKD
- Cancer
- Depression

---

### MDCGrouper

**Purpose**: Major Diagnostic Categories for DRG

**Usage**:
```java
MDCGrouper mdc = new MDCGrouper();
Integer mdcCode = mdc.getMDC("I50.1");
// Returns: 5 (Circulatory System)

String description = mdc.getMDCDescription(5);
// Returns: "Diseases and Disorders of the Circulatory System"
```

---

## Risk Adjusters

### AgeRiskAdjuster

**Purpose**: Age-based risk multiplication

**Usage**:
```java
AgeRiskAdjuster adjuster = new AgeRiskAdjuster();
double adjusted = adjuster.adjust(baseScore, patientAge);
double multiplier = adjuster.calculateAgeMultiplier(75);
// Returns: 1.8
```

**Multipliers**:
- <18: 0.5, 18-39: 0.8, 40-49: 1.0
- 50-59: 1.2, 60-69: 1.5, 70-79: 1.8, 80+: 2.2

---

### GenderRiskAdjuster

**Purpose**: Gender-specific risk adjustment

**Usage**:
```java
GenderRiskAdjuster adjuster = new GenderRiskAdjuster();
double adjusted = adjuster.adjust(baseScore, isMale, age);
```

**Multipliers** (Male/Female):
- <50: 1.1 / 0.9
- 50-69: 1.15 / 0.95
- 70+: 1.05 / 1.0

---

### DualEligibilityAdjuster

**Purpose**: Medicare/Medicaid dual eligible adjustment

**Usage**:
```java
DualEligibilityAdjuster adjuster = new DualEligibilityAdjuster();
double adjusted = adjuster.adjust(baseScore, isDualEligible);
// 25% increase if dual eligible

double coefficient = adjuster.calculateDualEligibilityCoefficient(
    isDualEligible,
    isFullBenefit
);
```

**Coefficients**:
- Full Dual: 0.209
- Partial Dual: 0.119

---

### DisabilityAdjuster

**Purpose**: Disability status risk adjustment

**Usage**:
```java
DisabilityAdjuster adjuster = new DisabilityAdjuster();
double adjusted = adjuster.adjust(baseScore, isDisabled);
// 35% increase if disabled

double coefficient = adjuster.calculateDisabilityCoefficient(
    isDisabled,
    age
);
```

**Coefficients by Age**:
- <35: 0.45, 35-44: 0.50, 45-54: 0.55
- 55-64: 0.60, 65+: 0.40

---

## Domain Models

### RiskIndexResult

**Core result object for all risk calculations**

```java
RiskIndexResult result = ...;

String name = result.getIndexName();
double score = result.getScore();
String interpretation = result.getInterpretation();
List<RiskExplanation> explanations = result.getExplanations();
Instant timestamp = result.getCalculatedAt();
String version = result.getVersion();

boolean highRisk = result.isHighRisk(10.0); // threshold
```

---

### RiskExplanation

**Individual risk factor explanation**

```java
RiskExplanation explanation = RiskExplanation.builder()
    .factor("Diabetes with complications")
    .description("HCC 18")
    .contribution(0.318)
    .evidenceCode("E11.21")
    .evidenceSystem("ICD-10-CM")
    .build();
```

---

### RiskTrend

**Track risk scores over time**

```java
RiskTrend trend = RiskTrend.builder()
    .indexName("Charlson Comorbidity Index")
    .patientId("patient-123")
    .addDataPoint(timestamp1, score1, interpretation1)
    .addDataPoint(timestamp2, score2, interpretation2)
    .build();

Double current = trend.getCurrentScore();
Double baseline = trend.getBaselineScore();
Double change = trend.getScoreChange();
Double percentChange = trend.getPercentageChange();
boolean trendingUp = trend.isTrendingUp();
boolean trendingDown = trend.isTrendingDown();
```

---

## Clinical Reference

### Common ICD-10 Codes

**Diabetes**:
- E11.9: Type 2 without complications
- E11.21: Type 2 with nephropathy (complications)

**Cardiovascular**:
- I21.0: Acute MI
- I50.1: CHF
- I10: Hypertension

**Respiratory**:
- J44.0: COPD with acute exacerbation
- J45.0: Asthma

**Renal**:
- N18.3: CKD Stage 3
- N18.4: CKD Stage 4
- N18.5: CKD Stage 5

**Cancer**:
- C50.911: Breast cancer
- C78.00: Metastatic cancer

---

## Integration Examples

### FHIR Patient Risk Calculation

```java
// Extract from FHIR resources
List<String> icd10Codes = patient.getConditions().stream()
    .map(c -> c.getCode().getCodingFirstRep().getCode())
    .collect(Collectors.toList());

int age = patient.getAge();

// Calculate risk
CharlsonComorbidityIndex charlson = new CharlsonComorbidityIndex();
RiskIndexResult result = charlson.calculate(icd10Codes, age);

// Create FHIR Observation
Observation obs = new Observation();
obs.setCode(new CodeableConcept()
    .setText("Charlson Comorbidity Index"));
obs.setValue(new DecimalType(result.getScore()));
obs.setInterpretation(new CodeableConcept()
    .setText(result.getInterpretation()));
```

---

### Population Health Analysis

```java
// Calculate risk for entire population
List<Patient> patients = getPatientCohort();
CharlsonComorbidityIndex charlson = new CharlsonComorbidityIndex();

Map<String, Double> riskScores = patients.stream()
    .collect(Collectors.toMap(
        Patient::getId,
        p -> charlson.calculate(
            p.getICD10Codes(),
            p.getAge()
        ).getScore()
    ));

// Identify high-risk patients
List<String> highRiskPatients = riskScores.entrySet().stream()
    .filter(e -> e.getValue() >= 5.0)
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
```

---

### Readmission Risk Workflow

```java
// At discharge
LACEIndex lace = new LACEIndex();
RiskIndexResult laceScore = lace.calculate(
    encounter.getLengthOfStay(),
    encounter.isAcuteAdmission(),
    patient.getICD10Codes(),
    patient.getEDVisitsIn6Months()
);

if (laceScore.getScore() >= 10) {
    // High readmission risk
    assignCareManager(patient);
    scheduleFollowUp(patient, 7); // days
    enableRemoteMonitoring(patient);
}
```

---

## Testing

All components have comprehensive TDD tests:

```bash
./gradlew :modules:shared:domain:risk-models:test
```

**Test Suites**:
- CharlsonComorbidityIndexTest (43 tests)
- ElixhauserComorbidityIndexTest (30 tests)
- LACEIndexTest (29 tests)
- HCCRiskScoreTest (28 tests)
- ICD10GrouperTest (14 tests)
- FrailtyIndexTest (10 tests)

**Total: 154 tests**

---

## Performance Notes

- **Thread-safe**: All components safe for concurrent use
- **Cacheable**: Results can be cached by patient/timestamp
- **Fast**: <2ms per calculation
- **Stateless**: No database dependencies for calculation
- **Scalable**: Suitable for batch/real-time processing

---

## Version Information

- **ICD-10-CM**: 2024
- **CMS-HCC Model**: V28 (2024)
- **Elixhauser Weights**: AHRQ 2024
- **Build Tool**: Gradle 8.x
- **Java Version**: 21

---

## Support & References

### Documentation
- Full implementation: `RISK_STRATIFICATION_IMPLEMENTATION.md`
- JavaDoc: Available in source code
- Tests: Comprehensive examples in test suites

### Clinical References
- Charlson ME, et al. J Chronic Dis. 1987
- Elixhauser A, et al. Med Care. 1998
- van Walraven C, et al. CMAJ. 2010
- CMS Risk Adjustment Documentation (2024)

### Standards
- ICD-10-CM Official Guidelines
- HL7 FHIR R4
- CMS Medicare Documentation
- AHRQ Clinical Classifications

---

**For detailed implementation information, see**: `RISK_STRATIFICATION_IMPLEMENTATION.md`
