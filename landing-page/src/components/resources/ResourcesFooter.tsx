'use client';

import { useSearchParams } from 'next/navigation';
import styles from '@/styles/agui-portal.module.css';

export default function ResourcesFooter() {
  const searchParams = useSearchParams();
  const shareMode = searchParams.get('share') === '1';

  if (shareMode) {
    return null;
  }

  return (
    <footer className={styles.portalShell}>
      <div className={styles.footer}>
        <p>HDIM Resources | healthdatainmotion.com</p>
      </div>
    </footer>
  );
}
