'use client';

import Link from 'next/link';
import { trackEvent } from '@/lib/analytics';
import styles from '@/styles/agui-portal.module.css';

type TrackedResourceLinkProps = {
  href: string;
  label: string;
  page: string;
  personaTrack: string;
  objectiveTrack: 'customer_pipeline' | 'strategic_partnership' | 'investor_credibility' | 'brand_awareness';
};

export default function TrackedResourceLink({
  href,
  label,
  page,
  personaTrack,
  objectiveTrack,
}: TrackedResourceLinkProps) {
  return (
    <Link
      className={styles.btnGhostDark}
      href={href}
      onClick={() => {
        if (objectiveTrack === 'investor_credibility') {
          trackEvent('investor_material_view', {
            page,
            persona_track: personaTrack,
            objective_track: objectiveTrack,
            target: href,
          });
        }
      }}
    >
      {label}
    </Link>
  );
}
