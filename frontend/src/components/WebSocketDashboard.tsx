/**
 * WebSocketDashboard Component
 * 
 * Complete WebSocket-driven analytics dashboard integrating:
 * - Real-time event monitoring
 * - Live batch progress
 * - Connection status and health
 * - Statistical analytics
 * - Performance metrics
 */

import { useState, useMemo } from 'react';
import {
  Box,
  Card,
  CardContent,
  GridLegacy as Grid,
  Typography,
  Tabs,
  Tab,
  Stack,
  LinearProgress,
  Chip,
  Paper,
  Divider,
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  Speed as SpeedIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Timeline as TimelineIcon,
} from '@mui/icons-material';
import { RealTimeEventPanel } from './RealTimeEventPanel';
import { AnalyticsPanel } from './AnalyticsPanel';
import { PerformanceMetricsPanel } from './PerformanceMetricsPanel';
import {
  useEvaluationStore,
  selectActiveBatchProgress,
  selectAllBatches,
  selectOverallSuccessRate,
} from '../store/evaluationStore';
import { ConnectionStatus } from '../services/websocket.service';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`dashboard-tabpanel-${index}`}
      aria-labelledby={`dashboard-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ pt: 2 }}>{children}</Box>}
    </div>
  );
}

export function WebSocketDashboard() {
  const [activeTab, setActiveTab] = useState(0);
  const [analyticsMetric] = useState<
    'successRate' | 'complianceRate' | 'avgDuration' | 'throughput'
  >('successRate');

  // Get data from store
  const activeBatch = useEvaluationStore(selectActiveBatchProgress);
  const allBatches = useEvaluationStore(selectAllBatches);
  const connectionStatus = useEvaluationStore((state) => state.connectionStatus);
  const totalCompleted = useEvaluationStore((state) => state.totalEvaluationsCompleted);
  const totalFailed = useEvaluationStore((state) => state.totalEvaluationsFailed);
  const averageCompliance = useEvaluationStore((state) => state.averageComplianceRate);
  const successRate = useEvaluationStore(selectOverallSuccessRate);

  // Calculate aggregate metrics
  const metrics = useMemo(() => {
    const activeBatchCount = allBatches.filter((b) => b.percentComplete < 100).length;
    const completedBatchCount = allBatches.filter((b) => b.percentComplete >= 100).length;
    const totalEvaluations = totalCompleted + totalFailed;

    return {
      activeBatches: activeBatchCount,
      completedBatches: completedBatchCount,
      totalBatches: allBatches.length,
      totalEvaluations,
      successRate,
      averageCompliance,
      connectionStatus,
    };
  }, [allBatches, totalCompleted, totalFailed, successRate, averageCompliance, connectionStatus]);

  // Get connection status display
  const getConnectionDisplay = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return { color: 'success', label: 'Connected', icon: <CheckCircleIcon fontSize="small" /> };
      case ConnectionStatus.CONNECTING:
        return { color: 'info', label: 'Connecting', icon: <TimelineIcon fontSize="small" /> };
      case ConnectionStatus.RECONNECTING:
        return { color: 'warning', label: 'Reconnecting', icon: <TimelineIcon fontSize="small" /> };
      case ConnectionStatus.ERROR:
        return { color: 'error', label: 'Error', icon: <ErrorIcon fontSize="small" /> };
      default:
        return { color: 'default', label: 'Disconnected', icon: <ErrorIcon fontSize="small" /> };
    }
  };

  const connectionDisplay = getConnectionDisplay();

  return (
    <Box>
      {/* Header with Connection Status */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="h5" fontWeight="bold">
            WebSocket Analytics Dashboard
          </Typography>
          <Chip
            icon={connectionDisplay.icon}
            label={connectionDisplay.label}
            color={connectionDisplay.color as 'success' | 'info' | 'warning' | 'error' | 'default'}
            variant="outlined"
          />
        </Stack>
      </Paper>

      {/* Overview Metrics */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <SpeedIcon color="primary" />
                <Typography variant="caption" color="text.secondary">
                  Active Batches
                </Typography>
              </Stack>
              <Typography variant="h4" fontWeight="bold">
                {metrics.activeBatches}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                of {metrics.totalBatches} total
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <CheckCircleIcon color="success" />
                <Typography variant="caption" color="text.secondary">
                  Total Evaluations
                </Typography>
              </Stack>
              <Typography variant="h4" fontWeight="bold">
                {metrics.totalEvaluations}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {totalCompleted} completed, {totalFailed} failed
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <TrendingUpIcon color="success" />
                <Typography variant="caption" color="text.secondary">
                  Success Rate
                </Typography>
              </Stack>
              <Typography variant="h4" fontWeight="bold" color="success.main">
                {metrics.successRate.toFixed(1)}%
              </Typography>
              <LinearProgress
                variant="determinate"
                value={metrics.successRate}
                color="success"
                sx={{ mt: 1 }}
              />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <TrendingUpIcon color="info" />
                <Typography variant="caption" color="text.secondary">
                  Avg Compliance
                </Typography>
              </Stack>
              <Typography variant="h4" fontWeight="bold" color="info.main">
                {metrics.averageCompliance.toFixed(1)}%
              </Typography>
              <LinearProgress
                variant="determinate"
                value={metrics.averageCompliance}
                color="info"
                sx={{ mt: 1 }}
              />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Active Batch Progress */}
      {activeBatch && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Active Batch: {activeBatch.batchId}
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Typography variant="body2" color="text.secondary">
                  Progress: {activeBatch.completedCount} / {activeBatch.totalPatients} patients
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={activeBatch.percentComplete}
                  sx={{ mt: 1, mb: 2 }}
                />
                <Stack direction="row" spacing={2}>
                  <Typography variant="caption">
                    Success: {activeBatch.successCount}
                  </Typography>
                  <Divider orientation="vertical" flexItem />
                  <Typography variant="caption">
                    Failed: {activeBatch.failedCount}
                  </Typography>
                  <Divider orientation="vertical" flexItem />
                  <Typography variant="caption">
                    Throughput: {activeBatch.currentThroughput.toFixed(1)}/s
                  </Typography>
                </Stack>
              </Grid>
              <Grid item xs={12} md={6}>
                <Stack spacing={1}>
                  <Typography variant="caption" color="text.secondary">
                    Measure: {activeBatch.measureName || 'N/A'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Avg Duration: {activeBatch.avgDurationMs.toFixed(0)} ms
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Compliance Rate: {activeBatch.cumulativeComplianceRate.toFixed(1)}%
                  </Typography>
                </Stack>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Tabs for Different Views */}
      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={activeTab}
            onChange={(_, newValue) => setActiveTab(newValue)}
            aria-label="dashboard tabs"
          >
            <Tab label="Real-Time Events" />
            <Tab label="Analytics" />
            <Tab label="Performance Metrics" />
          </Tabs>
        </Box>

        <TabPanel value={activeTab} index={0}>
          <Box sx={{ height: 600 }}>
            <RealTimeEventPanel maxEvents={100} autoScroll={true} />
          </Box>
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          <AnalyticsPanel batches={allBatches} metric={analyticsMetric} />
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          {activeBatch ? (
            <PerformanceMetricsPanel batchProgress={activeBatch} />
          ) : (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography color="text.secondary">
                No active batch. Performance metrics will appear when a batch is running.
              </Typography>
            </Box>
          )}
        </TabPanel>
      </Card>
    </Box>
  );
}
