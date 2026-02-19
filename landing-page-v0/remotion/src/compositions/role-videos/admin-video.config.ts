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
        'Admin dashboard — system health, active users, and compliance status at a glance',
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
        'Multi-tenant isolation — each health plan\'s data is cryptographically separated',
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
        'User management — create, deactivate, and audit all user accounts centrally',
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
        'RBAC role assignment — 6 predefined roles with granular permission control',
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
        'Complete PHI access audit trail — every patient record access logged with user, timestamp, and action',
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
          props: { width: 900, height: 250, borderColor: 'green', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-06-audit-search.png',
      narrativeCaption:
        'Filtered audit search — find specific PHI access events by user, patient, date, or action type',
      overlays: [
        {
          type: 'glow-highlight',
          startFrame: 20,
          duration: 150,
          position: { x: 8, y: 15 },
          props: { width: 500, height: 50, borderColor: 'blue', pulseCount: 2 },
        },
      ],
      durationFrames: 195,
    },
    {
      screenshot: 'screenshots/admin/admin-07-demo-seeding.png',
      narrativeCaption:
        'Demo data seeding — generate realistic test data without exposing real PHI',
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
        'Compliance dashboard — real-time status of all HIPAA technical safeguards',
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
        'Deployment monitoring — 51+ microservices health status with Prometheus + Grafana',
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
        'Live service monitor — real-time heartbeat visualization across the entire platform',
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
        value: 'Full Audit Trail',
        backgroundColor: 'rgba(239, 68, 68, 0.95)',
        borderColor: 'rgba(239, 68, 68, 1)',
        textColor: 'white',
        glowColor: 'rgba(239, 68, 68, 0.6)',
      },
      {
        value: 'Multi-Tenant Isolation',
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
