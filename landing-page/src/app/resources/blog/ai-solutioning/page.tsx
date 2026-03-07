import type { Metadata } from 'next';
import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'AI Solutioning in Healthcare Delivery | HDIM Blog',
  description: 'How specification-first AI execution changes delivery economics and reliability.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/blog/ai-solutioning' },
  openGraph: {
    title: 'AI Solutioning in Healthcare Delivery',
    description: 'How specification-first AI execution changes delivery economics and reliability.',
    type: 'article',
  },
};

export default function AISolutioningBlogPage() {
  return (
    <article className={styles.containerNarrow}>
      <h1 className={styles.sectionTitle} style={{ borderBottom: 'none' }}>AI Solutioning in Healthcare Delivery</h1>
      <p className={styles.subText}>Published by HDIM &middot; Architecture and Delivery</p>

      <p className={styles.sectionBody}>Healthcare platform delivery often fails at the seam between architecture and implementation. Plans are coherent, but execution drifts across teams, services, and timelines.</p>
      <p className={styles.sectionBody}>AI solutioning changes this when organizations adopt specification-first workflows. Instead of asking models to invent structure, teams supply architecture, contracts, and constraints up front.</p>

      <div className={styles.callout}>
        <strong>Key takeaway:</strong> AI is most effective when it executes decisions, not when it makes them.
      </div>

      <p className={styles.sectionBody}>HDIM applied this approach to a multi-service healthcare platform and demonstrated meaningful improvements in delivery speed, consistency, and validation readiness.</p>
      <p className={styles.sectionBody}>The Race Track FHIR assets and evidence reports show how communication, architecture, and runtime validation can stay aligned from narrative to implementation.</p>

      <figure style={{ margin: '1.5rem 0' }}>
        <Image
          src="/resources/screenshots/patient-management.jpg"
          alt="HDIM patient management dashboard showing 100 patients with demographics, MPI status, and search filters"
          width={1916}
          height={921}
          style={{ width: '100%', height: 'auto', borderRadius: '8px', border: '1px solid #e2e8f0' }}
        />
        <figcaption style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.5rem' }}>
          Patient management with demographics, MPI linkage status, and duplicate detection — built from specification, not iteration.
        </figcaption>
      </figure>

      <h2 className={styles.sectionTitle}>Architectural Leadership Through Delivery</h2>
      <p className={styles.sectionBody}>This build was guided as an architecture program, not a design-only sprint. The sequence below shows how platform foundations, proof systems, and go-to-market surfaces were intentionally connected.</p>

      <div className={styles.panel}>
        <h3 className={styles.sectionSubtitle} style={{ borderBottom: 'none', paddingBottom: 0 }}>Release Storyline (January-February 2026)</h3>
        <ul style={{ paddingLeft: '1.25rem' }}>
          <li><strong>January 13, 2026:</strong> Production GTM landing baseline established with validation cleanup and deployment hardening (<code className={styles.code}>0bb549d7c</code>).</li>
          <li><strong>February 17-19, 2026:</strong> Performance and reliability narrative grounded in load tests, SLO workflows, and evidence-linked content.</li>
          <li><strong>February 22, 2026:</strong> Race Track FHIR A/B proof loop operationalized with validators, timestamped artifacts, and deployment automation.</li>
          <li><strong>February 23, 2026:</strong> Care transitions pilot integrated across landing and research surfaces to close architecture-to-commercial continuity.</li>
          <li><strong>February 23, 2026:</strong> Vercel and lead API runtime hardening completed for cleaner production behavior.</li>
        </ul>
      </div>

      <p className={styles.sectionBody}>This is the core pattern: architecture decisions are published as verifiable product behavior, then reinforced by automated validation before each release point.</p>

      <p style={{ marginTop: '1.5rem' }}>
        <Link className={styles.link} href="/resources/build-evidence">Open Build Story Evidence Index</Link>
        {' | '}
        <Link className={styles.link} href="/resources">Back to Resources</Link>
      </p>
    </article>
  );
}
