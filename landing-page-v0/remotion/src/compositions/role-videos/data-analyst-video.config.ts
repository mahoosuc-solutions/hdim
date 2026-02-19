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
        'Analytics dashboard tracks 4,821 members across 24 HEDIS measures with real-time trend lines',
      panDirection: 'left',
      zoomLevel: 1.03,
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
        'Risk stratification segments 4,821 members into 5 tiers — 12% classified as high-risk',
      panDirection: 'right',
      zoomLevel: 1.05,
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
        'Analyst drills into the 578-member high-risk cohort by demographics, top 5 clinical conditions, and utilization rates',
      panDirection: 'left',
      zoomLevel: 1.02,
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
        'Quality constellation maps 24 measures by performance — 4 fall below the 75% benchmark threshold',
      panDirection: 'right',
      zoomLevel: 1.06,
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
        'Heatmap cross-tabulates 24 measures by 6 demographic groups, revealing a 23% BCS disparity',
      panDirection: 'none',
      zoomLevel: 1.04,
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
        'Flow network traces 1,200 patient journeys through 8 care pathways to identify drop-off points',
      panDirection: 'left',
      zoomLevel: 1.03,
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
        'HEDIS evaluation results table exports 2,847 records as CSV with 14 sortable and filterable columns',
      panDirection: 'right',
      zoomLevel: 1.05,
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
        'Report scheduler delivers 4 automated quality measure reports weekly to 12 stakeholders via email',
      panDirection: 'left',
      zoomLevel: 1.02,
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
        'Report builder assembles 6 HEDIS visualizations from 24 measures using drag-and-drop configuration',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 25 },
          props: { width: 400, height: 300, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/data-analyst/data-analyst-10-ai-insights.png',
      narrativeCaption:
        'AI detects Hispanic cohort has 23% lower BCS rate — recommends targeted outreach for 142 members',
      panDirection: 'none',
      zoomLevel: 1.0,
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
        value: '23% Disparity Found',
        backgroundColor: 'rgba(139, 92, 246, 0.95)',
        borderColor: 'rgba(139, 92, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(139, 92, 246, 0.6)',
      },
      {
        value: '4 Weekly Reports',
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
