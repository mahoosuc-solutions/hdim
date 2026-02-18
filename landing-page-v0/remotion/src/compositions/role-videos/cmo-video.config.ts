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
      'Real-time HEDIS measure visibility — from board room to bedside',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Health plan executives wait quarters for quality data — by then it\'s too late to course-correct',
    metric: '$millions in lost Medicare Advantage bonus payments from delayed visibility',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/cmo/cmo-01-dashboard.png',
      narrativeCaption:
        'Executive dashboard — organization-wide quality performance at a glance',
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
        'All HEDIS measures with pass/fail rates — CDC, BCS, CBP, and 20+ more',
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
        'CDC measure detail — numerator/denominator breakdown, performance against benchmark',
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
        'CQL evaluation results — automated clinical logic replaces weeks of manual chart review',
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
        'Side-by-side measure comparison — identify underperforming measures instantly',
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
        'Care gap summary by measure — see exactly where quality improvement efforts should focus',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 35 },
          props: { width: 500, height: 60, borderColor: 'red', pulseCount: 2 },
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
        'Risk stratification — high-risk cohorts identified for targeted interventions',
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
        'QRDA III reports ready for CMS submission — compliant export in one click',
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
        'Quality constellation — visual map of all measures showing relative performance',
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
        'AI-powered insights surface hidden patterns and recommend priority interventions',
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
    headline: 'Real-Time Quality Visibility',
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
        value: 'CMS-Ready Reports',
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
