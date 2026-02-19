import { RoleStoryConfig } from '../../types/role-story.types';

export const providerConfig: RoleStoryConfig = {
  role: {
    title: 'Provider / Physician',
    subtitle: 'Pre-Visit Patient Preparation',
    accentColor: '#06B6D4', // Cyan — clinical / precision
  },

  titleSlide: {
    headline: '"What Does My Patient\nNeed Today?"',
    subheadline:
      'Complete clinical context in 30 seconds — conditions, medications, gaps, and risk',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Physicians review fragmented data across 3-5 systems before each visit — missing gaps leads to missed interventions',
    metric: '40% of preventive care opportunities missed due to incomplete pre-visit data',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/provider/provider-01-pre-visit.png',
      narrativeCaption:
        'Pre-visit dashboard flags 12 scheduled patients with 8 open care gaps requiring action today',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Pre-Visit Planning', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-02-patient-search.png',
      narrativeCaption:
        'Search returns Michael Chen in 0.3 seconds — demographics, risk level, and 3 open gaps displayed',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 18 },
          props: { width: 500, height: 48, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-03-patient-demographics.png',
      narrativeCaption:
        'Demographics confirm Michael Chen, age 58, male — risk engine classifies as high-risk tier',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 70, y: 12 },
          props: {
            text: 'High Risk',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-04-conditions.png',
      narrativeCaption:
        'Active conditions list 3 chronic diagnoses: Type 2 Diabetes, Hypertension, and Hyperlipidemia',
      panDirection: 'right',
      zoomLevel: 1.06,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 28 },
          props: { width: 550, height: 120, borderColor: 'red', pulseCount: 2 },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 65 },
          props: { text: 'T2DM + HTN + HLD — triple therapy candidate', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-05-medications.png',
      narrativeCaption:
        'Medication list shows 6 active prescriptions: Metformin 1000mg, Lisinopril 20mg, Atorvastatin 40mg',
      panDirection: 'none',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: '6 Active Medications', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-06-care-gaps.png',
      narrativeCaption:
        'Patient-level gaps identify 3 overdue screenings: HbA1c, retinal exam, and kidney function',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 65, y: 10 },
          props: {
            text: '3 Open Gaps',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.4rem',
          },
        },
        {
          type: 'glow-highlight',
          startFrame: 40,
          duration: 130,
          position: { x: 8, y: 32 },
          props: { width: 600, height: 90, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-07-care-recommendations.png',
      narrativeCaption:
        'AI ranks 5 evidence-based interventions by clinical impact — HbA1c test tops the list',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 8 },
          props: {
            text: 'AI Recommendations',
            backgroundColor: 'rgba(139, 92, 246, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-08-risk-profile.png',
      narrativeCaption:
        'Risk profile scores HCC at 2.4, predicting 1.8x utilization against the population average',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 22 },
          props: { from: 0, to: 2.4, suffix: ' HCC Score', decimals: 1, fontSize: '2.2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-09-ai-assistant.png',
      narrativeCaption:
        'AI assistant answers "What screenings is Michael overdue for?" with 3 specific recommendations',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 50 },
          props: { width: 600, height: 60, borderColor: 'green', pulseCount: 2, borderRadius: 12 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-10-longitudinal-view.png',
      narrativeCaption:
        'Longitudinal view tracks HbA1c trending from 9.2% to 7.4% over 18 months across 6 visits',
      panDirection: 'none',
      zoomLevel: 1.0,
      overlays: [
        {
          type: 'text',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 68 },
          props: { text: 'HbA1c trending down: 9.2% → 7.4% over 18 months', fontSize: '1.4rem' },
        },
        {
          type: 'badge',
          startFrame: 60,
          duration: 110,
          position: { x: 55, y: 12 },
          props: {
            text: 'Improving Trend',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'Complete Clinical Context',
    highlightText: 'In 30 Seconds',
    stats: [
      {
        value: '40% Fewer Missed Gaps',
        backgroundColor: 'rgba(6, 182, 212, 0.95)',
        borderColor: 'rgba(6, 182, 212, 1)',
        textColor: 'white',
        glowColor: 'rgba(6, 182, 212, 0.6)',
      },
      {
        value: '14 Resource Types',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
      {
        value: 'FHIR R4 Native',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
    ],
    ctaText: 'Prepare for Every Visit',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540,
  },
};
