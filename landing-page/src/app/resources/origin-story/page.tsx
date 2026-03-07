import type { Metadata } from 'next';
import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Origin Story | HDIM Resources',
  description: 'Why HDIM exists: a 20-year healthcare IT veteran watched integration projects fail at scale, then built the platform that should have existed.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/origin-story' },
};

export default function OriginStoryPage() {
  return (
    <div className={styles.containerNarrow}>
      <h1 className={styles.sectionTitle} style={{ borderBottom: 'none', paddingBottom: 0 }}>Origin Story</h1>
      <p className={styles.subText}>Why HDIM exists, told from the architect&apos;s perspective.</p>

      <div className={styles.card}>
        <h2 className={styles.cardTitle}>The Pattern That Kept Repeating</h2>
        <p className={styles.cardBody}>
          Over 20 years of healthcare IT — from payer quality programs to HIE data pipelines — I watched the same project fail in the same way, repeatedly. A health plan or ACO would commit $1.5M-$3M to build a quality measurement platform. Twelve months in, they had 5-6 services, partial FHIR integration, and a growing list of compliance gaps.
        </p>
        <p className={styles.cardBody} style={{ marginTop: '0.75rem' }}>
          The problem was never the engineering talent. It was coordination overhead. Requirements lost in translation between clinical SMEs and developers. Rework consuming 30-40% of sprint capacity. Security and compliance retrofitted instead of built in. By the time the platform reached pilot, the original requirements had shifted.
        </p>
      </div>

      <div className={styles.card}>
        <h2 className={styles.cardTitle}>The Hypothesis</h2>
        <p className={styles.cardBody}>
          In October 2024, I tested a simple idea: what if the bottleneck in healthcare platform delivery isn&apos;t coding — it&apos;s the gap between domain knowledge and implementation? If one architect who understands HEDIS measures, FHIR R4, HIPAA requirements, and CQL evaluation could write precise specifications, could AI assistants handle the implementation volume?
        </p>
        <p className={styles.cardBody} style={{ marginTop: '0.75rem' }}>
          I started with a Node.js prototype. It worked for validation, but healthcare&apos;s core libraries — HAPI FHIR and the CQL Engine — are Java-native. Within a month, I made the harder decision: rebuild in Java using specification-driven AI development.
        </p>
      </div>

      <figure style={{ margin: '1.5rem 0' }}>
        <Image
          src="/resources/screenshots/provider-dashboard.jpg"
          alt="HDIM Clinical Portal provider dashboard showing role-based views, quality score, and quick actions"
          width={1915}
          height={925}
          style={{ width: '100%', height: 'auto', borderRadius: '8px', border: '1px solid #e2e8f0' }}
        />
        <figcaption style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.5rem' }}>
          The platform that came out: role-based dashboards, real-time quality scores, and clinical quick actions — built in 6 weeks.
        </figcaption>
      </figure>

      <div className={styles.card}>
        <h2 className={styles.cardTitle}>What Came Out</h2>
        <p className={styles.cardBody}>
          Six weeks later: 51+ services, 613+ automated tests, 157 documented API endpoints, 100% Liquibase rollback coverage, HIPAA-compliant audit logging, multi-tenant isolation enforced at the database level, and distributed tracing across every service.
        </p>
        <p className={styles.cardBody} style={{ marginTop: '0.75rem' }}>
          Not a prototype. A production-ready platform that does what 12-person teams spend 18 months trying to build — and does it with architectural consistency that large teams struggle to maintain.
        </p>
      </div>

      <div className={styles.card}>
        <h2 className={styles.cardTitle}>What It Proved</h2>
        <p className={styles.cardBody}>
          AI doesn&apos;t replace domain expertise. It removes the translation loss between knowing what to build and building it. When a healthcare architect writes a specification that includes FHIR profiles, CQL logic paths, tenant isolation rules, and test scenarios — and AI implements exactly that — the result is faster, more consistent, and more thoroughly tested than traditional development.
        </p>
        <p className={styles.cardBody} style={{ marginTop: '0.75rem' }}>
          That&apos;s not a claim about AI in general. It&apos;s a measured outcome from one specific project. The evidence is in the commit history, the test results, and the architecture decisions documented across 1,400+ files.
        </p>
      </div>

      <p style={{ marginTop: '1.5rem' }}>
        <Link className={styles.link} href="/resources/executive-summary">Read the full executive summary</Link>
        {' | '}
        <Link className={styles.link} href="/resources">Back to Resources</Link>
      </p>
    </div>
  );
}
