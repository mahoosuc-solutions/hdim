'use client';

import { trackEvent } from '@/lib/analytics';
import { CALENDAR_URL } from '@/lib/constants';
import styles from '@/styles/agui-portal.module.css';

type BookMeetingCtaProps = {
  page: string;
  personaTrack: string;
  objectiveTrack: 'customer_pipeline' | 'strategic_partnership' | 'investor_credibility' | 'brand_awareness';
  source?: string;
  medium?: string;
  variant?: 'primary' | 'ghostDark';
  label?: string;
};

function buildHimssBookingUrl(
  page: string,
  objectiveTrack: BookMeetingCtaProps['objectiveTrack'],
  source: string,
  medium: string,
) {
  const url = new URL(CALENDAR_URL);
  url.searchParams.set('utm_source', source);
  url.searchParams.set('utm_medium', medium);
  url.searchParams.set('utm_campaign', 'himss_2026');
  url.searchParams.set('utm_content', page);
  url.searchParams.set('utm_term', objectiveTrack);
  return url.toString();
}

export default function BookMeetingCta({
  page,
  personaTrack,
  objectiveTrack,
  source = 'himss_portal',
  medium = 'web',
  variant = 'primary',
  label = 'Book Meeting',
}: BookMeetingCtaProps) {
  const className = variant === 'ghostDark' ? styles.btnGhostDark : styles.btnPrimary;
  const href = buildHimssBookingUrl(page, objectiveTrack, source, medium);

  return (
    <a
      className={className}
      href={href}
      target="_blank"
      rel="noreferrer"
      onClick={() => {
        trackEvent('cta_book_meeting_click', {
          page,
          persona_track: personaTrack,
          objective_track: objectiveTrack,
          utm_source: source,
          utm_medium: medium,
          utm_campaign: 'himss_2026',
          utm_content: page,
        });

        if (objectiveTrack === 'strategic_partnership') {
          trackEvent('partner_interest_click', {
            page,
            persona_track: personaTrack,
          });
        }
      }}
    >
      {label}
    </a>
  );
}
