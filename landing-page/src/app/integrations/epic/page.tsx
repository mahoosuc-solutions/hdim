import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { EPIC_INTEGRATION } from '@/lib/data/epic-integration';

export default function EpicPage() {
  return <IntegrationPageLayout data={EPIC_INTEGRATION} />;
}
