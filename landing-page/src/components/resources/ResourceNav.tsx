'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useRouter } from 'next/navigation';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import styles from '@/styles/agui-portal.module.css';

const navItems = [
  { href: '/resources', labelKey: 'nav_home' as const },
  { href: '/resources/executive', labelKey: 'nav_exec' as const },
  { href: '/resources/clinical', labelKey: 'nav_clinical' as const },
  { href: '/resources/technical', labelKey: 'nav_technical' as const },
  { href: '/resources/himss-brief', labelKey: 'nav_himss' as const },
  { href: '/resources/trust-center', labelKey: 'nav_trust' as const },
  { href: '/resources/build-evidence', labelKey: 'nav_proof' as const },
  { href: '/resources/evidence-room', labelKey: 'nav_evidence_room' as const },
];

export default function ResourceNav() {
  const pathname = usePathname();
  const router = useRouter();
  const { locale, setLocale, t } = useLanguage();

  return (
    <nav className={styles.portalNav} aria-label="Resources navigation">
      <div className={`${styles.portalShell} ${styles.portalNavInner}`}>
        <Link className={styles.brand} href="/resources">
          <span className={styles.brandBadge}>H</span>
          <span className={styles.brandName}>HDIM Resources</span>
        </Link>
        <div className={styles.navLinks}>
          {navItems.map(({ href, labelKey }) => (
            <Link
              key={href}
              href={href}
              className={`${styles.navLink} ${pathname === href ? styles.navLinkActive : ''}`}
            >
              {t(labelKey)}
            </Link>
          ))}
        </div>
        <label className={styles.mobileNavLabel} htmlFor="resource-nav-select">
          Section
        </label>
        <select
          id="resource-nav-select"
          className={styles.mobileNavSelect}
          value={navItems.some((item) => item.href === pathname) ? pathname : '/resources'}
          onChange={(event) => router.push(event.target.value)}
          aria-label="Resources section selector"
        >
          {navItems.map(({ href, labelKey }) => (
            <option key={href} value={href}>
              {t(labelKey)}
            </option>
          ))}
        </select>
        <div className={styles.langToggle} aria-label="Language toggle">
          <button
            type="button"
            className={`${styles.langBtn} ${locale === 'en' ? styles.langBtnActive : ''}`}
            onClick={() => setLocale('en')}
          >
            EN
          </button>
          <button
            type="button"
            className={`${styles.langBtn} ${locale === 'es' ? styles.langBtnActive : ''}`}
            onClick={() => setLocale('es')}
          >
            ES
          </button>
        </div>
      </div>
    </nav>
  );
}
