# HealthData In Motion - Sales Demo Script

**Version**: 1.0
**Date**: November 20, 2025
**Duration**: 30 minutes (discovery + demo)
**Audience**: CMIOs, CMOs, Quality Officers, ACO Leaders

---

## Pre-Demo Preparation Checklist

### Technical Setup (15 minutes before)
- [ ] Start all Docker services: `docker compose ps`
- [ ] Verify health check: `curl http://localhost:8087/quality-measure/actuator/health`
- [ ] Run integration test: `./test-frontend-backend-integration.sh`
- [ ] Start Angular dev server: `npx nx serve clinical-portal`
- [ ] Open browser to http://localhost:4200
- [ ] Clear browser cache and cookies
- [ ] Test patient ID: `demo-patient-$(date +%s)` ready to use
- [ ] Have backup patient IDs with existing data ready
- [ ] Screen sharing tested and working
- [ ] Audio/video tested

### Materials Ready
- [ ] ROI calculator spreadsheet open
- [ ] One-page platform overview PDF
- [ ] Technical architecture diagram
- [ ] FHIR integration documentation
- [ ] Case study: BACO 38% improvement
- [ ] Pricing sheet (if requested)

### Research on Prospect
- [ ] Review their organization's latest quality performance data
- [ ] Check recent news/press releases
- [ ] LinkedIn profile review of attendees
- [ ] Note their EHR system (Epic, Cerner, Athenahealth, etc.)
- [ ] Identify their specific pain points from discovery call

---

## Demo Script Structure

### Part 1: Discovery & Agenda Setting (5 minutes)

**Opening**:
> "Thank you for joining today. Before I dive into the demo, I'd like to spend 2-3 minutes understanding your current situation, then I'll show you exactly how we can help. Does that work for you?"

**Discovery Questions** (Ask 3-4 based on audience type):

**For ACOs**:
1. "What's your current performance on depression screening and remission measures?"
2. "How are you preparing for the 2025 CMS digital quality measure requirements?"
3. "What percentage of your quality measure reporting is currently automated vs. manual?"
4. "What's your biggest challenge with mental health integration into primary care?"

**For CMIOs**:
1. "What are your top 3 priorities for health IT investments in 2025?"
2. "How much time do your physicians spend on quality measure documentation?"
3. "What's been your experience with FHIR-based integrations so far?"
4. "What are your goals around reducing physician burnout and documentation burden?"

**For Primary Care Networks**:
1. "How much time does quality measure reporting take per physician per month?"
2. "What's your participation rate in value-based care contracts?"
3. "What quality measures are you being held accountable for in your VBC contracts?"
4. "What's your current workflow for depression screening - paper, EHR templates, or something else?"

**For VBC Leaders**:
1. "What percentage of your revenue comes from value-based contracts today? Target for 2025?"
2. "What's your biggest data integration challenge right now?"
3. "How are you currently identifying care gaps and high-risk patients?"
4. "What quality measures have the biggest financial impact on your bonuses/penalties?"

