import { Manrope, Space_Grotesk } from 'next/font/google';
import { LanguageProvider } from '@/lib/i18n/LanguageContext';
import ResourceNav from '@/components/resources/ResourceNav';
import ResourcesFooter from '@/components/resources/ResourcesFooter';
import styles from '@/styles/agui-portal.module.css';

const manrope = Manrope({
  subsets: ['latin'],
  variable: '--font-manrope',
  display: 'swap',
});

const spaceGrotesk = Space_Grotesk({
  subsets: ['latin'],
  variable: '--font-space-grotesk',
  display: 'swap',
});

export default function ResourcesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <LanguageProvider>
      <div
        className={`${manrope.variable} ${spaceGrotesk.variable}`}
        style={{
          fontFamily: 'var(--font-manrope), "Segoe UI", sans-serif',
          color: '#0d1b2a',
          background:
            'radial-gradient(circle at 10% -30%, rgba(23, 183, 200, 0.2), transparent 45%), radial-gradient(circle at 90% -10%, rgba(45, 212, 168, 0.18), transparent 40%), linear-gradient(180deg, #f0f7ff 0%, #fbfdff 40%, #ffffff 100%)',
          minHeight: '100vh',
        }}
      >
        <ResourceNav />
        <main className={styles.portalShell}>{children}</main>
        <ResourcesFooter />
      </div>
    </LanguageProvider>
  );
}
