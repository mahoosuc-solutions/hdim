import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/agui-portal.module.css';
import BookMeetingCta from '@/components/resources/BookMeetingCta';

const capabilities = [
  {
    title: 'FHIR R4 Data Ingestion',
    description:
      'Ingest clinical data from any EHR system through standards-based FHIR R4 interfaces. Multi-source, multi-payer, production-ready.',
  },
  {
    title: 'HEDIS Quality Measures',
    description:
      'Evaluate CQL-based HEDIS measures automatically against patient populations. No manual chart review required.',
  },
  {
    title: 'Care Gap Detection',
    description:
      'Identify and prioritize care gaps before they become Stars rating penalties. Automated, real-time, actionable.',
  },
  {
    title: 'Risk Stratification',
    description:
      'Stratify patient populations by clinical and financial risk to focus interventions where they matter most.',
  },
  {
    title: 'Clinical Decision Support',
    description:
      'Surface evidence-based recommendations at the point of care. Integrated with quality measures and care gap workflows.',
  },
  {
    title: 'HIPAA-Compliant Architecture',
    description:
      'Multi-tenant isolation, PHI audit logging, encrypted transport, and session controls built into every layer.',
  },
];

const partnerTracks = [
  {
    audience: 'Health Plans & ACOs',
    message:
      'Evaluate the full platform against your quality programs at no cost. HEDIS measure evaluation, care gap detection, and quality reporting — deployed in minutes.',
    cta: 'Explore Technical Details',
    href: '/resources/technical',
  },
  {
    audience: 'Providers & Health Systems',
    message:
      'Close care gaps faster with automated quality measurement. HDIM connects to your EHR ecosystem through FHIR R4 and delivers actionable insights to your care teams.',
    cta: 'View Clinical Workflows',
    href: '/resources/clinical',
  },
  {
    audience: 'Integration Vendors',
    message:
      'Your engines move the data — HDIM measures the outcomes. Open APIs, IHE gateway support, and 62+ documented endpoints ready for integration.',
    cta: 'View Architecture',
    href: '/resources/architecture',
  },
  {
    audience: 'Investors & Partners',
    message:
      'Execution discipline backed by evidence. Trust posture, release governance, and licensing transparency — all verifiable.',
    cta: 'Open Trust Center',
    href: '/resources/trust-center',
  },
];

