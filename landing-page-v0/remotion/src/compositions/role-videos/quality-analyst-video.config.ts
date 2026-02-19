import { RoleStoryConfig } from '../../types/role-story.types';

export const qualityAnalystConfig: RoleStoryConfig = {
  role: {
    title: 'Quality Analyst',
    subtitle: 'Evaluate Measures, Generate Reports',
    accentColor: '#8B5CF6', // Purple — analytical / precision
  },

  titleSlide: {
    headline: '"Show Me the\nMeasure Results"',
    subheadline:
      'CQL automation replaces weeks of manual chart review with instant evaluations',
    durationFrames: 90, // 3s
  },

  problemSlide: {
    statement:
      'Quality analysts manually review thousands of charts per measure cycle — error-prone, slow, and impossible to scale',
    metric: '6-8 weeks per measure cycle using traditional chart review',
    durationFrames: 120, // 4s
  },

  scenes: [
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-01-measures-list.png',
      narrativeCaption:
        'Measure catalog displays 24 quality measures with compliance rates ranging from 62% to 94%',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: '24 Quality Measures', fontSize: '1.6rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-02-select-cdc.png',
      narrativeCaption:
        'Analyst selects CDC (Comprehensive Diabetes Care) — 1 of 24 active HEDIS measures',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 35 },
          props: { width: 500, height: 48, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-03-measure-detail.png',
      narrativeCaption:
        'Measure detail shows numerator/denominator definitions with CQL logic auto-generated from 24 HEDIS specs',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 60 },
          props: { text: 'CQL Logic: Auto-Generated', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-04-run-evaluation.png',
      narrativeCaption:
        'One-click CQL evaluation processes 2,847 eligible patients across 6 CDC sub-measures',
      panDirection: 'right',
      zoomLevel: 1.06,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 35, y: 45 },
          props: { width: 300, height: 50, borderColor: 'blue', pulseCount: 2, borderRadius: 12 },
        },
        {
          type: 'badge',
          startFrame: 50,
          duration: 120,
          position: { x: 60, y: 38 },
          props: {
            text: 'Run Evaluation',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-05-batch-processing.png',
      narrativeCaption:
        'CQL engine evaluates 2,847 patients across 6 CDC sub-measures simultaneously in 4.2 seconds',
      panDirection: 'none',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'metric',
          startFrame: 20,
          duration: 150,
          position: { x: 60, y: 25 },
          props: { from: 0, to: 2847, suffix: ' Patients', fontSize: '2.2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-06-results-pass-fail.png',
      narrativeCaption:
        'Results reveal 87% compliance rate — 2,477 of 2,847 patients pass the CDC measure',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 20 },
          props: { from: 0, to: 87, suffix: '% Pass Rate', fontSize: '2.5rem', glowOnComplete: true },
        },
        {
          type: 'badge',
          startFrame: 65,
          duration: 105,
          position: { x: 60, y: 48 },
          props: {
            text: 'Above Benchmark',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-07-non-compliant-drilldown.png',
      narrativeCaption:
        'Drilldown identifies 370 non-compliant patients with specific gaps preventing CDC closure',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 30 },
          props: { width: 600, height: 60, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-08-qrda-export.png',
      narrativeCaption:
        'QRDA III export packages 24 measures into 1 CMS-compliant XML file with validation checks',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 12 },
          props: {
            text: 'QRDA III Ready',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-09-report-builder.png',
      narrativeCaption:
        'Report builder configures 4 scheduled HEDIS reports for weekly stakeholder delivery via email',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 65 },
          props: { text: 'Scheduled: Weekly Executive Summary', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/quality-analyst/quality-analyst-10-measure-trends.png',
      narrativeCaption:
        'Trend analysis tracks CDC from 72.1% to 87.3% — a 15.2-point improvement over 6 months',
      panDirection: 'none',
      zoomLevel: 1.0,
      overlays: [
        {
          type: 'metric',
          startFrame: 20,
          duration: 150,
          position: { x: 60, y: 22 },
          props: {
            from: 72.1,
            to: 87.3,
            suffix: '% CDC Rate',
            decimals: 1,
            fontSize: '2.2rem',
            glowOnComplete: true,
          },
        },
        {
          type: 'badge',
          startFrame: 70,
          duration: 100,
          position: { x: 60, y: 50 },
          props: {
            text: '+15.2% improvement',
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
    headline: 'Evaluate Measures in Minutes',
    highlightText: 'Not Months',
    stats: [
      {
        value: '2,847 Patients/Run',
        backgroundColor: 'rgba(139, 92, 246, 0.95)',
        borderColor: 'rgba(139, 92, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(139, 92, 246, 0.6)',
      },
      {
        value: '87% Compliance',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
      {
        value: '24-Measure QRDA III',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
    ],
    ctaText: 'Automate Your Quality Reporting',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540, // 18s
  },
};
