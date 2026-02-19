import { RoleStoryConfig } from '../../types/role-story.types';

export const adminConfig: RoleStoryConfig = {
  role: {
    title: 'Administrator',
    subtitle: 'Security & HIPAA Compliance',
    accentColor: '#EF4444', // Red — security / critical
  },

  titleSlide: {
    headline: '"Is Our Platform\nHIPAA Compliant?"',
    subheadline:
      'Multi-tenant isolation, audit trails, and RBAC — all verifiable in real-time',
    durationFrames: 90,
  },

  problemSlide: {
    statement:
      'Healthcare IT admins manage HIPAA compliance manually — audit logs scattered, access controls inconsistent, and tenant isolation unverified',
    metric: '$1.5M average HIPAA breach penalty — non-compliance is existential risk',
    durationFrames: 120,
  },

  scenes: [
    {
      screenshot: 'screenshots/admin/admin-01-admin-dashboard.png',
      narrativeCaption:
        'Admin dashboard monitors 51 services, 47 active users, and 3 HIPAA compliance controls in real time',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'System Administration', fontSize: '1.5rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-02-tenant-settings.png',
      narrativeCaption:
        'Tenant isolation enforces database-level separation for 4 health plans with zero cross-tenant leakage',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 10 },
          props: {
            text: 'Tenant Isolation: ACTIVE',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-03-user-management.png',
      narrativeCaption:
        'HIPAA-compliant user management lists 47 active accounts across 6 roles with last-login timestamps',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 15 },
          props: { from: 0, to: 47, suffix: ' Active Users', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-04-role-assignment.png',
      narrativeCaption:
        'RBAC assigns 1 of 6 roles per user with 24 granular permissions per HIPAA §164.312(a)(1)',
      panDirection: 'right',
      zoomLevel: 1.06,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 12, y: 30 },
          props: { width: 450, height: 200, borderColor: 'blue', pulseCount: 2 },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 68 },
          props: { text: 'HIPAA §164.312(a)(1) — Access Control', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-05-audit-logs.png',
      narrativeCaption:
        'Audit trail logs 100% of PHI access events — 12,847 entries this month with user, timestamp, and action',
      panDirection: 'none',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 60, y: 8 },
          props: {
            text: 'HIPAA §164.312(b) Audit Controls',
            backgroundColor: 'rgba(239, 68, 68, 0.9)',
            color: 'white',
            fontSize: '1.1rem',
          },
        },
        {
          type: 'glow-highlight',
          startFrame: 40,
          duration: 130,
          position: { x: 5, y: 25 },
          props: { width: 900, height: 250, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-06-audit-search.png',
      narrativeCaption:
        'Audit search filters 12,847 PHI access events by 5 criteria: user, patient, date range, action type, and resource',
      panDirection: 'left',
      zoomLevel: 1.03,
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 15 },
          props: { width: 500, height: 50, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-07-demo-seeding.png',
      narrativeCaption:
        'Demo seeder generates 500 synthetic patients across 3 tenants without exposing real PHI',
      panDirection: 'right',
      zoomLevel: 1.05,
      overlays: [
        {
          type: 'text',
          startFrame: 30,
          duration: 140,
          position: { x: 55, y: 10 },
          props: { text: 'Synthetic Data Generation', fontSize: '1.4rem' },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-08-compliance-dashboard.png',
      narrativeCaption:
        'Compliance dashboard verifies 8 HIPAA technical safeguards — all 8 controls show PASSING status',
      panDirection: 'left',
      zoomLevel: 1.02,
      overlays: [
        {
          type: 'badge',
          startFrame: 25,
          duration: 145,
          position: { x: 55, y: 12 },
          props: {
            text: 'All Controls: PASSING',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-09-deployment-monitor.png',
      narrativeCaption:
        'Deployment monitor tracks 51 HIPAA-regulated microservices via Prometheus — 99.9% uptime across all nodes',
      panDirection: 'right',
      zoomLevel: 1.04,
      overlays: [
        {
          type: 'metric',
          startFrame: 25,
          duration: 145,
          position: { x: 60, y: 20 },
          props: { from: 0, to: 51, suffix: ' Services Running', fontSize: '2rem', glowOnComplete: true },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-10-live-monitor.png',
      narrativeCaption:
        'Live heartbeat visualizes 51 HIPAA-compliant services at 5-second intervals — SLO breach triggers alert in 30 seconds',
      panDirection: 'none',
      zoomLevel: 1.0,
      overlays: [
        {
          type: 'badge',
          startFrame: 20,
          duration: 150,
          position: { x: 55, y: 10 },
          props: {
            text: '99.9% Uptime SLO',
            backgroundColor: 'rgba(34, 197, 94, 0.95)',
            color: 'white',
            fontSize: '1.3rem',
          },
        },
        {
          type: 'text',
          startFrame: 55,
          duration: 115,
          position: { x: 55, y: 68 },
          props: { text: 'Observable SLOs — verifiable by customers in real-time', fontSize: '1.3rem' },
        },
      ],
      durationFrames: 195,
    },
  ],

  cta: {
    headline: 'HIPAA Compliance Built In',
    highlightText: 'Not Bolted On',
    stats: [
      {
        value: '100% Audit Trail',
        backgroundColor: 'rgba(239, 68, 68, 0.95)',
        borderColor: 'rgba(239, 68, 68, 1)',
        textColor: 'white',
        glowColor: 'rgba(239, 68, 68, 0.6)',
      },
      {
        value: '4-Tenant Isolation',
        backgroundColor: 'rgba(59, 130, 246, 0.95)',
        borderColor: 'rgba(59, 130, 246, 1)',
        textColor: 'white',
        glowColor: 'rgba(59, 130, 246, 0.6)',
      },
      {
        value: '99.9% Uptime SLO',
        backgroundColor: 'rgba(34, 197, 94, 0.95)',
        borderColor: 'rgba(34, 197, 94, 1)',
        textColor: 'white',
        glowColor: 'rgba(34, 197, 94, 0.6)',
      },
    ],
    ctaText: 'See Our Security Posture',
    ctaUrl: 'healthdatainmotion.com',
    durationFrames: 540,
  },
};
