# UTM Parameter Guide: HDIM LinkedIn Campaign

## UTM Structure

All LinkedIn ads should use this UTM parameter structure for proper tracking in Google Analytics and Zoho CRM.

---

## Standard UTM Template

```
https://hdim.com/demo?utm_source=linkedin&utm_medium=paid_social&utm_campaign=[CAMPAIGN_NAME]&utm_content=[AD_VARIANT]&utm_term=[AUDIENCE_SEGMENT]
```

---

## UTM Parameters Explained

### utm_source (Required)
**Value**: `linkedin`
**Why**: Identifies traffic source

### utm_medium (Required)
**Value**: `paid_social`
**Why**: Distinguishes paid LinkedIn from organic LinkedIn

### utm_campaign (Required)
**Format**: `linkedin_[objective]_[month]_[year]`

**Examples**:
- `linkedin_leadgen_jan_2025`
- `linkedin_leadgen_q1_2025`
- `linkedin_retargeting_feb_2025`

**Why**: Groups all ads in this 90-day campaign

### utm_content (Recommended)
**Format**: `[variant]_[format]`

**Examples**:
- `v1_data_driven_leadgen` (Variant 1 Lead Gen Form)
- `v2_problem_focused_sponsored` (Variant 2 Sponsored Content)
- `v3_contrarian_inmail` (Variant 3 Sponsored InMail)

**Why**: Identifies which ad creative drove the click

### utm_term (Recommended)
**Format**: `[job_title]_[company_size]`

**Examples**:
- `cio_500plus` (CIOs at 500+ employee hospitals)
- `cmio_200to500` (CMIOs at 200-500 employee hospitals)
- `director_retargeting` (Directors - retargeting audience)

**Why**: Identifies which audience segment converted best

---

## Campaign-Specific UTM Examples

### Campaign 1: Lead Gen - High Intent (CIOs/CMIOs)

**Ad Set 1A: CIO - Hospitals 500+ employees**
```
utm_campaign=linkedin_leadgen_jan_2025
utm_content=v1_data_driven_leadgen
utm_term=cio_500plus
```

**Full URL**:
```
https://hdim.com/demo?utm_source=linkedin&utm_medium=paid_social&utm_campaign=linkedin_leadgen_jan_2025&utm_content=v1_data_driven_leadgen&utm_term=cio_500plus
```

---

### Campaign 2: Sponsored Content - Awareness

**Ad Set 3A: Problem-focused (all audiences)**
```
utm_campaign=linkedin_awareness_jan_2025
utm_content=v2_problem_focused_sponsored
utm_term=all_it_leaders
```

**Full URL**:
```
https://hdim.com/demo?utm_source=linkedin&utm_medium=paid_social&utm_campaign=linkedin_awareness_jan_2025&utm_content=v2_problem_focused_sponsored&utm_term=all_it_leaders
```

---

### Campaign 4: Retargeting - Warm Audience

**Ad Set 4A: Website visitors (last 30 days)**
```
utm_campaign=linkedin_retargeting_jan_2025
utm_content=v1_data_driven_retargeting
utm_term=website_visitors_30d
```

---

### Campaign 5: Sponsored InMail - C-Suite

**Message to CIOs**
```
utm_campaign=linkedin_inmail_jan_2025
utm_content=demo_invitation_inmail
utm_term=cio_targeted
```

---

## LinkedIn Campaign Manager Setup

### Where to Add UTMs in LinkedIn

1. **Lead Gen Forms**: 
   - LinkedIn captures leads in-platform (no URL redirect)
   - Add UTM parameters to "Website URL" field in form builder
   - These appear when users click "Learn More" after submitting

2. **Sponsored Content**:
   - Add UTM parameters to the "Destination URL" field
   - Example: https://hdim.com/demo?utm_source=...

3. **Sponsored InMail**:
   - Add UTM parameters to CTA button URL
   - Example: "Book Demo" button → https://hdim.com/demo?utm_source=...

4. **Document Ads**:
   - Add UTM parameters to CTA URL after document view

---

## UTM URL Builder Tool

Use this Google UTM Builder for easy URL creation:
https://ga-dev-tools.google/campaign-url-builder/

