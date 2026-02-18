import { RoleStoryConfig } from '../../types/role-story.types';

export const careManagerConfig: RoleStoryConfig = {
  role: {
    title: 'Care Manager',
    subtitle: 'Close Care Gaps Faster',
    accentColor: '#10B981', // Emerald green — healing / resolution
  },

  titleSlide: {
    headline: '"How Do I Close\nMore Gaps Today?"',
    subheadline:
      'A care manager closes a mammography gap in 8 seconds — not 8 days',
    durationFrames: 90, // 3s
  },

  problemSlide: {
    statement:
      'Care managers spend 40% of their day hunting through spreadsheets to find which patients need outreach',
    metric: '73% of gaps take >30 days to close manually',
    durationFrames: 120, // 4s
  },

  scenes: [
    {
      screenshot: 'screenshots/care-manager/care-manager-01-dashboard-overview.png',
      narrativeCaption:
        'Morning overview — summary cards show today\'s care gap priorities at a glance',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 30,
          duration: 140,
          position: { x: 14, y: 25 },
          props: { width: 380, height: 120, borderColor: 'green', pulseCount: 2 },
        },
        {
          type: 'text',
          startFrame: 60,
          duration: 110,
          position: { x: 55, y: 12 },
          props: { text: '45 Open Care Gaps', fontSize: '1.6rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-02-care-gaps-table.png',
      narrativeCaption:
        'Full gap table sorted by urgency — highest-impact patients surface first',
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 72, y: 10 },
          props: {
            text: '9 HIGH Urgency',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.4rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-03-high-urgency-filter.png',
      narrativeCaption:
        'One-click filter isolates the 9 high-urgency gaps for immediate action',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 18 },
          props: { width: 160, height: 40, borderColor: 'red', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-04-eleanor-row.png',
      narrativeCaption:
        'Eleanor Anderson, age 63 — mammogram overdue 60 days (Breast Cancer Screening measure)',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 15,
          duration: 155,
          position: { x: 5, y: 42 },
          props: { width: 900, height: 48, borderColor: 'red', pulseCount: 3 },
        },
        {
          type: 'metric',
          startFrame: 50,
          duration: 120,
          position: { x: 72, y: 28 },
          props: { from: 0, to: 60, suffix: ' days overdue', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-05-gap-detail.png',
      narrativeCaption:
        'Gap detail shows clinical context — measure rationale, evidence period, and intervention options',
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 65 },
          props: { text: 'HEDIS BCS Measure — Mammography', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-06-intervention-dialog.png',
      narrativeCaption:
        'One-click intervention: schedule screening, mark complete, or document patient refusal',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 30, y: 40 },
          props: { width: 400, height: 50, borderColor: 'green', pulseCount: 2, borderRadius: 12 },
        },
        {
          type: 'badge',
          startFrame: 60,
          duration: 110,
          position: { x: 62, y: 32 },
          props: {
            text: 'Schedule Screening',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-07-gap-closed.png',
      narrativeCaption:
        'Gap closed in 8 seconds — total gaps drop from 45 to 44, high-urgency from 9 to 8',
      overlays: [
        {
          type: 'badge',
          startFrame: 10,
          duration: 160,
          position: { x: 55, y: 8 },
          props: {
            text: 'Gap Closed!',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.5rem',
          },
        },
        {
          type: 'metric',
          startFrame: 40,
          duration: 130,
          position: { x: 15, y: 72 },
          props: { from: 45, to: 44, suffix: ' Total Gaps', fontSize: '2rem', glowOnComplete: true },
        },
        {
          type: 'metric',
          startFrame: 65,
          duration: 105,
          position: { x: 55, y: 72 },
          props: { from: 9, to: 8, suffix: ' High Urgency', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-08-patient-detail.png',
      narrativeCaption:
        'Patient health record confirms closure — CLOSED status on BCS gap with timestamp',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 25,
          duration: 145,
          position: { x: 12, y: 55 },
          props: { width: 280, height: 40, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-09-outreach-campaigns.png',
      narrativeCaption:
        'Automated outreach campaigns handle remaining gaps — email, SMS, and letter reminders',
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Automated Outreach Active', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/care-manager/care-manager-10-dashboard-updated.png',
      narrativeCaption:
        'Real-time impact — compliance rate improved 2.1% from a single gap closure',
      overlays: [
        {
          type: 'metric',
          startFrame: 20,
          duration: 150,
          position: { x: 60, y: 20 },
          props: {
            from: 76.2,
            to: 78.3,
            suffix: '% Compliance',
            decimals: 1,
            fontSize: '2.5rem',
            glowOnComplete: true,
          },
        },
        {
          type: 'badge',
          startFrame: 70,
          duration: 100,
          position: { x: 60, y: 48 },
          props: {
            text: '+2.1% improvement',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'Close Care Gaps in Seconds',
    highlightText: 'Not Weeks',
    stats: [
      {
        value: '8.2x ROI',
        backgroundColor: 'rgba(251, 191, 36, 0.95)',
        borderColor: 'rgba(251, 191, 36, 1)',
        textColor: '#78350F',
        glowColor: 'rgba(251, 191, 36, 0.6)',
      },
      {
        value: '48% Success Rate',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
      {
        value: '30-Day Avg Closure',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
    ],
    ctaText: 'Try the Interactive Demo',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540, // 18s
  },
};
