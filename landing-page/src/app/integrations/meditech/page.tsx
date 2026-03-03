import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { MEDITECH_INTEGRATION } from '@/lib/data/meditech-integration';

export default function MeditechPage() {
  return <IntegrationPageLayout data={MEDITECH_INTEGRATION} />;
}
