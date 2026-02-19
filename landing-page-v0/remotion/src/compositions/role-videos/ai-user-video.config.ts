import { RoleStoryConfig } from '../../types/role-story.types';

export const aiUserConfig: RoleStoryConfig = {
  role: {
    title: 'AI User',
    subtitle: 'AI-Accelerated Clinical Workflows',
    accentColor: '#A855F7', // Purple — AI / intelligence
  },

  titleSlide: {
    headline: '"What Can AI Do\nFor My Patients?"',
    subheadline:
      'Natural language queries replace complex database searches — 100+ patients processed instantly',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Clinicians process 100+ patients daily with fragmented tools — cognitive overload leads to burnout and missed interventions',
    metric: '62% of clinicians report burnout from administrative data burden',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/ai-user/ai-user-01-ai-assistant.png',
      narrativeCaption:
        'AI assistant opens with 4 suggested clinical queries spanning care gaps, risk, and HEDIS measures',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'AI Clinical Assistant', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-02-natural-language-query.png',
      narrativeCaption:
        'Clinician types "Which diabetic patients are overdue for HbA1c?" — AI parses the CDC measure in 0.8 seconds',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 10, y: 55 },
          props: { width: 700, height: 50, borderColor: 'blue', pulseCount: 2, borderRadius: 12 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-03-ai-response.png',
      narrativeCaption:
        'AI returns 23 patients with HbA1c gaps — each row includes last test date, days overdue, and risk tier',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'metric',
          startFrame: 20,
          duration: 150,
          position: { x: 60, y: 15 },
          props: { from: 0, to: 23, suffix: ' Patients Found', fontSize: '2.2rem', glowOnComplete: true },
        },
        {
          type: 'badge',
          startFrame: 55,
          duration: 115,
          position: { x: 60, y: 42 },
          props: {
            text: 'AI-Generated List',
            backgroundColor: 'rgba(168, 85, 247, 0.95)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-04-care-recommendations.png',
      narrativeCaption:
        'AI ranks 5 evidence-based interventions by clinical impact for the 23 identified patients',
      panDirection: 'right',
      zoomLevel: 1.06,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 8 },
          props: {
            text: 'Evidence-Based',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-05-measure-builder.png',
      narrativeCaption:
        'CQL measure builder generates 120 lines of clinical logic from a 2-sentence natural language prompt',
      panDirection: 'none',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 10, y: 25 },
          props: { width: 500, height: 300, borderColor: 'blue', pulseCount: 2 },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 68 },
          props: { text: 'AI-Assisted CQL Generation', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-06-agent-builder.png',
      narrativeCaption:
        'Agent builder creates custom AI agents — 3 pre-built templates for care gaps, risk, and outreach',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Custom AI Agents', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-07-knowledge-base.png',
      narrativeCaption:
        'Knowledge base indexes 24 HEDIS specs, 150+ CQL libraries, and 2,000 clinical guidelines',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 12 },
          props: {
            text: 'HEDIS + CQL + Guidelines',
            backgroundColor: 'rgba(168, 85, 247, 0.95)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-08-insights-dashboard.png',
      narrativeCaption:
        'AI insight detects rural patients are 2.3x less likely to complete BCS screenings across 3 counties',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'text',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 65 },
          props: { text: 'Pattern: Rural patients 2.3x less likely to complete screenings', fontSize: '1.2rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-09-constellation-ai.png',
      narrativeCaption:
        'AI highlights 4 underperforming measures on the quality constellation — BCS flagged as top priority',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 30, y: 35 },
          props: { width: 80, height: 80, borderColor: 'green', pulseCount: 3, borderRadius: 40 },
        },
        {
          type: 'badge',
          startFrame: 55,
          duration: 115,
          position: { x: 60, y: 28 },
          props: {
            text: 'AI: Needs Attention',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/ai-user/ai-user-10-pre-visit-summary.png',
      narrativeCaption:
        'AI generates 1-page FHIR-based pre-visit summary in 30 seconds — replacing 15 minutes of manual chart review',
      panDirection: 'none',
      zoomLevel: 1.0,
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 55, y: 10 },
          props: {
            text: 'AI-Generated Summary',
            backgroundColor: 'rgba(168, 85, 247, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 68 },
          props: { text: '30 seconds to review vs. 15 minutes manually', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'AI-Powered Clinical Intelligence',
    highlightText: 'Reducing Burden, Not Adding To It',
    stats: [
      {
        value: '23 Patients in 0.8s',
        backgroundColor: 'rgba(168, 85, 247, 0.95)',
        borderColor: 'rgba(168, 85, 247, 1)',
        textColor: 'white',
        glowColor: 'rgba(168, 85, 247, 0.6)',
      },
      {
        value: '90% Less Manual Search',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
      {
        value: '2,000+ Guidelines',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
    ],
    ctaText: 'Experience AI-Powered Healthcare',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540,
  },
};
