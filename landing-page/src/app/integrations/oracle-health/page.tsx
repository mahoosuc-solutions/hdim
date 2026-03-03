import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { ORACLE_HEALTH_INTEGRATION } from '@/lib/data/oracle-health-integration';

export default function OracleHealthPage() {
  return <IntegrationPageLayout data={ORACLE_HEALTH_INTEGRATION} />;
}
