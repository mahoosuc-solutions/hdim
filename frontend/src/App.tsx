/**
 * Main App component with dashboard layout
 *
 * Features:
 * - Material-UI theme
 * - AppBar with connection status
 * - Basic layout structure
 * - WebSocket integration
 */

import { useState, useMemo, useEffect } from 'react';
import {
  AppBar,
  Box,
  CssBaseline,
  Toolbar,
  Typography,
  Container,
  Card,
  CardContent,
  GridLegacy as Grid,
  ThemeProvider,
  createTheme,
  IconButton,
  Tooltip,
  Dialog,
  DialogContent,
  DialogTitle,
  Stack
} from '@mui/material';
import {
  AssessmentOutlined,
  Settings as SettingsIcon,
  HelpOutline as HelpIcon,
  CompareArrows as CompareIcon,
  TrendingUp as TrendingUpIcon
} from '@mui/icons-material';
import { ConnectionStatus } from './components/ConnectionStatus';
import { DarkModeToggle } from './components/DarkModeToggle';
import { PerformanceMetricsPanel } from './components/PerformanceMetricsPanel';
import { BatchSelector } from './components/BatchSelector';
import type { Batch } from './components/BatchSelector';
import { SimpleEventFilter } from './components/SimpleEventFilter';
import { SearchBar } from './components/SearchBar';
import { ExportButton } from './components/ExportButton';
import EventDetailsModal from './components/EventDetailsModal';
import { KeyboardShortcutsPanel } from './components/KeyboardShortcutsPanel';
import { SettingsPanel } from './components/SettingsPanel';
import { VirtualizedEventList } from './components/VirtualizedEventList';
import BatchComparisonView from './components/BatchComparisonView';
import TrendsChart from './components/TrendsChart';
import AdvancedExportDialog from './components/AdvancedExportDialog';
import { useWebSocket } from './hooks/useWebSocket';
import { useDarkMode } from './hooks/useDarkMode';
import { useNotifications } from './hooks/useNotifications';
import { useSettings } from './hooks/useSettings';
import {
  useEvaluationStore,
  selectActiveBatchProgress,
  selectOverallSuccessRate,
  selectAllBatches
} from './store/evaluationStore';
import { useUIStore, useToast } from './store/uiStore';
import { ToastContainer } from './components/ToastContainer';
import { AnalyticsPanel } from './components/AnalyticsPanel';
import MultiBatchComparison from './components/MultiBatchComparison';
import { ComplianceGauge } from './components/ComplianceGauge';
import type { AnyEvaluationEvent } from './types/events';
import { EventType } from './types/events';

