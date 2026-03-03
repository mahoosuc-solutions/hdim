import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { ECLINICALWORKS_INTEGRATION } from '@/lib/data/eclinicalworks-integration';

export default function EClinicalWorksPage() {
  return <IntegrationPageLayout data={ECLINICALWORKS_INTEGRATION} />;
}
