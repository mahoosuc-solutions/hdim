import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Build Story Evidence Index | HDIM Resources',
  description: 'Commit-backed evidence index for HDIM architecture, validation, and release readiness.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/build-evidence' },
};

export default function BuildEvidencePage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.sectionTitle} style={{ borderBottom: 'none' }}>Build Story Evidence Index</h1>
      <p className={styles.subText}>Prepared for RC2 planning &middot; Updated February 24, 2026</p>

      <div className={styles.panel}>
        <p>This index ties architecture claims to commits, validation outputs, and visual artifacts so release reviews can verify narrative and delivery alignment quickly.</p>
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Architecture Timeline</h2>
        <div className={styles.panel}>
          <ul style={{ paddingLeft: '1.25rem' }}>
            <li><code className={styles.code}>0bb549d7c</code> (Jan 13, 2026): GTM landing production baseline and validation closure.</li>
            <li><code className={styles.code}>5d4ffea7c</code> (Feb 19, 2026): Performance page + traditional-vs-HDIM proof narrative.</li>
            <li><code className={styles.code}>3e24bec9f</code> (Feb 22, 2026): Race Track FHIR A/B validation and evidence publication.</li>
            <li><code className={styles.code}>4cb1a4bd9</code> (Feb 22, 2026): Automated portal deployment validation workflow.</li>
            <li><code className={styles.code}>bb422d3c7</code> (Feb 23, 2026): Public care transitions pilot and portal integration.</li>
            <li><code className={styles.code}>f2b52c9de</code> (Feb 23, 2026): Vercel cleanup and resilient lead API runtime behavior.</li>
          </ul>
        </div>
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Validation Artifacts</h2>
        <div className={styles.panel}>
          <ul style={{ paddingLeft: '1.25rem' }}>
            <li><Link className={styles.link} href="/resources/fhir-evidence">Clinical Validation Approach</Link></li>
            <li><Link className={styles.link} href="/resources/performance">Performance Benchmarking</Link></li>
          </ul>
        </div>
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Release Point RC2 Checklist</h2>
        <div className={styles.panel}>
          <ul style={{ paddingLeft: '1.25rem' }}>
            <li>Landing content + screenshot set frozen and reviewed.</li>
            <li><code className={styles.code}>validate:ci</code> green in <code className={styles.code}>landing-page-v0</code>.</li>
            <li>Portal link audit regenerated with fresh timestamp.</li>
            <li>Release note references this evidence index directly.</li>
          </ul>
          <p style={{ marginTop: '1rem' }}>
            <Link className={styles.link} href="/resources/executive-summary">Read executive summary</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
