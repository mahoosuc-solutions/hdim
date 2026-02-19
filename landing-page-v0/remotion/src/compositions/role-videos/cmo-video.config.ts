import { RoleStoryConfig } from '../../types/role-story.types';

export const cmoConfig: RoleStoryConfig = {
  role: {
    title: 'CMO / VP Quality',
    subtitle: 'Star Ratings at a Glance',
    accentColor: '#3B82F6', // Royal blue — authority / trust
  },

  titleSlide: {
    headline: '"How Are Our\nStar Ratings?"',
    subheadline:
      'A CMO sees Star Rating impact in 5 seconds — not after next quarter\'s 90-day lag',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Health plan executives wait 90+ days for quality data — by then it\'s too late to course-correct',
    metric: 'Plans forfeit $1,200 per member annually from 90-day quality data lag',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/cmo/cmo-01-dashboard.png',
      narrativeCaption:
        'Executive dashboard aggregates 24 HEDIS measures across 4,821 members in real time',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Real-time Quality Overview', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-02-quality-measures.png',
      narrativeCaption:
        'Measure catalog lists 24 active HEDIS measures with pass rates from 62% to 94%',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 70, y: 8 },
          props: {
            text: '24 Active Measures',
            backgroundColor: 'rgba(59, 130, 246, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-03-cdc-measure-detail.png',
      narrativeCaption:
        'CDC detail shows 412/487 members compliant, yielding 84.6% against the 80% benchmark',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 30 },
          props: { width: 450, height: 100, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-04-evaluation-results.png',
      narrativeCaption:
        'CQL engine evaluates 487 eligible members in 4.2 seconds, replacing weeks of chart review',
      panDirection: 'right',
      zoomLevel: 1.06,
      overlays: [
        {
          type: 'metric',
          startFrame: 30,
          duration: 140,
          position: { x: 60, y: 20 },
          props: { from: 0, to: 87, suffix: '% Pass Rate', fontSize: '2.2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-05-measure-comparison.png',
      narrativeCaption:
        'Side-by-side comparison reveals CDC at 84.6% outperforms BCS at 72.1% by 12.5 points',
      panDirection: 'none',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'text',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 65 },
          props: { text: 'CDC outperforming BCS by 12%', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-06-care-gap-summary.png',
      narrativeCaption:
        'Gap summary flags BCS with 45 open gaps — the largest quality improvement opportunity',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 35 },
          props: { width: 500, height: 60, borderColor: 'green', pulseCount: 2 },
        },
        {
          type: 'badge',
          startFrame: 55,
          duration: 115,
          position: { x: 65, y: 28 },
          props: {
            text: 'BCS: 45 Open Gaps',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-07-risk-stratification.png',
      narrativeCaption:
        'Risk engine identifies 142 high-risk members from 4,821 for targeted clinical outreach',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 18 },
          props: { from: 0, to: 142, suffix: ' High Risk', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-08-reports.png',
      narrativeCaption:
        'QRDA III export packages 24 measures into 1 CMS-compliant XML file in a single click',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'badge',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 12 },
          props: {
            text: 'CMS-Ready Export',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-09-quality-constellation.png',
      narrativeCaption:
        'Quality constellation maps 24 measures by performance, projecting Star Rating from 3.5 to 4.0',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'text',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 68 },
          props: { text: 'Star Rating trajectory: 3.5 → 4.0', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/cmo/cmo-10-ai-insights.png',
      narrativeCaption:
        'AI analysis surfaces 3 priority interventions targeting BCS, CBP, and CDC for Star Rating lift',
      panDirection: 'none',
      zoomLevel: 1.0,
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 55, y: 15 },
          props: {
            text: 'AI Recommendation',
            backgroundColor: 'rgba(139, 92, 246, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
        {
          type: 'text',
          startFrame: 50,
          duration: 120,
          position: { x: 55, y: 55 },
          props: { text: 'Focus on BCS — highest ROI intervention', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'See Star Ratings Move in Real Time',
    highlightText: 'Not Quarterly Spreadsheets',
    stats: [
      {
        value: '4.0★ Star Rating',
        backgroundColor: 'rgba(251, 191, 36, 0.95)',
        borderColor: 'rgba(251, 191, 36, 1)',
        textColor: '#78350F',
        glowColor: 'rgba(251, 191, 36, 0.6)',
      },
      {
        value: '24 HEDIS Measures',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
      {
        value: '90% Faster Reporting',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
    ],
    ctaText: 'See Your Star Ratings Today',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540,
  },
};