**Pre-filled Template**:
1. Website URL: https://hdim.com/demo
2. Campaign Source: linkedin
3. Campaign Medium: paid_social
4. Campaign Name: linkedin_leadgen_jan_2025
5. Campaign Content: [your ad variant]
6. Campaign Term: [your audience segment]

**Result**: Copy the generated URL into LinkedIn Campaign Manager

---

## Tracking in Google Analytics

### View Campaign Performance

**Google Analytics 4** (GA4):
1. Reports → Acquisition → Traffic Acquisition
2. Filter by: Session source/medium = linkedin / paid_social
3. Secondary dimension: Session campaign

**Google Analytics Universal** (if still using):
1. Acquisition → Campaigns → All Campaigns
2. Filter by Source/Medium: linkedin / paid_social

### Custom Report

**Metrics to Track**:
- Sessions
- Users
- Bounce Rate
- Avg. Session Duration
- Goal Completions (demo requests)
- Goal Conversion Rate

**Dimensions**:
- Campaign
- Content (ad variant)
- Term (audience segment)

---

## Tracking in Zoho CRM

### Automatic Lead Source Tracking

When LinkedIn leads are synced to Zoho CRM (via Zapier or LinkedIn integration):

**Lead Source**: `LinkedIn`
**Campaign**: Extracted from utm_campaign parameter
**Ad Variant**: Extracted from utm_content parameter
**Audience**: Extracted from utm_term parameter

### Custom Field Mapping

| UTM Parameter | Zoho CRM Field |
|--------------|----------------|
| utm_source | Lead Source |
| utm_campaign | Campaign Name |
| utm_content | Ad Variant |
| utm_term | Audience Segment |

**Set up in Zoho CRM**:
1. Settings → Modules → Leads → Fields
2. Create custom fields: "Ad Variant", "Audience Segment"
3. Map UTM parameters in integration settings

---

## URL Shortening (Optional but Recommended)

Long UTM URLs look spammy. Use a URL shortener:

**Options**:
- Bitly (branded short links)
- LinkedIn's native link shortener (automatically applied in ads)
- Custom domain shortener (e.g., hdim.link/demo-cio)

**Example**:
- Long: https://hdim.com/demo?utm_source=linkedin&utm_medium=paid_social&utm_campaign=linkedin_leadgen_jan_2025&utm_content=v1_data_driven_leadgen&utm_term=cio_500plus
- Short: https://hdim.link/demo-cio

**Note**: LinkedIn automatically shortens links in ads, but you can use custom short links for tracking external shares.

---

## Common Mistakes to Avoid

❌ **Inconsistent naming**: `LinkedIn` vs. `linkedin` vs. `LINKEDIN`
   ✅ Always use lowercase: `linkedin`

❌ **Spaces in parameters**: `utm_campaign=linkedin leadgen`
   ✅ Use underscores or hyphens: `linkedin_leadgen` or `linkedin-leadgen`

❌ **Missing parameters**: Only using utm_source and utm_medium
   ✅ Always include utm_campaign minimum

❌ **Too generic**: `utm_campaign=q1_campaign`
   ✅ Be specific: `utm_campaign=linkedin_leadgen_jan_2025`

❌ **Forgetting to test**: Not verifying UTMs in GA before launch
   ✅ Test by clicking your own ad and checking GA Real-Time report

---

## Testing Checklist

Before launch, verify:

- [ ] All ad variants have unique UTM parameters
- [ ] UTM parameters appear in LinkedIn Campaign Manager preview
- [ ] Click test ad → verify UTM parameters in browser URL bar
- [ ] Check Google Analytics Real-Time report → See session with correct source/medium/campaign
- [ ] Zoho CRM receives lead with correct campaign attribution
- [ ] URL shortener (if used) redirects correctly

---

## Reporting Dashboard

Create a weekly dashboard tracking:

| Metric | This Week | Last Week | % Change |
|--------|-----------|-----------|----------|
| LinkedIn Sessions | | | |
| Demo Requests | | | |
| Conversion Rate | | | |
| Top Ad Variant (utm_content) | | | |
| Top Audience (utm_term) | | | |
| Cost Per Lead | | | |

**Source**: Google Analytics + LinkedIn Campaign Manager + Zoho CRM

---

**Questions?** Contact marketing team for UTM parameter approval before launch.
