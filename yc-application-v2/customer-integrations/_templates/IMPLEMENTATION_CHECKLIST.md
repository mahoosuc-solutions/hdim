# HDIM Implementation Checklist

> Universal checklist for implementing HDIM across all customer types. Customize based on integration method and customer complexity.

## Pre-Implementation

### Customer Discovery (Day 0)

- [ ] **Organization Assessment**
  - [ ] Confirm organization type and size
  - [ ] Document number of patients/attributed lives
  - [ ] Identify all practice locations/sites
  - [ ] Determine quality program participation (MIPS, ACO, UDS)
  - [ ] Assess IT capabilities and resources

- [ ] **Technical Assessment**
  - [ ] Identify all EHR systems in use
  - [ ] Confirm EHR versions and FHIR capabilities
  - [ ] Document other data sources (labs, claims, registries)
  - [ ] Identify data format requirements
  - [ ] Assess network/firewall requirements

- [ ] **Stakeholder Identification**
  - [ ] Executive sponsor
  - [ ] Quality lead / clinical champion
  - [ ] IT contact (if applicable)
  - [ ] End users (care coordinators, MAs)

- [ ] **Success Criteria**
  - [ ] Define baseline quality scores
  - [ ] Set target quality scores
  - [ ] Define time savings goals
  - [ ] Agree on go-live date

---

## Integration Setup

### CSV Upload Path

- [ ] **Template Configuration**
  - [ ] Download CSV templates from HDIM
  - [ ] Map EHR export fields to HDIM template
  - [ ] Create saved export in EHR (if supported)
  - [ ] Document export procedure

- [ ] **Initial Upload**
  - [ ] Export patient roster from EHR
  - [ ] Export diagnosis/condition data
  - [ ] Export medication data
  - [ ] Export lab results (if available)
  - [ ] Upload all files to HDIM
  - [ ] Verify import success

### FHIR API Path

- [ ] **EHR Authorization**
  - [ ] Register HDIM as authorized application
  - [ ] Complete OAuth authorization flow
  - [ ] Verify access token functionality
  - [ ] Configure refresh token handling

- [ ] **Scope Configuration**
  - [ ] Patient demographics (Patient.read)
  - [ ] Conditions (Condition.read)
  - [ ] Medications (MedicationRequest.read)
  - [ ] Labs (Observation.read)
  - [ ] Vitals (Observation.read)
  - [ ] Immunizations (Immunization.read)
  - [ ] Encounters (Encounter.read)

- [ ] **Sync Configuration**
  - [ ] Configure sync frequency (daily/real-time)
  - [ ] Set patient population filters
  - [ ] Configure error notification emails

### n8n Workflow Path

- [ ] **n8n Deployment**
  - [ ] Determine deployment model (HDIM-managed / On-prem / Hybrid)
  - [ ] Provision n8n instance
  - [ ] Configure HDIM API credentials in n8n

- [ ] **Source Connections**
  - [ ] Document source system connection details
  - [ ] Configure source credentials in n8n
  - [ ] Test source connectivity

- [ ] **Workflow Development**
  - [ ] Design data transformation workflow
  - [ ] Build and test workflow in development
  - [ ] Validate output matches FHIR format
  - [ ] Schedule workflow execution

- [ ] **Monitoring**
  - [ ] Configure workflow failure alerts
  - [ ] Set up execution logging
  - [ ] Document troubleshooting procedures

---

## Validation

### Data Quality (Days 4-5)

- [ ] **Patient Matching**
  - [ ] Verify patient count matches source
  - [ ] Check for duplicate patients
  - [ ] Validate demographic accuracy

- [ ] **Clinical Data**
  - [ ] Verify condition codes are mapped correctly
  - [ ] Confirm medication data is complete
  - [ ] Validate lab result values and dates
  - [ ] Check immunization records

- [ ] **Measure Calculation**
  - [ ] Run initial measure calculations
  - [ ] Validate denominator populations
  - [ ] Verify exclusion logic
  - [ ] Compare to known baseline (if available)

### User Acceptance (Days 6-7)

- [ ] **Dashboard Review**
  - [ ] Walk through quality dashboard with customer
  - [ ] Explain measure scores and trends
  - [ ] Review care gap lists
  - [ ] Demonstrate patient drill-down

