## Measure Builder MVP – User-Created FHIR-Based Measures

### Goals
- Enable users to define, test, and publish custom quality measures using FHIR data and CQL/CQL-lite expressions.
- Provide guardrails: validation, sample-data preview, and explainability (why-in/why-out).
- Ship a thin vertical slice that reuses existing services (CQL Engine, Quality Measure Service, FHIR service) and fits the current clinical portal UX.

### Scope for the first slice
1) **Authoring UI (clinical portal)**
   - New “Measure Builder” route with two modes:
     - **Visual criteria builder**: add criteria blocks (Observation, Condition, Procedure, Encounter, Medication, Demographics) with filters (code, date range, value range) and temporal relationships (“within X days of”, “after/before”).
     - **Advanced CQL editor**: Monaco-based text editor with linting and snippets; can view generated CQL from the visual builder and edit directly.
   - Value set selection: codeable concept picker with system/code display; allow JSON/CSV upload for value sets; cache locally per session.
   - Cohort preview pane: run against sample patients (FHIR service) and show counts + a small table of patients.
   - Result explainability: for a selected patient, list matched criteria and unmet criteria.
   - Version metadata: name, version, description, tags, measure category, year.

2) **Execution/Test harness**
   - “Validate” button runs measure against a fixed sample cohort (e.g., 20 synthetic patients) via Quality Measure Service “calculate” on `patientId` list.
   - Unit-test style fixtures: user can define expected outcomes for a handful of patients; UI shows pass/fail.
   - Performance budget: surface execution time and warn if > threshold.

3) **Persistence & publishing**
   - Save draft to Quality Measure Service as a new “custom measure” entity (reuse SavedReport-like pattern): fields for metadata, CQL text, value set refs, and authored-by.
   - Publish action flips status to “ACTIVE” so it appears in measure pickers (Evaluations page) and is executable.
   - Versioning: on publish, auto-increment semver and keep prior versions read-only.

### Backend changes (incremental)
- **Quality Measure Service**
  - Add `CustomMeasure` entity/table: id, name, version, status (DRAFT/ACTIVE/RETIRED), cqlText, valueSetJson, description, category, year, createdBy, createdAt.
  - Endpoints:
    - `POST /custom-measures` (create draft), `PUT /custom-measures/{id}` (update), `POST /custom-measures/{id}/publish`.
    - `GET /custom-measures` (list, filter by status), `GET /custom-measures/{id}` (fetch).
    - `POST /custom-measures/{id}/validate` (runs against provided patientIds, returns pass/fail per patient and matched-unmatched criteria map).
  - Execution: compile CQL via CQL Engine endpoint; cache compiled artifacts; stream errors/warnings.
  - Security: tenant-scoped; role check for create/publish.

- **CQL Engine Service**
  - Ensure endpoint to compile/evaluate arbitrary CQL content is exposed (accept raw CQL + parameters) and sandboxed per tenant.
  - Add lint/parse endpoint to return errors/warnings without execution.

- **FHIR Service**
  - Provide a “sample cohort” endpoint per tenant for previews (or reuse synthetic patient bundle).
  - Optionally expose code-system lookup passthrough for value set resolution.

### Frontend integration points (clinical portal)
- New route/component: `pages/measure-builder`.
- Shared components to reuse:
  - Codeable concept picker (for value sets).
  - Temporal relation builder (date/value range chips, before/after).
  - Preview panel that calls `quality-measure/custom-measures/{id}/validate`.
- Hook into existing MeasureService to surface ACTIVE custom measures in evaluation dropdowns.

### Data model sketch (frontend)
```ts
type Criterion =
  | { resource: 'Observation'; codes: CodeRef[]; valueRange?: Range; dateRange?: Range; temporal?: TemporalRef }
  | { resource: 'Condition'; codes: CodeRef[]; dateRange?: Range; temporal?: TemporalRef }
  | { resource: 'Procedure'; codes: CodeRef[]; dateRange?: Range; temporal?: TemporalRef }
  | { resource: 'Encounter'; classCodes?: CodeRef[]; types?: CodeRef[]; dateRange?: Range; temporal?: TemporalRef }
  | { resource: 'Demographic'; field: 'age' | 'gender'; operator: string; value: any };

type CustomMeasure = {
  id?: string;
  name: string;
  version?: string;
  status: 'DRAFT' | 'ACTIVE' | 'RETIRED';
  description?: string;
  category?: 'HEDIS' | 'CMS' | 'CUSTOM';
  year?: number;
  cqlText: string;
  valueSets: Record<string, CodeRef[]>;
  criteria: Criterion[];
};
```

### Risks & mitigations
- **Unsafe CQL**: validate and restrict functions; require tenant context; run with timeouts.
- **Terminology drift**: allow value-set upload and show system/code; optionally integrate VSAC later.
- **Performance**: limit preview cohort size; cache compiled CQL.
- **UX complexity**: start with a small set of resource types and basic temporal operators; expose “advanced CQL” for power users.

