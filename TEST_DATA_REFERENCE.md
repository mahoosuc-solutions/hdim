# Test Data Reference - Patient Database

**Date:** November 14, 2025
**Status:** ✅ Validated and Ready for Testing

---

## Database Summary

**Total Patients:** 38
- **With MRN (Ready for Testing):** 8
- **Without MRN:** 30
- **Gender Distribution:** 18 Male | 20 Female

---

## Test Patients (With Complete MRN Data)

These 8 patients have complete data including Medical Record Numbers and are ready for Reports feature testing:

### 1. James Michael Anderson
- **MRN:** MRN001234
- **ID:** 84
- **Gender:** Male
- **Date of Birth:** March 15, 1965
- **Age:** 60 years
- **Address:** 123 Main St, Springfield, IL 62701

### 2. Maria Elena Rodriguez
- **MRN:** MRN002567
- **ID:** 85
- **Gender:** Female
- **Date of Birth:** July 22, 1978
- **Age:** 47 years
- **Address:** 456 Oak Ave, Chicago, IL 60601

### 3. David Wei Chen
- **MRN:** MRN003891
- **ID:** 86
- **Gender:** Male
- **Date of Birth:** November 30, 1985
- **Age:** 39 years
- **Address:** 789 Elm St, Aurora, IL 60505

### 4. Jennifer Marie Williams
- **MRN:** MRN004123
- **ID:** 87
- **Gender:** Female
- **Date of Birth:** April 18, 1992
- **Age:** 33 years
- **Address:** 321 Pine Rd, Naperville, IL 60540

### 5. Robert James Taylor
- **MRN:** MRN005456
- **ID:** 88
- **Gender:** Male
- **Date of Birth:** September 5, 1958
- **Age:** 67 years
- **Address:** 654 Maple Dr, Rockford, IL 61101

### 6. Patricia Ann Martinez
- **MRN:** MRN006789
- **ID:** 89
- **Gender:** Female
- **Date of Birth:** December 10, 1970
- **Age:** 54 years
- **Address:** 987 Birch Ln, Peoria, IL 61602

### 7. Michael Andrew Thompson
- **MRN:** MRN007012
- **ID:** 90
- **Gender:** Male
- **Date of Birth:** June 25, 1988
- **Age:** 37 years
- **Address:** 147 Cedar St, Joliet, IL 60432

### 8. Elizabeth Rose Davis
- **MRN:** MRN008345
- **ID:** 91
- **Gender:** Female
- **Date of Birth:** February 14, 1995
- **Age:** 30 years
- **Address:** 258 Willow Ave, Elgin, IL 60120

---

## Age Distribution

| Age Range | Count |
|-----------|-------|
| 30-39     | 3     |
| 40-49     | 1     |
| 50-59     | 1     |
| 60-69     | 3     |

---

## Gender Distribution

| Gender | Count |
|--------|-------|
| Male   | 4     |
| Female | 4     |

---

## Testing Scenarios

### Scenario 1: Generate Patient Report - Young Adult
**Use Patient:** Elizabeth Rose Davis (MRN008345)
- Age: 30, Female
- Good for testing young adult care measures

### Scenario 2: Generate Patient Report - Middle Age
**Use Patient:** Maria Elena Rodriguez (MRN002567)
- Age: 47, Female
- Good for testing preventive care measures

### Scenario 3: Generate Patient Report - Senior
**Use Patient:** Robert James Taylor (MRN005456)
- Age: 67, Male
- Good for testing senior care measures (Medicare)

### Scenario 4: Generate Patient Report - Male Patient
**Use Patient:** David Wei Chen (MRN003891)
- Age: 39, Male
- Good for testing male-specific measures

### Scenario 5: Patient Selection Dialog - Search by Name
**Search Terms to Test:**
- "James" - Should find James Anderson
- "Maria" - Should find Maria Rodriguez
- "Chen" - Should find David Chen
- "Williams" - Should find Jennifer Williams

### Scenario 6: Patient Selection Dialog - Search by MRN
**Search Terms to Test:**
- "MRN001234" - Should find James Anderson
- "MRN002567" - Should find Maria Rodriguez
- "MRN003891" - Should find David Chen
- "008345" - Should find Elizabeth Davis (partial MRN)

### Scenario 7: Generate Multiple Reports
**Test with these patients in sequence:**
1. James Anderson (MRN001234)
2. Patricia Martinez (MRN006789)
3. Michael Thompson (MRN007012)

**Expected Result:** 3 separate reports in Saved Reports list

### Scenario 8: Population Report
**Test Years:**
- Current Year (2025) - Should include all recent evaluations
- 2024 - Should include historical data (if any)
- 2023 - Should include historical data (if any)

---

## API Testing Commands

### Get Patient by ID
```bash
curl -s http://localhost:8083/fhir/Patient/84 | python3 -m json.tool
```

### Get Patient by MRN
```bash
curl -s "http://localhost:8083/fhir/Patient?identifier=MRN001234" | python3 -m json.tool
```

### Search Patients by Name
```bash
curl -s "http://localhost:8083/fhir/Patient?name=Anderson" | python3 -m json.tool
```

