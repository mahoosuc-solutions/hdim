/**
 * WebSocket Integration Example
 * 
 * Demonstrates how to use the WebSocket-driven analytics components
 * in your application.
 */

import { Box, Container, Typography } from '@mui/material';
import { WebSocketDashboard } from '../components/WebSocketDashboard';
import { useWebSocket } from '../hooks/useWebSocket';

export function WebSocketExample() {
  // Initialize WebSocket connection with authentication
  useWebSocket({
    tenantId: 'TENANT001',
    autoConnect: true,
    baseUrl: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine',
    authToken: localStorage.getItem('authToken') || undefined,
  });

  return (
    <Container maxWidth="xl" sx={{ py: 3 }}>
      <Box mb={3}>
        <Typography variant="h4" gutterBottom>
          Real-Time CQL Evaluation Monitoring
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Live WebSocket-driven analytics and event monitoring
        </Typography>
      </Box>

      <WebSocketDashboard />
    </Container>
  );
}

/**
 * Alternative: Use individual components
 */
import { RealTimeEventPanel } from '../components/RealTimeEventPanel';
import { AnalyticsPanel } from '../components/AnalyticsPanel';
import { useEvaluationStore, selectAllBatches } from '../store/evaluationStore';

export function CustomWebSocketLayout() {
  const allBatches = useEvaluationStore(selectAllBatches);

  // Initialize WebSocket
  useWebSocket({
    tenantId: 'TENANT001',
    autoConnect: true,
    baseUrl: 'ws://localhost:8081/cql-engine',
  });

  return (
    <Container maxWidth="xl">
      <Box display="grid" gridTemplateColumns="1fr 1fr" gap={2}>
        {/* Real-time events on the left */}
        <Box>
          <RealTimeEventPanel maxEvents={50} autoScroll={true} />
        </Box>

        {/* Analytics on the right */}
        <Box>
          <AnalyticsPanel batches={allBatches} metric="successRate" />
        </Box>
      </Box>
    </Container>
  );
}
