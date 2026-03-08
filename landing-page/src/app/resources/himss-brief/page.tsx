import type { Metadata } from 'next';
import HimssBriefHub from './HimssBriefHub';

export const metadata: Metadata = {
  title: 'HIMSS Briefing | HDIM Resources',
  description:
    'Unified HIMSS briefing for HDIM accelerator, platform, validation, and performance posture.',
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/himss-brief',
  },
};

export default function HimssBriefPage() {
  return <HimssBriefHub />;
}
