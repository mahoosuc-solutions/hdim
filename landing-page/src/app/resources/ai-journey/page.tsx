import type { Metadata } from 'next';
import { redirect } from 'next/navigation';

export const metadata: Metadata = {
  title: 'AI Solutioning Journey | HDIM Resources',
  description:
    'How one architect built an enterprise healthcare platform using spec-driven development with AI coding assistants.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/executive-summary' },
};

export default function AIJourneyPage() {
  redirect('/resources/executive-summary');
}