function App() {
  const [tenantId] = useState('TENANT001'); // Default tenant for testing

  // Dark mode integration
  const { isDarkMode } = useDarkMode();

  // UI Store integration
  const toast = useToast();
  const analyticsVisible = useUIStore((state) => state.panelVisibility.analyticsPanel);
  const toggleAnalyticsPanel = useUIStore((state) => state.togglePanel);

  // Create theme dynamically based on dark mode
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: isDarkMode ? 'dark' : 'light',
          primary: {
            main: '#1976d2',
          },
          secondary: {
            main: '#dc004e',
          },
        },
      }),
    [isDarkMode]
  );

  // Initialize WebSocket connection
  useWebSocket({
    tenantId,
    autoConnect: true,
    baseUrl: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine'
  });

  // Get state from store
  const activeBatch = useEvaluationStore(selectActiveBatchProgress);
  const allBatches = useEvaluationStore(selectAllBatches);
  const activeBatchId = useEvaluationStore(state => state.activeBatchId);
  const setActiveBatch = useEvaluationStore(state => state.setActiveBatch);
  const totalCompleted = useEvaluationStore(state => state.totalEvaluationsCompleted);
  const totalFailed = useEvaluationStore(state => state.totalEvaluationsFailed);
  const averageCompliance = useEvaluationStore(state => state.averageComplianceRate);
  const successRate = useEvaluationStore(selectOverallSuccessRate);
  const recentEvents = useEvaluationStore(state => state.recentEvents);

  // Get filters from store (single source of truth)
  const eventFilters = useEvaluationStore(state => state.eventFilters);

  // Local state for search only
  const [searchQuery, setSearchQuery] = useState('');

  // Modal state
  const [selectedEvent, setSelectedEvent] = useState<AnyEvaluationEvent | null>(null);
  const [shortcutsPanelOpen, setShortcutsPanelOpen] = useState(false);
  const [settingsPanelOpen, setSettingsPanelOpen] = useState(false);
  const [batchComparisonOpen, setBatchComparisonOpen] = useState(false);
  const [advancedExportOpen, setAdvancedExportOpen] = useState(false);

  // Advanced features hooks
  const { showNotification, notificationsEnabled, requestPermission } = useNotifications();
  // Settings hook called for side effects (loads settings into context)
  useSettings();

  // Convert BatchProgressEvents to Batch type for BatchSelector
  const batches: Batch[] = useMemo(() => {
    return allBatches.map(batch => ({
      batchId: batch.batchId,
      measureName: batch.measureName,
      timestamp: batch.timestamp,
      status: batch.percentComplete >= 100 ? 'completed' as const : 'active' as const
    }));
  }, [allBatches]);

  // Get unique event types and measures from recent events
  const availableEventTypes = useMemo(() => {
    return Object.values(EventType);
  }, []);

  // Get available measures from store
  // IMPORTANT: Use useMemo with recentEvents dependency to ensure stable array reference
  // This prevents MUI Select from re-rendering infinitely due to prop changes
  const availableMeasures = useMemo(() => {
    const measures = new Set<string>();
    recentEvents.forEach(event => {
      if ('measureId' in event && event.measureId) {
        measures.add(event.measureId);
      }
    });
    return Array.from(measures);
  }, [recentEvents]);

  // Filter events based on filters and search
  const filteredEvents = useMemo(() => {
    let filtered = recentEvents;

    // Apply event type filter
    if (eventFilters.eventTypes.length > 0) {
      filtered = filtered.filter(event =>
        eventFilters.eventTypes.includes(event.eventType)
      );
    }

    // Apply measure filter
    if (eventFilters.measureId) {
      filtered = filtered.filter(event =>
        'measureId' in event && event.measureId === eventFilters.measureId
      );
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(event => {
        const eventType = event.eventType.toLowerCase();
        const measureId = 'measureId' in event ? event.measureId?.toLowerCase() || '' : '';
        const patientId = 'patientId' in event ? event.patientId?.toLowerCase() || '' : '';
        return eventType.includes(query) || measureId.includes(query) || patientId.includes(query);
      });
    }

    return filtered;
  }, [recentEvents, eventFilters, searchQuery]);

  // Batch completion notifications
  useEffect(() => {
    if (!activeBatch) return;

    // Check if batch just completed
    if (activeBatch.percentComplete === 100) {
      const successRate = ((activeBatch.successCount / activeBatch.completedCount) * 100).toFixed(1);

      // Toast notification
      toast.success(
        `Batch ${activeBatch.measureName} completed: ${activeBatch.completedCount} evaluations (${successRate}% success)`
      );

      // Browser notification if enabled
      if (notificationsEnabled) {
        showNotification(
          'Batch Evaluation Complete',
          `${activeBatch.measureName}: ${activeBatch.completedCount} evaluations (${successRate}% success)`,
          {
            icon: '/favicon.ico',
            badge: '/favicon.ico'
          }
        );
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeBatch?.percentComplete, activeBatch?.batchId, notificationsEnabled]); // Removed showNotification to prevent re-trigger

  // Global keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Don't trigger shortcuts when typing in inputs
      const target = event.target as HTMLElement;
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
        return;
      }

      // Ctrl+? or Cmd+? - Open keyboard shortcuts panel
      if ((event.ctrlKey || event.metaKey) && event.key === '?') {
        event.preventDefault();
        setShortcutsPanelOpen(true);
      }

      // Ctrl+, - Open settings
      if ((event.ctrlKey || event.metaKey) && event.key === ',') {
        event.preventDefault();
        setSettingsPanelOpen(true);
      }

      // Ctrl+B - Compare batches
      if ((event.ctrlKey || event.metaKey) && event.key === 'b' && allBatches.length >= 2) {
        event.preventDefault();
        setBatchComparisonOpen(true);
      }

      // Ctrl+A - Toggle analytics
      if ((event.ctrlKey || event.metaKey) && event.key === 'a' && allBatches.length > 0) {
        event.preventDefault();
        toggleAnalyticsPanel('analyticsPanel');
      }

      // Ctrl+E - Export
      if ((event.ctrlKey || event.metaKey) && event.key === 'e' && filteredEvents.length > 0) {
        event.preventDefault();
        setAdvancedExportOpen(true);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [allBatches.length, filteredEvents.length, toggleAnalyticsPanel]);

  // Request notification permission on first render if enabled in settings
  useEffect(() => {
    if (notificationsEnabled && Notification.permission === 'default') {
      requestPermission();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [notificationsEnabled]); // Removed requestPermission - it's stable from useCallback

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        {/* AppBar */}
        <AppBar position="static" elevation={1}>
          <Toolbar>
            <AssessmentOutlined sx={{ mr: 2 }} />
            <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
              CQL Engine Evaluation Dashboard
            </Typography>
            <Typography variant="body2" sx={{ mr: 2 }}>
              Tenant: {tenantId}
            </Typography>
            <ConnectionStatus />
            <Box sx={{ ml: 2, display: 'flex', gap: 1 }}>
              <Tooltip title="Compare Batches">
                <IconButton
                  color="inherit"
                  onClick={() => setBatchComparisonOpen(true)}
                  disabled={allBatches.length < 2}
                  aria-label="compare batches"
                >
                  <CompareIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="View Trends">
                <IconButton
                  color="inherit"
                  onClick={() => {
                    const trendsSection = document.getElementById('trends-section');
                    trendsSection?.scrollIntoView({ behavior: 'smooth' });
                  }}
                  disabled={allBatches.length < 2}
                  aria-label="view trends"
                >
                  <TrendingUpIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Keyboard Shortcuts (Ctrl+?)">
                <IconButton
                  color="inherit"
                  onClick={() => setShortcutsPanelOpen(true)}
                  aria-label="keyboard shortcuts"
                >
                  <HelpIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Settings">
                <IconButton
                  color="inherit"
                  onClick={() => setSettingsPanelOpen(true)}
                  aria-label="settings"
                >
                  <SettingsIcon />
                </IconButton>
              </Tooltip>
              <DarkModeToggle />
            </Box>
          </Toolbar>
        </AppBar>

        {/* Main Content */}
        <Container maxWidth="xl" sx={{ mt: 4, mb: 4, flex: 1 }}>
          <Grid container spacing={3}>
            {/* Quick Actions Panel */}
            {allBatches.length > 0 && (
              <Grid item xs={12}>
                <Card elevation={0} sx={{ bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
                      <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center' }}>
                        <AssessmentOutlined sx={{ mr: 1 }} />
                        Quick Actions
                      </Typography>
                      <Stack direction="row" spacing={2} flexWrap="wrap">
                        <Tooltip title="View statistical analytics">
                          <IconButton
                            color="inherit"
                            onClick={() => toggleAnalyticsPanel('analyticsPanel')}
                            disabled={allBatches.length === 0}
                            sx={{
                              bgcolor: analyticsVisible ? 'rgba(255,255,255,0.3)' : 'rgba(255,255,255,0.1)',
                              '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' }
                            }}
                          >
                            📊
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Compare multiple batches side-by-side">
                          <IconButton
                            color="inherit"
                            onClick={() => setBatchComparisonOpen(true)}
                            disabled={allBatches.length < 2}
                            sx={{ bgcolor: 'rgba(255,255,255,0.1)', '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } }}
                          >
                            <CompareIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Export filtered events">
                          <IconButton
                            color="inherit"
                            onClick={() => setAdvancedExportOpen(true)}
                            disabled={filteredEvents.length === 0}
                            sx={{ bgcolor: 'rgba(255,255,255,0.1)', '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } }}
                          >
                            <AssessmentOutlined />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title={notificationsEnabled ? "Notifications enabled" : "Enable notifications"}>
                          <IconButton
                            color="inherit"
                            onClick={requestPermission}
                            sx={{ bgcolor: 'rgba(255,255,255,0.1)', '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } }}
                          >
                            {notificationsEnabled ? '🔔' : '🔕'}
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Summary Statistics - Enhanced with Gauges */}
            <Grid item xs={12} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom variant="body2">
                    Total Completed
                  </Typography>
                  <Typography variant="h4">{totalCompleted.toLocaleString()}</Typography>
                  <Typography variant="caption" color="textSecondary">
                    {totalFailed > 0 ? `${totalFailed} failed` : 'All successful'}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={3}>
              <Card>
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography color="textSecondary" gutterBottom variant="body2">
                    Success Rate
                  </Typography>
                  <Box sx={{ display: 'flex', justifyContent: 'center', my: 1 }}>
                    <ComplianceGauge
                      complianceRate={successRate}
                      numerator={totalCompleted - totalFailed}
                      denominator={totalCompleted}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={3}>
              <Card>
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography color="textSecondary" gutterBottom variant="body2">
                    Avg Compliance
                  </Typography>
                  <Box sx={{ display: 'flex', justifyContent: 'center', my: 1 }}>
                    <ComplianceGauge
                      complianceRate={averageCompliance}
                      label="Overall"
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={3}>
              <Card>
                <CardContent>
                  <Typography color="textSecondary" gutterBottom variant="body2">
                    Active Batches
                  </Typography>
                  <Typography variant="h4">{allBatches.length}</Typography>
                  <Typography variant="caption" color="textSecondary">
                    {allBatches.filter(b => b.percentComplete < 100).length} in progress
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            {/* Batch Selector */}
            {batches.length > 0 && (
              <Grid item xs={12}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Select Batch Evaluation
                    </Typography>
                    <BatchSelector
                      batches={batches}
                      selectedBatchId={activeBatchId || ''}
                      onBatchSelect={setActiveBatch}
                    />
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Performance Metrics Panel - Enhanced Visualizations */}
            <Grid item xs={12}>
              <PerformanceMetricsPanel batchProgress={activeBatch} />
            </Grid>

            {/* Analytics Panel - Statistical Insights */}
            {analyticsVisible && allBatches.length > 0 && (
              <Grid item xs={12}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                      <Typography variant="h6">
                        📊 Statistical Analytics
                      </Typography>
                      <IconButton
                        size="small"
                        onClick={() => toggleAnalyticsPanel('analyticsPanel')}
                        aria-label="close analytics"
                      >
                        ✕
                      </IconButton>
                    </Box>
                    <AnalyticsPanel batches={allBatches} metric="successRate" />
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Historical Trends Chart */}
            {allBatches.length > 1 && (
              <Grid item xs={12} id="trends-section">
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                      <Typography variant="h6">
                        <TrendingUpIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                        Historical Trends
                      </Typography>
                      <Stack direction="row" spacing={1}>
                        <Tooltip title="Compare Selected Batches">
                          <IconButton
                            onClick={() => setBatchComparisonOpen(true)}
                            size="small"
                            color="primary"
                          >
                            <CompareIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Export Chart Data">
                          <IconButton
                            onClick={() => setAdvancedExportOpen(true)}
                            size="small"
                            color="primary"
                          >
                            <AssessmentOutlined />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </Box>
                    <TrendsChart
                      batches={allBatches}
                      metric="successRate"
                      timeRange="all"
                      chartType="line"
                    />
                  </CardContent>
                </Card>
              </Grid>
            )}

            {/* Event Filter - Simplified version without MUI Select */}
            <Grid item xs={12}>
              <SimpleEventFilter
                availableEventTypes={availableEventTypes}
                availableMeasures={availableMeasures}
              />
            </Grid>

            {/* Recent Events */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2, flexWrap: 'wrap', gap: 2 }}>
                    <Box>
                      <Typography variant="h6">
                        Recent Events
                      </Typography>
                      <Typography variant="body2" color="textSecondary">
                        {filteredEvents.length} of {recentEvents.length} events
                        {eventFilters.eventTypes.length > 0 || eventFilters.measureId || searchQuery ? ' (filtered)' : ''}
                      </Typography>
                    </Box>
                    <Stack direction="row" spacing={1}>
                      <Tooltip title="Advanced Export Options">
                        <IconButton
                          onClick={() => setAdvancedExportOpen(true)}
                          disabled={filteredEvents.length === 0}
                          size="small"
                          color="primary"
                        >
                          <AssessmentOutlined />
                        </IconButton>
                      </Tooltip>
                      <ExportButton
                        data={filteredEvents}
                        filename="evaluation-events"
                        disabled={filteredEvents.length === 0}
                      />
                    </Stack>
                  </Box>

                  {/* Search Bar */}
                  <Box sx={{ mb: 2 }}>
                    <SearchBar
                      onSearch={setSearchQuery}
                      placeholder="Search events (Ctrl+K)..."
                      debounceMs={300}
                    />
                  </Box>

                  {filteredEvents.length === 0 ? (
                    <Typography variant="body2" color="textSecondary">
                      {recentEvents.length === 0
                        ? 'No events received yet. Waiting for evaluations...'
                        : 'No events match the current filters.'}
                    </Typography>
                  ) : (
                    <VirtualizedEventList
                      events={filteredEvents}
                      onEventClick={setSelectedEvent}
                      height={400}
                      itemHeight={60}
                    />
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Modals */}
      <EventDetailsModal
        event={selectedEvent}
        open={!!selectedEvent}
        onClose={() => setSelectedEvent(null)}
      />

      <KeyboardShortcutsPanel
        open={shortcutsPanelOpen}
        onClose={() => setShortcutsPanelOpen(false)}
      />

      <SettingsPanel
        open={settingsPanelOpen}
        onClose={() => setSettingsPanelOpen(false)}
      />

      {/* Phase 3 Modals - Enhanced Batch Comparison */}
      <Dialog
        open={batchComparisonOpen}
        onClose={() => setBatchComparisonOpen(false)}
        maxWidth="xl"
        fullWidth
      >
        <DialogTitle>
          {allBatches.length >= 3 ? 'Multi-Batch Statistical Comparison' : 'Batch Comparison'}
        </DialogTitle>
        <DialogContent>
          {allBatches.length >= 3 ? (
            <MultiBatchComparison
              batches={allBatches}
              maxBatches={10}
              onClose={() => setBatchComparisonOpen(false)}
            />
          ) : (
            <BatchComparisonView
              batches={allBatches}
              onClose={() => setBatchComparisonOpen(false)}
            />
          )}
        </DialogContent>
      </Dialog>

      <AdvancedExportDialog
        open={advancedExportOpen}
        onClose={() => setAdvancedExportOpen(false)}
        data={filteredEvents}
        defaultFilename="evaluation-events"
      />

      {/* Toast Notifications */}
      <ToastContainer />
    </ThemeProvider>
  );
}

export default App
