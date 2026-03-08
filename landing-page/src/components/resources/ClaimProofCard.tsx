import styles from '@/styles/agui-portal.module.css';

type ProofItem = {
  label: string;
  href: string;
  freshness: string;
  dataStatus: 'Observed' | 'Modeled';
};

type ClaimProofCardProps = {
  claim: string;
  outcome: string;
  proofItems: ProofItem[];
};

export default function ClaimProofCard({ claim, outcome, proofItems }: ClaimProofCardProps) {
  return (
    <article className={styles.claimProofCard}>
      <h3 className={styles.cardTitle}>{claim}</h3>
      <p className={styles.cardBody}>{outcome}</p>
      <ul className={styles.proofList}>
        {proofItems.map((item) => (
          <li key={`${item.label}-${item.href}`} className={styles.proofItem}>
            <a href={item.href} className={styles.proofLink}>
              {item.label}
            </a>
            <div className={styles.proofMeta}>
              <span className={styles.badge}>Freshness: {item.freshness}</span>
              <span className={`${styles.badge} ${item.dataStatus === 'Observed' ? styles.badgeObserved : styles.badgeModeled}`}>
                {item.dataStatus}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </article>
  );
}