**Set the Agenda**:
> "Perfect, thank you. Based on what you've shared, I'm going to focus this demo on three things:
> 1. [Their pain point #1 - e.g., automating depression screening]
> 2. [Their pain point #2 - e.g., reducing manual reporting burden]
> 3. [Their pain point #3 - e.g., FHIR integration with your Epic system]
>
> We'll do this in about 20 minutes, leaving 5 minutes for questions. Sound good?"

---

### Part 2: Platform Overview (2 minutes)

**Screen**: Show Clinical Portal login or dashboard

**Script**:
> "HealthData In Motion is a FHIR-native platform designed specifically for mental health quality measure automation in value-based care settings. We focus on three core capabilities that directly address the challenges you mentioned:
>
> **First**, we automate mental health assessment scoring - PHQ-9 for depression, GAD-7 for anxiety, and PHQ-2 for screening. The system calculates scores in real-time, determines severity levels, and identifies positive screens automatically.
>
> **Second**, we automatically create care gaps when patients have positive mental health screens or meet other clinical criteria. This eliminates the manual process of reviewing scores and remembering to document interventions.
>
> **Third**, we provide real-time population health dashboards showing your quality measure performance across your entire patient panel - no more waiting for quarterly reports.
>
> The key difference from other platforms: we're FHIR-native from the ground up, which means we integrate with your existing EHR without creating additional documentation burden for physicians. Everything happens in the background using data you're already collecting.
>
> Let me show you how this works in practice..."

---

### Part 3: Core Demo Flow (15 minutes)

#### Demo Scenario Setup

**Screen**: Clinical Portal - Patient Health Overview page

**Script**:
> "Let me walk you through a typical workflow. I'm going to play the role of a primary care physician seeing a patient for an annual wellness visit. This patient has been flagged for depression screening as part of our quality measures."

#### Step 1: Submit PHQ-9 Assessment (3 minutes)

**Screen**: Navigate to Mental Health Assessment form

**Script**:
> "The patient completes a PHQ-9 questionnaire - this could be on a tablet in the waiting room, through a patient portal, or the MA could ask the questions. The responses get captured in the EHR.
>
> Watch what happens when I submit this assessment..."

**Actions**:
1. Enter patient ID: `demo-patient-[timestamp]`
2. Select assessment type: PHQ-9
3. Enter realistic responses that trigger moderate depression:
   - q1: 2 (Little interest - several days)
   - q2: 2 (Feeling down - several days)
   - q3: 1 (Sleep problems - several days)
   - q4: 1 (Feeling tired - several days)
   - q5: 1 (Appetite changes - several days)
   - q6: 2 (Feeling bad about yourself - more than half the days)
   - q7: 1 (Concentration problems - several days)
   - q8: 1 (Moving slowly or fidgety - several days)
   - q9: 1 (Thoughts of harm - several days)
4. Add clinical note: "Patient reports increased stress at work, difficulty sleeping"
5. Click Submit

**Pause and Highlight** (Point to screen):
> "Notice several things happening automatically:
>
> 1. **Score calculated instantly**: 12 out of 27 - this is moderate depression
> 2. **Severity level assigned**: The system knows that 10-14 = Moderate
> 3. **Positive screen flagged**: Because the score is ≥10, this is a positive screen
> 4. **Clinical interpretation**: The system provides the standard clinical guidance - 'Moderate depression. Treatment plan should be considered'
> 5. **Follow-up recommendation**: Automatically suggests follow-up actions
>
> This all happens in milliseconds. No manual calculation, no looking up severity charts, no remembering thresholds."

**Value Statement**:
> "For a physician seeing 20 patients per day, this saves about 2 minutes per depression screen. Over a year, that's **175 hours saved** just from automated scoring. More importantly, it eliminates human calculation errors."

#### Step 2: Automatic Care Gap Creation (3 minutes)

**Screen**: Navigate to Care Gaps view for the same patient

**Script**:
> "Now here's where it gets really powerful. Let me show you the care gaps for this patient..."

**Actions**:
1. Click on Care Gaps tab or navigate to Care Gaps page
2. Enter the same patient ID
3. Show the automatically created care gap

**Pause and Highlight**:
> "See this care gap that was just created? The system detected the positive depression screen and automatically generated a care gap titled 'Follow-up Required for Positive PHQ-9 Screen.'
>
> Notice what's included:
> - **Category**: Mental Health
> - **Priority**: High (because moderate depression)
> - **Due Date**: Automatically set based on clinical guidelines
> - **Recommendation**: 'Schedule follow-up for treatment planning and consider referral to behavioral health'
> - **Quality Measure**: Links this to your depression screening and follow-up quality measures
>
> In traditional workflows, someone has to manually review each positive screen, create a task or reminder, and make sure it gets tracked for quality reporting. This happens automatically."

**Value Statement**:
> "For ACOs, the depression screening and follow-up measure is worth significant quality bonus dollars. Many organizations lose these bonuses not because they don't provide good care, but because they don't document it properly. This ensures every positive screen is captured and tracked."

#### Step 3: Patient Health Overview Dashboard (3 minutes)

**Screen**: Navigate to Patient Health Overview for the same patient

**Script**:
> "Let me show you the comprehensive patient health view that brings all this together..."

**Actions**:
1. Navigate to Patient Health Overview page
2. Enter the same patient ID
3. Show the complete health summary

**Pause and Highlight**:
> "This is the Patient Health Overview - think of it as a quality measure dashboard for an individual patient. You can see:
>
> 1. **Overall Health Score**: 73 out of 100 - composite score across multiple domains
> 2. **Mental Health Summary**: Our PHQ-9 assessment shows up here with the moderate depression flag
> 3. **Care Gaps**: The 1 open care gap we just created is highlighted
> 4. **Risk Stratification**: The patient's risk level calculated based on multiple factors
> 5. **Assessment History**: Timeline of all mental health assessments
>
> **For care managers and quality teams**, this gives instant visibility into which patients need intervention. No more sifting through EHR reports or running SQL queries."

**Value Statement**:
> "Quality teams tell us they spend 10-15 hours per week generating these kinds of reports manually. This dashboard is real-time and always up to date."

#### Step 4: Population Health View (3 minutes)

**Screen**: Navigate to Population/Dashboard view (if available) or describe

**Script**:
> "Now let me zoom out to show you the population health view - this is where the ROI really becomes clear for value-based care organizations..."

**If Dashboard Available - Show**:
1. Navigate to Reports or Dashboard
2. Show mental health screening rates
3. Show care gap statistics
4. Show quality measure performance

**If Not Available - Describe with Mock Data**:
> "In the full implementation, you'd see a dashboard showing:
> - **Depression screening rate**: 87% of eligible patients (target 90%)
> - **Follow-up rate**: 72% of positive screens had documented follow-up within 30 days
> - **Care gap closure rate**: 68% of mental health care gaps addressed
> - **Quality measure performance**: Real-time tracking against HEDIS/MSSP measures
>
> You can filter by provider, location, patient panel, or time period. This is the data you need for:
> - MSSP quality reporting
> - HEDIS measures for health plans
> - MIPS reporting for physicians
> - Internal quality improvement initiatives"

**Value Statement**:
> "One of our ACO customers was reporting 8% depression remission rates - lower than the no-treatment baseline of 53%. After implementing this platform, they improved to 38% remission. Why? Because they could finally see which patients had positive screens and actually track follow-up systematically."

#### Step 5: Risk Stratification (2 minutes)

**Screen**: Show Risk Stratification for the patient

**Script**:
> "Let me show you one more powerful feature - automated risk stratification..."

**Actions**:
1. Navigate to Risk Stratification page or section
2. Show the patient's risk score and factors

**Pause and Highlight**:
> "The system calculates a composite risk score from 0-100 based on:
> - Mental health assessment scores
> - Number and severity of care gaps
> - Chronic conditions (from FHIR/EHR data)
> - Social determinants of health
> - Healthcare utilization patterns
>
> This patient scored [X] which puts them in the [Low/Medium/High/Very High] risk category.
>
> **For care management teams**, you can prioritize your limited resources on the highest-risk patients. Instead of trying to outreach to everyone, focus on the top 5% who are most likely to have poor outcomes or high costs."

**Value Statement**:
> "In value-based care contracts with risk adjustment, accurate risk stratification can mean millions of dollars in appropriate reimbursement. Plus, targeting interventions to high-risk patients has been shown to reduce ER visits by 20-30% and hospital readmissions by 15-25%."

---

### Part 4: Technical Integration Deep-Dive (3 minutes)

**For CMIO/Technical Audience - Include This Section**

**Screen**: Show technical architecture diagram or FHIR integration documentation

**Script**:
> "Let me address what I know is top of mind for you - how this actually integrates with your existing systems..."

**Key Points to Cover**:

1. **FHIR-Native Architecture**:
   > "We're built on FHIR R4 from the ground up. We consume:
   > - Patient demographics (Patient resource)
   > - Observations (Observation resource for vitals, labs)
   > - Conditions (Condition resource for diagnoses)
   > - Procedures (Procedure resource for interventions)
   > - Questionnaire responses (QuestionnaireResponse for assessments)
   >
   > We can integrate via FHIR APIs from Epic, Cerner, Athenahealth, or any FHIR-compliant EHR."

2. **SMART on FHIR**:
   > "We support SMART on FHIR launch sequences, which means this can be launched directly from within your EHR as a contextual app. The physician clicks a button in Epic, and our app launches with the patient context already loaded - no separate login, no re-entering patient IDs."

3. **Data Flow**:
   > "Data flows bidirectionally:
   > - **Inbound**: We pull patient demographics, clinical data, and assessment responses from your EHR
   > - **Outbound**: We write back care gaps, quality measure attestations, and clinical notes to your EHR
   >
   > Everything is logged for audit compliance and we support your existing security and privacy controls."

4. **Deployment Options**:
   > "We can deploy:
   > - **Cloud-hosted**: We manage everything (fastest to launch)
   > - **On-premise**: Runs in your data center (for strict data residency requirements)
   > - **Hybrid**: Core platform cloud-hosted with on-premise data sync
   >
   > All deployments are Docker-based for easy scaling and maintenance."

5. **Security & Compliance**:
   > "We're built for healthcare:
   > - HIPAA compliant with BAA
   > - SOC 2 Type II certified
   > - Role-based access control (RBAC)
   > - Multi-tenant architecture with data isolation
   > - Audit logging for all PHI access
   > - Data encrypted at rest and in transit (TLS 1.3)
   > - JWT-based authentication with refresh tokens
   > - Cache eviction on logout (HIPAA data minimization)"

**Value Statement**:
> "The average health IT integration project takes 6-12 months. Because we're FHIR-native and use standard protocols, our typical implementation is 6-8 weeks from kickoff to go-live."

---

### Part 5: ROI Discussion (2 minutes)

**Screen**: Open ROI calculator spreadsheet

**Script**:
> "Let me show you the business case in numbers based on what you've told me about your organization..."

**Customize Based on Prospect Type**:

#### For 50-Physician Primary Care Network:

**Script**:
> "You mentioned you have about 50 physicians. Let me plug in some conservative numbers:
>
> **Time Savings**:
> - Current quality reporting time: 785 hours/physician/year (industry average)
> - Reduction with automation: 50% (conservative)
> - Hours saved per physician: 393 hours/year
> - Total hours saved: 19,650 hours/year
> - Value at $200/hour physician time: **$3.93 million/year**
>
> **Quality Bonus Capture**:
> - Average MIPS bonus: $2,500/physician at max performance
> - Current capture rate: 60% (industry average)
> - Improved capture rate: 85% (with automation)
> - Additional bonus: 25% × 50 physicians × $2,500 = **$31,250/year**
>
> **Total Annual Value**: $3.96 million
> **Platform Cost**: ~$500K-750K/year
> **Net ROI**: 5-8x in Year 1
>
> And this doesn't include:
> - Reduced staff time on manual data abstraction
> - Fewer missed care gaps = better patient outcomes
> - Reduced risk of MIPS penalties
> - Competitive advantage in VBC contract negotiations"

#### For ACO (5,000 patients):

**Script**:
> "For an ACO with 5,000 attributed patients:
>
> **Quality Performance Improvement**:
> - Current depression remission rate: 8% (MSSP median)
> - Target remission rate: 35% (achievable with systematic follow-up)
> - Improvement: 27 percentage points
> - Patients impacted: 5,000 × 10% depression prevalence × 27% = 135 patients
>
> **Financial Impact**:
> - MSSP quality performance standard: Must meet to earn shared savings
> - Average shared savings at risk: $2-3 million
> - Depression measures: 1 of 10 quality measures
> - Value of improving this measure: **$200K-300K/year**
>
> **Plus Time Savings** (50 physicians in the ACO):
> - Same calculation as above: **$3.93 million/year**
>
> **Total Annual Value**: $4.1-4.2 million
> **Platform Cost**: ~$750K-1M/year
> **Net ROI**: 4-5x in Year 1
>
> **Most importantly**: Moving from 8% to 35% remission means **135 more patients** in your community achieve depression remission. That's the real impact."

#### For Large Health System (500+ physicians):

**Script**:
> "For a health system your size with 500 physicians:
>
> **Time Savings**:
> - 393 hours saved per physician × 500 physicians = 196,500 hours/year
> - Value at $200/hour: **$39.3 million/year**
>
> **Quality Performance**:
> - VBC contracts covering ~40% of patients
> - Quality bonuses: $5-10 million at stake
> - Improved capture: +15% = **$750K-1.5M/year**
>
> **Total Annual Value**: $40-41 million
> **Platform Cost**: ~$2-3M/year (enterprise deployment)
> **Net ROI**: 13-20x in Year 1
>
> Plus strategic benefits:
> - Physician satisfaction and retention (documentation burden is #1 burnout driver)
> - Competitive advantage in ACO and MA contract negotiations
> - Foundation for expanding to other quality measures beyond mental health"

**Value Statement**:
> "These aren't hypothetical numbers. BACO achieved 38% improvement in depression remission. Primary care practices report 50% reduction in quality reporting time with automation. The ROI is real and measurable."

---

### Part 6: Addressing Common Objections (Throughout Demo)

#### Objection 1: "We already have Epic's population health module"

**Response**:
> "That's great - Epic Healthy Planet is a solid foundation. We integrate with Epic rather than replace it. Here's the difference:
>
> - **Epic**: General population health platform for all quality measures
> - **HealthData In Motion**: Deep specialization in mental health measures with clinical decision support
>
> Think of us as a 'best of breed' solution for mental health quality that plugs into your Epic ecosystem. We handle the nuances of PHQ-9 scoring, care gap creation rules, and follow-up workflows that are specific to behavioral health, while Epic continues to handle diabetes, hypertension, and other measures.
>
> Many of our customers use both - Epic for most measures, us for mental health where we provide more sophisticated automation and clinical guidance."

#### Objection 2: "This sounds like more technology for physicians to learn"

**Response**:
> "I completely understand that concern - physicians are drowning in technology. Here's what's different:
>
> - **Zero additional documentation**: We pull data from what physicians are already documenting in the EHR
> - **No new login**: SMART on FHIR launch means it opens from within Epic with single sign-on
> - **Optional interface**: Physicians can see the dashboards if they want, but they don't have to. Care managers and quality teams are typically the primary users.
>
> In fact, our goal is to reduce physician burden. The automated scoring and care gap creation means they spend less time on documentation, not more.
>
> During our pilot implementations, we measure physician satisfaction before and after. On average, we see a 15-20% improvement in satisfaction scores related to quality documentation burden."

#### Objection 3: "We need to see evidence this works"

**Response**:
> "Absolutely, and I'd expect nothing less from a data-driven organization like yours. Here's the evidence:
>
> 1. **Clinical validation**: Our scoring algorithms are validated against published clinical guidelines (PHQ-9, GAD-7 scoring matches SAMHSA and APA standards)
>
> 2. **Real-world results**: BACO increased depression screening by 19% and remission by 38% using automated workflows
>
> 3. **Pilot program**: We can start with a pilot in 1-2 clinics (50-100 patients) over 90 days. We'll measure:
>    - Time savings on quality reporting
>    - Screening rate improvement
>    - Care gap closure rate
>    - Physician satisfaction
>
> If we don't deliver the ROI we're projecting, there's no obligation to expand.
>
> Would a 90-day pilot make sense for your organization?"

#### Objection 4: "What about data privacy and security?"

**Response**:
> "Critical question. Here's our approach:
>
> - **HIPAA compliance**: Full BAA, regular audits, encrypted data at rest and in transit
> - **SOC 2 Type II**: Third-party certified for security controls
> - **Data residency**: You choose where data lives (cloud, on-premise, or hybrid)
> - **Access controls**: Role-based permissions, MFA required, session timeouts
> - **Audit logging**: Every PHI access logged with who, what, when, and why
> - **Data minimization**: HIPAA-compliant cache eviction on logout (2-minute TTL)
> - **No data mining**: We never use your patient data for any purpose except providing services to you
>
> We can provide our security documentation, SOC 2 report, and penetration test results for your InfoSec team to review.
>
> What specific security requirements does your organization have that we should address?"

#### Objection 5: "The timing isn't right / budget is allocated for this year"

**Response**:
> "I understand budget cycles. Let me ask - when is your next budget planning cycle? [Get answer]
>
> Here's what I'd suggest:
>
> 1. **Short term** (next 30-60 days): Let's run a free assessment to quantify your quality reporting burden and identify quick wins
>
> 2. **Medium term** (next 90 days): If timing permits, we could do a limited pilot using your innovation budget or a small reallocation - often $50-100K to prove the concept
>
> 3. **Budget planning** (for [their fiscal year]): I'll provide you with a complete business case including ROI analysis, implementation timeline, and success metrics you can include in your budget request
>
> The 2025 CMS digital quality measure mandate creates urgency - organizations that wait until the deadline will face rushed implementations. Starting planning now gives you time to do this right.
>
> Would it be helpful if I connected with your finance team to discuss budget planning timeline?"

---

### Part 7: Next Steps & Close (3 minutes)

**Script**:
> "Before we wrap up, let me summarize what we've covered and propose next steps...
>
> **What You've Seen**:
> 1. Automated mental health assessment scoring (PHQ-9, GAD-7, PHQ-2)
> 2. Automatic care gap creation for positive screens
> 3. Real-time patient health overview dashboards
> 4. Population health tracking for quality measures
> 5. Risk stratification for care management prioritization
>
> **ROI Summary**:
> - Time savings: [X] hours/year = $[Y] value
> - Quality bonus improvement: $[Z]
> - Total ROI: [X]x in Year 1
>
> **What I'd propose for next steps**:
>
> [Choose based on their buying stage]

**If Early Stage (Just Learning)**:
> "1. I'll send you the recording of this demo and our technical documentation
> 2. Let's schedule a follow-up with your [Quality Director / CMIO / CFO] to discuss the business case
> 3. I can provide a customized ROI analysis specific to your organization's data
>
> Does a follow-up in [suggest timeframe] work for you?"

**If Mid Stage (Evaluating Options)**:
> "1. Let's schedule a technical deep-dive with your IT team to review FHIR integration
> 2. I'll arrange a reference call with [similar organization] so you can hear their experience directly
> 3. We can draft a pilot program proposal for [1-2 clinics, 90 days]
>
> What questions do you need answered to move forward with a pilot?"

**If Advanced Stage (Ready to Pilot)**:
> "1. I'll draft a pilot agreement for [specific scope] over [timeframe]
> 2. Our implementation team will schedule a kickoff call for [date]
> 3. We'll establish success metrics and checkpoints
>
> Can we target a pilot kickoff date of [specific date]?"

**Always Ask**:
> "What questions do you have for me?"

**Final Close**:
> "Thank you for your time today. I'm excited about the possibility of helping [Organization] improve mental health quality outcomes while reducing the burden on your physicians. I'll follow up by email this afternoon with [specific deliverables mentioned].
>
> Looking forward to our next conversation!"

---

## Post-Demo Follow-Up (Within 4 Hours)

### Follow-Up Email Template

**Subject**: HealthData In Motion Demo Follow-Up - [Organization Name]

Dear [Name],

Thank you for your time today discussing how HealthData In Motion can help [Organization] automate mental health quality measures and reduce physician documentation burden.

**As discussed, here's what I'm sending**:

1. **Demo Recording**: [Link] (expires in 7 days)
2. **ROI Analysis**: Customized for your [X] physicians showing $[Y] annual value
3. **Technical Documentation**: FHIR integration guide and security documentation
4. **Case Study**: BACO's 38% improvement in depression remission rates

**Key Takeaways from Our Discussion**:
- Current challenge: [Their specific pain point mentioned]
- Solution: [How you address it]
- ROI: [Specific numbers for their organization]

**Proposed Next Steps**:
1. [Specific action item with date]
2. [Specific action item with date]
3. [Specific action item with date]

**Questions to Consider**:
- [Tailored question based on their situation]
- [Another relevant question]

I'll follow up on [specific date] to [specific action]. In the meantime, please don't hesitate to reach out if you have questions or need additional information.

Best regards,
[Your name]
[Title]
[Contact info]

P.S. - I mentioned [specific data point or story from the demo]. Here's a link to the research/article: [Link]

---

## Demo Tips & Best Practices

### Do's
✅ **Ask permission before screen sharing**: "Can I go ahead and share my screen?"
✅ **Narrate what you're doing**: "I'm now clicking on..."
✅ **Pause for questions**: "Does this make sense so far?"
✅ **Use their terminology**: If they say "providers" instead of "physicians," match their language
✅ **Relate to their specific situation**: "You mentioned your screening rate is X%, this would help you get to Y%"
✅ **Show enthusiasm**: Your energy is contagious
✅ **Take notes during demo**: Write down their questions and concerns to address in follow-up
✅ **Use patient data that tells a story**: Moderate depression is more compelling than minimal

### Don'ts
❌ **Don't read from the script**: Use it as a guide, speak naturally
❌ **Don't go over time**: Respect their schedule
❌ **Don't show features they don't care about**: Stay focused on their pain points
❌ **Don't get too technical** (unless they want it): Match their level
❌ **Don't trash competitors**: Position yourself positively
❌ **Don't apologize for what you don't have**: Focus on what you do have
❌ **Don't leave next steps vague**: Always schedule the next meeting before you hang up

### Handling Technical Glitches
If something doesn't work during the demo:
1. **Stay calm**: "Looks like we have a connectivity issue. Let me try this alternative..."
2. **Have backup**: Screenshots or video recording ready
3. **Move on quickly**: Don't spend 5 minutes troubleshooting live
4. **Offer to show it later**: "I'll record this specific feature and send it to you after our call"

### Reading the Room (Virtual Edition)
- **Lots of questions** = Engaged and interested → Spend more time, answer thoroughly
- **Silent** = Could be thinking or lost → Pause and ask "What questions do you have?"
- **Checking email** (you can tell) = Losing them → Ask a direct question to re-engage
- **Taking notes** = Very interested → Slow down, give them time
- **Multiple people join late** = Recap quickly for them

---

## Demo Variations by Audience

### For Clinical Audiences (CMOs, Quality Directors)
**Focus**:
- Clinical outcomes (depression remission rates)
- Evidence-based algorithms
- Patient safety and care quality
- Physician satisfaction

**Language**:
- "Evidence-based"
- "Clinical guidelines"
- "Patient outcomes"
- "Quality of care"

### For Technical Audiences (CMIOs, CTOs)
**Focus**:
- FHIR integration architecture
- Security and compliance
- Scalability and performance
- Integration with existing systems

**Language**:
- "FHIR R4 resources"
- "RESTful APIs"
- "Authentication/authorization"
- "Deployment architecture"

### For Financial Audiences (CFOs, VBC Leaders)
**Focus**:
- ROI and time savings
- Quality bonus capture
- Risk adjustment
- Contract performance

**Language**:
- "Return on investment"
- "Cost savings"
- "Revenue opportunity"
- "Financial impact"

### For Operational Audiences (COOs, Practice Managers)
**Focus**:
- Workflow efficiency
- Staff time savings
- Implementation timeline
- Training requirements

**Language**:
- "Workflow integration"
- "Staff productivity"
- "Quick wins"
- "Ease of use"

---

## Success Metrics to Track

### Demo Effectiveness
- % of demos that lead to pilot discussions
- % of demos that lead to reference requests
- % of demos that lead to technical deep-dives
- Average time from demo to pilot agreement

### Questions to Note
Track what questions prospects ask most frequently:
- Update demo script to proactively address them
- Create FAQ document
- Develop additional materials to address common concerns

### Competitive Intelligence
Note when prospects mention competitors:
- Which competitors are you hearing about?
- What are they comparing you to?
- What features do they ask for that you don't have?

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Next Review**: Weekly during active sales process

---

*Remember: The best demos are conversations, not presentations. Listen more than you talk, and show them exactly what they need to see to make a decision.*
