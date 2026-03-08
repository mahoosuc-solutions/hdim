import type { Metadata } from 'next';
import EvidenceRoomHub from './EvidenceRoomHub';

export const metadata: Metadata = {
  title: 'Evidence Room | HDIM Resources',
  description:
    'Gated diligence room for security, release, and procurement evidence packets.',
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/evidence-room',
  },
};

export default function EvidenceRoomPage() {
  return <EvidenceRoomHub />;
}
