import type { Metadata } from 'next';
import styles from '@/styles/agui-portal.module.css';
import EvidenceApprovalConsole from '@/components/resources/EvidenceApprovalConsole';

export const metadata: Metadata = {
  title: 'Evidence Request Review Console | HDIM Resources',
  description: 'Manual approval console for pending evidence-room requests.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function EvidenceReviewPage() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>INTERNAL REVIEW CONSOLE</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Approve pending evidence-room requests and issue secure access links.
          </h1>
          <p className={styles.heroBody}>
            Use the request details from webhook/CRM intake. Approval requires the server-side approver API key.
          </p>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>Manual approval</h2>
        <EvidenceApprovalConsole />
      </section>
    </>
  );
}