### Generate Patient Report (Quality Measure Service)
```bash
curl -X POST -H "X-Tenant-ID: tenant1" \
  "http://localhost:8087/quality-measure/quality-measure/report/patient/save?patient=84&name=Test%20Report&createdBy=api-test" \
  | python3 -m json.tool
```

### List All Saved Reports
```bash
curl -H "X-Tenant-ID: tenant1" \
  "http://localhost:8087/quality-measure/quality-measure/reports" \
  | python3 -m json.tool
```

---

## Data Validation Checklist

### Patient Data Quality
- [x] All test patients have unique MRNs
- [x] All test patients have complete names (first + last)
- [x] All test patients have valid birth dates
- [x] All test patients have gender specified
- [x] Age range covers 30-67 years (diverse)
- [x] Gender distribution is balanced (50/50)
- [x] All test patients have addresses

### FHIR Compliance
- [x] All patients stored in FHIR Patient resource format
- [x] MRN stored in identifier field with proper type
- [x] Names stored in HumanName format
- [x] Birth dates in ISO format (YYYY-MM-DD)
- [x] Gender uses FHIR value set (male/female)

### Database Status
- [x] FHIR server accessible (http://localhost:8083)
- [x] PostgreSQL accessible (port 5435)
- [x] Quality Measure Service accessible (port 8087)
- [x] All Docker containers healthy

---

## Quick Reference Table

| Name                    | MRN       | ID | Age | Gender | Quick Notes           |
|-------------------------|-----------|-----|-----|--------|-----------------------|
| James Anderson          | MRN001234 | 84  | 60  | M      | Senior, good for Medicare |
| Maria Rodriguez         | MRN002567 | 85  | 47  | F      | Middle-age preventive care |
| David Chen              | MRN003891 | 86  | 39  | M      | Young adult male |
| Jennifer Williams       | MRN004123 | 87  | 33  | F      | Young adult female |
| Robert Taylor           | MRN005456 | 88  | 67  | M      | Oldest patient, Medicare |
| Patricia Martinez       | MRN006789 | 89  | 54  | F      | Medicare-eligible soon |
| Michael Thompson        | MRN007012 | 90  | 37  | M      | Active age group |
| Elizabeth Davis         | MRN008345 | 91  | 30  | F      | Youngest test patient |

---

## Testing Tips

### Patient Selection Dialog Testing
1. **Empty Search:** Should show all 38 patients (or 100 max)
2. **Partial Name:** "And" should find Anderson patients
3. **Full Name:** "James Anderson" should find exact match
4. **MRN Search:** "MRN001234" should find James Anderson
5. **Case Insensitive:** "maria" should find Maria Rodriguez

### Report Generation Testing
1. **Success Path:** Select patient → Generate → See success toast → Navigate to Saved Reports
2. **Cancel Path:** Select patient → Click cancel → No report generated
3. **Multiple Reports:** Generate 3-5 reports to test list display
4. **Filter by Type:** Generate both patient and population reports, test filters

### Export Testing
1. **CSV Export:** Download should start immediately, file should be valid CSV
2. **Excel Export:** Download should start immediately, file should open in Excel
3. **File Naming:** Check that report name is used in filename

### Deletion Testing
1. **Confirmation Dialog:** Should show warning icon and report name
2. **Cancel:** Dialog closes, report remains
3. **Confirm:** Report deleted, success toast shown, list refreshes

---

## Validation Commands

### Count Total Patients
```bash
curl -s "http://localhost:8083/fhir/Patient?_summary=count" | python3 -m json.tool
```

### Count Patients with MRN
```bash
curl -s "http://localhost:8083/fhir/Patient?identifier:missing=false&_count=100" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Patients with identifiers: {len(d.get(\"entry\", []))}')"
```

### Verify Test Patient Exists
```bash
# Replace 84 with any patient ID
curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/fhir/Patient/84
# Should return: 200
```

---

## Troubleshooting

### Issue: Patients not appearing in Patient Selection Dialog
**Solution:**
1. Verify FHIR server is running: `docker ps | grep hapi`
2. Check patient count: `curl http://localhost:8083/fhir/Patient?_count=1`
3. Restart frontend dev server

### Issue: MRNs not displaying
**Solution:**
1. Verify MRN structure in database
2. Check frontend PatientService extracts MRN correctly
3. Use patients from "Test Patients" list above (IDs 84-91)

### Issue: Report generation fails
**Solution:**
1. Check Quality Measure Service: `docker logs healthdata-quality-measure`
2. Verify tenant ID header: `X-Tenant-ID: tenant1`
3. Check patient ID exists: `curl http://localhost:8083/fhir/Patient/{id}`

---

## Conclusion

✅ **Database validated and ready for testing**

**Key Points:**
- 8 unique patients with complete MRN data
- Diverse age range (30-67 years)
- Balanced gender distribution
- All FHIR-compliant
- Ready for Reports feature testing

**Recommended First Test:**
1. Generate Patient Report for **James Anderson (MRN001234, ID: 84)**
2. Verify report appears in Saved Reports
3. Test View, Export CSV, Export Excel, Delete actions
4. Repeat with other test patients

---

**Data Created:** November 14, 2025
**Validation Status:** ✅ Complete
**Ready for Testing:** Yes
