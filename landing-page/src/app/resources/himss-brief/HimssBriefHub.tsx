import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/agui-portal.module.css';

const pillars = [
  {
    title: '1) The Accelerator',
    summary:
      'AI-assisted delivery system that converts domain expertise into repeatable enterprise implementation. Scope is story-pointed, priced with clear ALF assumptions, and governed by release evidence.',
    bullets: [
      'Story-point implementation model with transparent scope-to-price conversion',
      'AI Leverage Factor (ALF) to explain velocity without masking risk',
      'Reusable integration and governance patterns to compress time-to-value',
    ],
    links: [
      { href: '/resources/trust-center', label: 'Trust Center' },
      { href: '/resources/evidence-room', label: 'Evidence Room' },
    ],
  },
  {
    title: '2) The Platform',
    summary:
      'HDIM is an event-driven healthcare quality platform designed for multi-EHR operations, secure deployment choices, and role-specific buyer diligence.',
    bullets: [
      'FHIR R4-native integration path with major EHR ecosystems',
      'Deployment flexibility: cloud, hybrid, and controlled environments',
      'Operational alignment for CIO/CISO, clinical leadership, and procurement',
    ],
    links: [
      { href: '/resources/technical', label: 'Technical Evaluation' },
      { href: '/resources/cio-ciso', label: 'CIO/CISO Path' },
      { href: '/resources/procurement', label: 'Procurement Path' },
    ],
  },
  {
    title: '3) Validation Implemented',
    summary:
      'Release and compliance assertions are tied to concrete evidence. Claims are mapped to proof artifacts with freshness and observed/modeled labels.',
    bullets: [
      'Fail-closed release governance and evidence-linked go/no-go posture',
      'Control-to-evidence traceability with decision ownership',
      'Gated packet access for deeper security, reliability, and commercial diligence',
    ],
    links: [
      { href: '/resources/build-evidence', label: 'Release Evidence' },
      { href: '/resources/executive', label: 'Executive + Compliance' },
      { href: '/resources/evidence-room/review', label: 'Internal Review Console' },
    ],
  },
  {
    title: '4) Performance Implemented',
    summary:
      'Performance, reliability, and quality outcomes are communicated with explicit assumptions and evidence references for customer and investor diligence.',
    bullets: [
      'Performance and architecture signals documented in resources and evidence views',
      'ROI and outcomes supported through standardized defensibility methods',
      'Buyer conversion telemetry now instrumented across evidence request flows',
    ],
    links: [
      { href: '/resources/performance', label: 'Performance' },
      { href: '/resources/ai-metrics', label: 'AI Metrics' },
      { href: '/resources/evidence-room', label: 'Request Evidence' },
    ],
  },
];

export default function HimssBriefHub() {
  return (
    <>
      <section className={styles.hero}>
        <div
          className={styles.heroPanel}
          style={{
            background:
              'linear-gradient(125deg, rgba(7, 26, 45, 0.92), rgba(17, 40, 68, 0.88)), url("/resources/screenshots/patient-management.jpg") center/cover',
          }}
        >
          <span className={styles.kicker}>HIMSS BRIEFING PAGE</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            How HDIM communicates the accelerator, platform, validation, and performance.
          </h1>
          <p className={styles.heroBody}>
            This page is the single narrative for conference conversations. It aligns executive messaging, technical posture,
            proof artifacts, and delivery economics.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/trust-center">Open Trust Center</Link>
            <Link className={styles.btnGhost} href="/resources/evidence-room">Open Evidence Room</Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          People-first outcomes in real workflows
        </h2>
        <p className={styles.sectionLead}>
          These product views reinforce the story: technology should make care teams more effective and help patients receive timely interventions.
        </p>
        <div className={styles.cardGrid}>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/care-gaps-interventions.jpg"
              alt="Care team intervention workflow with patient gaps and recommended actions"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>Care-gap interventions prioritized for real people, not abstract dashboards.</figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/provider-dashboard.jpg"
              alt="Provider dashboard showing quality and patient action context"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>Provider-facing quality context supports faster and safer decisions.</figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/patient-management.jpg"
              alt="Patient management view with status and outreach coordination"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>Patient management workflows connect analytics to action and follow-through.</figcaption>
          </figure>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Four pillars for HIMSS conversations
        </h2>
        <div className={styles.cardGrid}>
          {pillars.map((pillar) => (
            <article className={styles.card} key={pillar.title}>
              <h3 className={styles.cardTitle}>{pillar.title}</h3>
              <p className={styles.cardBody}>{pillar.summary}</p>
              <ul>
                {pillar.bullets.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
              <div className={styles.heroActions}>
                {pillar.links.map((link) => (
                  <Link key={link.href} className={styles.btnGhostDark} href={link.href}>
                    {link.label}
                  </Link>
                ))}
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Talk track you can use verbatim
        </h2>
        <div className={styles.notice}>
          HDIM pairs domain architecture decisions with AI-assisted execution to deliver enterprise-grade healthcare quality
          capabilities faster. We do not ask buyers to trust claims alone. We map claims to evidence, enforce release
          governance, and show performance and ROI methods with explicit assumptions.
        </div>
      </section>
    </>
  );
}