export default function HimssLaunchPage() {
  return (
    <>
      <section className={styles.hero}>
        <div
          className={styles.heroPanel}
          style={{
            background:
              'linear-gradient(125deg, rgba(7, 26, 45, 0.94), rgba(17, 40, 68, 0.90)), url("/resources/screenshots/care-gaps-interventions.jpg") center/cover',
          }}
        >
          <span className={styles.kicker}>HIMSS 2026 &mdash; LAS VEGAS</span>
          <h1
            className={styles.heroTitle}
            style={{ fontFamily: 'var(--font-space-grotesk)' }}
          >
            HealthData-in-Motion is now open source.
          </h1>
          <p className={styles.heroBody}>
            Built by an experienced healthcare architect to fast-forward the
            improvement of healthcare outcomes and reduce costs for every
            customer of healthcare. Released under the Business Source License
            1.1 &mdash; free to evaluate, built to partner.
          </p>
          <div className={styles.heroActions}>
            <a
              className={styles.btnPrimary}
              href="https://github.com/mahoosuc-solutions/hdim"
              target="_blank"
              rel="noreferrer"
            >
              View on GitHub
            </a>
            <BookMeetingCta
              page="himss_launch"
              personaTrack="mixed_buyer"
              objectiveTrack="customer_pipeline"
              label="Book a Meeting"
            />
            <Link className={styles.btnGhost} href="/resources/himss-brief">
              Full HIMSS Brief
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2
          className={styles.sectionTitle}
          style={{ fontFamily: 'var(--font-space-grotesk)' }}
        >
          The license is simple
        </h2>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Free to evaluate</h3>
            <p className={styles.cardBody}>
              Download, install, and test the entire platform — 51
              microservices, Angular clinical portal, full FHIR R4 pipeline — at
              no cost for development, testing, evaluation, and research.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Production requires a license</h3>
            <p className={styles.cardBody}>
              Managed service delivery, hosted operation, or external
              customer-facing deployment requires a commercial agreement with
              Grateful House Inc.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Converts to Apache 2.0 in 2030</h3>
            <p className={styles.cardBody}>
              On March 7, 2030, the license automatically converts to Apache
              License 2.0. Full open source, no strings attached.
            </p>
          </article>
        </div>
        <p
          className={styles.sectionLead}
          style={{ marginTop: '1.5rem', textAlign: 'center' }}
        >
          <Link href="/resources/licensing">
            Read the full licensing transparency page &rarr;
          </Link>
        </p>
      </section>

      <section className={styles.section}>
        <h2
          className={styles.sectionTitle}
          style={{ fontFamily: 'var(--font-space-grotesk)' }}
        >
          What the platform does
        </h2>
        <p className={styles.sectionLead}>
          51 Java microservices handling the full healthcare quality lifecycle.
          Dockerized, HIPAA-compliant, deployable in minutes.
        </p>
        <div className={styles.cardGrid}>
          {capabilities.map((cap) => (
            <article className={styles.card} key={cap.title}>
              <h3 className={styles.cardTitle}>{cap.title}</h3>
              <p className={styles.cardBody}>{cap.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2
          className={styles.sectionTitle}
          style={{ fontFamily: 'var(--font-space-grotesk)' }}
        >
          People-first outcomes
        </h2>
        <p className={styles.sectionLead}>
          Technology should make care teams more effective and help patients
          receive timely interventions.
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
            <figcaption className={styles.figureCaption}>
              Care-gap interventions prioritized for real people, not abstract
              dashboards.
            </figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/provider-dashboard.jpg"
              alt="Provider dashboard showing quality and patient action context"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Provider-facing quality context supports faster and safer
              decisions.
            </figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/patient-management.jpg"
              alt="Patient management view with status and outreach coordination"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Patient management workflows connect analytics to action and
              follow-through.
            </figcaption>
          </figure>
        </div>
      </section>

      <section className={styles.section}>
        <h2
          className={styles.sectionTitle}
          style={{ fontFamily: 'var(--font-space-grotesk)' }}
        >
          Built to partner
        </h2>
        <p className={styles.sectionLead}>
          Whether you&apos;re a health plan, provider, integration vendor, or
          investor — there&apos;s a path to work together.
        </p>
        <div className={styles.cardGrid}>
          {partnerTracks.map((track) => (
            <article className={styles.card} key={track.audience}>
              <h3 className={styles.cardTitle}>{track.audience}</h3>
              <p className={styles.cardBody}>{track.message}</p>
              <div className={styles.heroActions}>
                <Link className={styles.btnGhostDark} href={track.href}>
                  {track.cta}
                </Link>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2
          className={styles.sectionTitle}
          style={{ fontFamily: 'var(--font-space-grotesk)' }}
        >
          Let&apos;s talk at HIMSS
        </h2>
        <p className={styles.sectionLead}>
          I&apos;m at HIMSS 2026 in Las Vegas this week to meet customers and
          vendors who are ready to improve healthcare outcomes together.
        </p>
        <div
          style={{
            display: 'flex',
            gap: '1rem',
            justifyContent: 'center',
            flexWrap: 'wrap',
            marginTop: '1.5rem',
          }}
        >
          <BookMeetingCta
            page="himss_launch_footer"
            personaTrack="mixed_buyer"
            objectiveTrack="customer_pipeline"
            label="Book a Meeting"
          />
          <a
            className={styles.btnGhost}
            href="mailto:info@mahoosuc.solutions?subject=HIMSS%202026%20—%20HDIM%20Conversation"
          >
            Email Us
          </a>
          <a
            className={styles.btnGhost}
            href="https://github.com/mahoosuc-solutions/hdim"
            target="_blank"
            rel="noreferrer"
          >
            Star on GitHub
          </a>
        </div>
      </section>
    </>
  );
}