- [ ] **Workflow Validation**
  - [ ] Test patient search and filtering
  - [ ] Verify care gap worklist functionality
  - [ ] Confirm report export works
  - [ ] Test alert/notification settings

- [ ] **Issue Resolution**
  - [ ] Document any data discrepancies
  - [ ] Resolve mapping issues
  - [ ] Address user feedback
  - [ ] Re-validate after fixes

---

## Go-Live (Day 8)

### Launch Activities

- [ ] **Final Sync**
  - [ ] Perform final data sync
  - [ ] Verify data freshness
  - [ ] Confirm measure calculations are current

- [ ] **User Enablement**
  - [ ] Complete end-user training
  - [ ] Distribute login credentials
  - [ ] Share quick-start guide
  - [ ] Confirm support contact information

- [ ] **Documentation**
  - [ ] Document final configuration
  - [ ] Record sync schedule
  - [ ] Note any custom configurations
  - [ ] Update customer record in CRM

### Go-Live Confirmation

- [ ] Customer confirms system is operational
- [ ] Customer confirms data accuracy
- [ ] Support escalation path confirmed
- [ ] 30-day check-in scheduled

---

## Post-Implementation

### Week 1 Check-in

- [ ] Verify sync is running successfully
- [ ] Address any user questions
- [ ] Review initial usage metrics
- [ ] Confirm data freshness

### Week 2-4 Check-ins

- [ ] Monitor quality score trends
- [ ] Track care gap closure rates
- [ ] Gather user feedback
- [ ] Identify optimization opportunities

### 30-Day Review

- [ ] **Success Metrics Review**
  - [ ] Compare quality scores to baseline
  - [ ] Calculate time savings achieved
  - [ ] Document ROI to date

- [ ] **Optimization**
  - [ ] Identify additional data sources
  - [ ] Discuss measure expansion
  - [ ] Plan for next quality reporting period

- [ ] **Testimonial/Reference**
  - [ ] Request customer testimonial (if appropriate)
  - [ ] Identify reference potential

---

## Integration-Specific Checklists

### Epic Integration

- [ ] Register in App Orchard (or use existing registration)
- [ ] Complete Epic security questionnaire
- [ ] Obtain customer Epic instance URL
- [ ] Configure SMART on FHIR launch
- [ ] Test with Epic sandbox
- [ ] Production go-live approval

### Cerner Integration

- [ ] Register in Cerner CODE program
- [ ] Configure Cerner Ignite FHIR API
- [ ] Obtain customer Cerner tenant ID
- [ ] Configure OAuth with Cerner IDP
- [ ] Test with Cerner sandbox
- [ ] Production activation

### athenahealth Integration

- [ ] Register in athenahealth Marketplace
- [ ] Configure athenahealth API access
- [ ] Obtain customer practice ID
- [ ] Configure OAuth flow
- [ ] Test with athenahealth sandbox
- [ ] Production go-live

### CSV Upload Integration

- [ ] Provide customer with CSV templates
- [ ] Document field mapping
- [ ] Train on export from their EHR
- [ ] Establish upload schedule
- [ ] Configure upload notifications

### n8n Custom Integration

- [ ] Document source system details
- [ ] Design data flow architecture
- [ ] Build transformation workflow
- [ ] Test with sample data
- [ ] Configure production schedule
- [ ] Set up monitoring/alerting

---

## Troubleshooting Quick Reference

| Issue | Likely Cause | Resolution |
|-------|--------------|------------|
| Auth failure | Token expired | Re-authorize in EHR |
| Missing patients | Population filter too narrow | Expand filter criteria |
| Incorrect measures | Wrong condition codes | Verify code mapping |
| Slow sync | Large dataset | Switch to bulk export |
| Duplicate patients | Multiple MRNs | Configure matching rules |
| Stale data | Sync disabled | Check sync schedule |

---

## Support Contacts

| Type | Contact |
|------|---------|
| **Technical Support** | support@healthdatainmotion.com |
| **Integration Help** | integrations@healthdatainmotion.com |
| **Account Management** | success@healthdatainmotion.com |
| **Urgent Issues** | See SLA for tier-specific response times |

---

*Implementation Checklist Version: 1.0*
*Last Updated: December 2025*