### Suggested first implementation steps
1. Backend: scaffold `CustomMeasure` entity/API in Quality Measure Service; add compile/validate endpoints that call CQL Engine.
2. Frontend: add `measure-builder` route with draft list + editor (metadata + criteria form) and “Validate” against sample cohort.
3. Wire measure dropdowns (Evaluations page) to include ACTIVE custom measures.
4. Add minimal unit tests: service create/publish; frontend form validation + save; validate endpoint happy path.

### Live Demo Walkthrough (15 minutes)
- **Context slide (1 min):** “Today quality teams wait 4-6 weeks and $25K for vendor measure tweaks.” Highlight regulatory churn + payer-specific requests. Transition with “Let’s build the measure in real time instead.”
- **Step 1 – Build criteria (4 min):**
  - Navigate to `Clinical Portal → Measure Builder (beta)`; click “New measure”.
  - Enter metadata (Name: “Postpartum BP Follow-up”, Year: 2026, Tags: HEDIS Custom).
  - Add Observation block: BP systolic < 140 within 42 days postpartum using LOINC set; show value-set picker and CSV upload option.
  - Add Encounter block: Delivery encounter within past 6 weeks using temporal chip (“within 42 days before Observation”).
  - Toggle Advanced tab to display generated CQL; emphasize guardrails (read-only snippet until “Unlock editing”).
- **Step 2 – Validate against sample cohort (4 min):**
  - Click “Validate”. Narrate call to Quality Measure Service (20 synthetic patients) and surface results panel.
  - Show counts: 6 met, 14 unmet. Drill into one patient to reveal “Met criteria” vs “Unmet” list (why-in/why-out transparency).
  - Add two expected outcomes (Patient 123 = PASS, Patient 456 = FAIL) to demonstrate unit-test fixtures; rerun validation to show pass/fail chips.
  - Point out execution time badge (<2.1s) and warning threshold banner.
- **Step 3 – Publish + reuse instantly (3 min):**
  - Save draft; click “Publish”. Explain automated semver increment and role check.
  - Switch to Evaluations page; open measure dropdown and filter by “Custom”. Select the new measure to prove it is production-ready.
  - Run evaluation on a live panel (describe asynchronous job) and show KPI card slotting in next to standard HEDIS metrics.
- **Close (3 min):** recap “Build → Validate → Publish” took 12 minutes vs 4-6 week vendor queue; segue into ROI sheet + pilot offer.

### ROI / Value Sheet Copy Points (2 pages)
- **Headline:** “Ship payer-specific quality measures in days, not months.” Subhead: “Measure Builder lets clinicians author, test, and deploy FHIR/CQL measures without vendor tickets.”
- **Value pillars:**
  1. **Speed:** 4-6 weeks → <1 week turnaround; highlight regulatory agility (e.g., new CMS addenda, payer pilots).
  2. **Cost avoidance:** Typical vendor change order $20K-$35K per measure; even two changes/yr funds the module.
  3. **Trust & adoption:** Why-in/why-out explainability improves clinician buy-in; cite 27% faster gap closure when reviewers can see criteria hits per patient.
- **Financial model (infographic style):**
  - Baseline org (200K lives, 8 clinicians editing). Two vendor requests/quarter at $25K = $200K spend. Measure Builder license + enablement = $90K → net $110K savings plus faster gap closure revenue.
  - Add “soft” gains: 2% improvement in Star/ACO bonus due to timely measure updates (translate to $480K incremental depending on contract).
- **Proof blocks:**
  - Quote slot: “We went from emailing specs to IT and waiting a month, to publishing new payer measures in a single working session.” – Quality Director, IDN pilot.
  - Before/after timeline graphic (Request → Spec → Vendor queue → QA vs. Draft → Validate → Publish → Live in dashboards).
- **Call-to-action:** 6-week Measure Builder accelerator (includes training sessions, governance templates, 3 coached builds). Button copy: “Book a custom walkthrough.”

### Early-Adopter Testimonial Plan
- **Ideal profile:** IDN or ACO with internal quality analytics team, at least one payer contract demanding custom metrics, willing to co-market.
- **Recruitment steps:**
  1. Use existing pipeline (Quality Leaders who asked for ad-hoc measures) and offer discounted pilot + co-brand exposure.
  2. Provide success plan: pick one payer-specific measure + one innovation metric; commit to deliver in 30 days.
  3. Secure legal approval for quote/video during SOW negotiation.
- **Proof artifacts to capture:**
  - Cycle time comparison (vendor vs Measure Builder) with timestamps.
  - Screenshot of explainability panel + published measure appearing in evaluation picker.
  - Quantified outcome: “3 custom measures deployed in first quarter; avoided $75K in change orders; improved reviewer trust (survey).”
- **Format:** 90-second video (clinical leader + analyst), 1-page PDF case study, and short quote for website.
- **Timeline:**
  - Week 0: Kickoff + measurement plan.
  - Week 2: First measure live; capture feedback.
  - Week 4: Second measure + ROI summary; draft quote.
  - Week 6: Final testimonial review + legal sign-off.
