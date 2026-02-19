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
        'AI assistant dashboard — natural language interface for clinical queries',
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
        'Ask in plain English: "Which diabetic patients are overdue for HbA1c testing?"',
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
        'AI responds with actionable patient list — 23 patients identified with clinical context',
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
        'AI care recommendations — evidence-based interventions ranked by clinical impact',
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
        'CQL measure builder — AI assists in constructing clinical quality logic',
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
        'Agent builder — create custom AI agents for specific clinical workflows',
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
        'Knowledge base — AI trained on HEDIS specifications, CQL libraries, and clinical guidelines',
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
        'AI insights dashboard — machine learning surfaces patterns humans would miss',
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
        'Quality constellation with AI highlights — underperforming measures flagged automatically',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 30, y: 35 },
          props: { width: 80, height: 80, borderColor: 'red', pulseCount: 3, borderRadius: 40 },
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
        'AI pre-visit summary — one-page clinical brief generated automatically for each patient encounter',
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
        value: 'Natural Language Queries',
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
        value: 'Evidence-Based Insights',
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
