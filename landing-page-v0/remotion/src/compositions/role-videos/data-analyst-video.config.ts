import { RoleStoryConfig } from '../../types/role-story.types';

export const dataAnalystConfig: RoleStoryConfig = {
  role: {
    title: 'Data Analyst',
    subtitle: 'Population Health Trends',
    accentColor: '#F59E0B', // Amber — data / insight
  },

  titleSlide: {
    headline: '"Where Are the\nHidden Patterns?"',
    subheadline:
      'Population-level analytics surface systemic quality gaps before they become crises',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Health plan analysts juggle 15+ data sources to build a single population report — inconsistent, delayed, and incomplete',
    metric: '3-4 weeks to produce a quarterly population health report',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/data-analyst/data-analyst-01-dashboard-analytics.png',
      narrativeCaption:
        'Analytics dashboard — population health KPIs with trend lines and benchmarks',
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Population Health Overview', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-02-risk-stratification.png',
      narrativeCaption:
        'Risk stratification distribution — bell curve showing patient risk tiers across the population',
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 15 },
          props: { from: 0, to: 4821, suffix: ' Members', fontSize: '2rem', glowOnComplete: true },
        },
        {
          type: 'badge',
          startFrame: 55,
          duration: 115,
          position: { x: 60, y: 42 },
          props: {
            text: '12% High Risk',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-03-high-risk-cohort.png',
      narrativeCaption:
        'High-risk cohort detail — drill into demographics, conditions, and utilization patterns',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 28 },
          props: { width: 600, height: 80, borderColor: 'red', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-04-quality-constellation.png',
      narrativeCaption:
        'Quality constellation — visual map showing all measures and their relative performance',
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 68 },
          props: { text: '24 measures mapped — 4 below threshold', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-05-measure-matrix.png',
      narrativeCaption:
        'Measure matrix heatmap — cross-tabulate measures by demographics to find disparities',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 25, y: 35 },
          props: { width: 200, height: 200, borderColor: 'red', pulseCount: 2 },
        },
        {
          type: 'badge',
          startFrame: 55,
          duration: 115,
          position: { x: 65, y: 25 },
          props: {
            text: 'Disparity Detected',
            backgroundColor: 'rgba(245, 158, 11, 0.95)',
            color: '#78350F',
            fontSize: '1.2rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-06-flow-network.png',
      narrativeCaption:
        'Flow network visualization — patient journeys through care pathways',
      overlays: [
        {
          type: 'text',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 10 },
          props: { text: 'Care Pathway Analysis', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-07-evaluation-results.png',
      narrativeCaption:
        'Evaluation results table — sortable, filterable, exportable for deep analysis',
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 18 },
          props: { from: 0, to: 2847, suffix: ' Evaluations', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-08-scheduled-reports.png',
      narrativeCaption:
        'Scheduled reports — automated weekly/monthly delivery to stakeholders',
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 12 },
          props: {
            text: 'Auto-Scheduled',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-09-report-builder.png',
      narrativeCaption:
        'Custom report builder — drag-and-drop measures, filters, and visualizations',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 25 },
          props: { width: 400, height: 300, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-10-ai-insights.png',
      narrativeCaption:
        'AI-powered insights — machine learning surfaces hidden correlations in quality data',
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 55, y: 12 },
          props: {
            text: 'AI Pattern Detection',
            backgroundColor: 'rgba(139, 92, 246, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 60 },
          props: { text: 'Hispanic cohort 23% lower BCS rate — outreach recommended', fontSize: '1.2rem' },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'Population Insights in Real-Time',
    highlightText: 'Not Quarterly Reports',
    stats: [
      {
        value: '4,821 Members Analyzed',
        backgroundColor: 'rgba(245, 158, 11, 0.95)',
        borderColor: 'rgba(245, 158, 11, 1)',
        textColor: '#78350F',
        glowColor: 'rgba(245, 158, 11, 0.6)',
      },
      {
        value: 'Disparity Detection',
        backgroundColor: 'rgba(139, 92, 246, 0.95)',
        borderColor: 'rgba(139, 92, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(139, 92, 246, 0.6)',
      },
      {
        value: 'Automated Reports',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
    ],
    ctaText: 'Unlock Your Population Data',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540,
  },
};
