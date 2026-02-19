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
        'Pre-visit planning dashboard — today\'s scheduled patients with open gaps flagged',
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
        'Search for Michael Chen — instant results with demographics and risk level',
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
        'Patient demographics — age 58, male, primary language English, insurance verified',
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
        'Active conditions — Type 2 Diabetes, Hypertension, Hyperlipidemia with onset dates',
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
        'Current medications — Metformin, Lisinopril, Atorvastatin with dosages and refill dates',
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
        'Patient-level care gaps — HbA1c overdue, eye exam needed, kidney screening due',
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
          props: { width: 600, height: 90, borderColor: 'red', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-07-care-recommendations.png',
      narrativeCaption:
        'AI-generated care recommendations — evidence-based interventions prioritized by impact',
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
        'Risk profile — HCC score, predicted utilization, and social determinants of health',
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
        'Ask the AI assistant — "What screenings is Michael overdue for?"',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 50 },
          props: { width: 600, height: 60, borderColor: 'blue', pulseCount: 2, borderRadius: 12 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/provider/provider-10-longitudinal-view.png',
      narrativeCaption:
        'Longitudinal health view — 3-year trajectory with labs, visits, and interventions',
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
