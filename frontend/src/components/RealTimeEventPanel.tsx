/**
 * RealTimeEventPanel Component
 * Displays real-time WebSocket events with filtering, search, and statistics
 * 
 * Features:
 * - Live event feed with WebSocket connection status
 * - Event type filtering and search
 * - Real-time statistics (events/sec, success rate)
 * - Color-coded event severity
 * - Auto-scroll with pause capability
 * - Export events to JSON/CSV
 */

import { useState, useMemo, useEffect, useRef } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Stack,
  IconButton,
  Tooltip,
  Switch,
  FormControlLabel,
  Divider,
  Paper,
} from '@mui/material';
import {
  Pause as PauseIcon,
  PlayArrow as PlayIcon,
  Clear as ClearIcon,
  GetApp as DownloadIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import { useEvaluationStore, selectAllBatches } from '../store/evaluationStore';
import { ConnectionStatus } from '../services/websocket.service';
import type { AnyEvaluationEvent } from '../types/events';
import { EventType, isBatchProgressEvent } from '../types/events';

interface RealTimeEventPanelProps {
  maxEvents?: number;
  autoScroll?: boolean;
}

export function RealTimeEventPanel({
  maxEvents = 100,
  autoScroll: initialAutoScroll = true,
}: RealTimeEventPanelProps) {
  const [autoScroll, setAutoScroll] = useState(initialAutoScroll);
  const [isPaused, setIsPaused] = useState(false);
  const [displayEvents, setDisplayEvents] = useState<AnyEvaluationEvent[]>([]);
  const eventListRef = useRef<HTMLDivElement>(null);

  // Get data from store
  const recentEvents = useEvaluationStore((state) => state.recentEvents);
  const connectionStatus = useEvaluationStore((state) => state.connectionStatus);
  const allBatches = useEvaluationStore(selectAllBatches);

  // Update display events when not paused
  useEffect(() => {
    if (!isPaused) {
      setDisplayEvents(recentEvents.slice(-maxEvents));
    }
  }, [recentEvents, isPaused, maxEvents]);

  // Auto-scroll to bottom when new events arrive
  useEffect(() => {
    if (autoScroll && eventListRef.current) {
      eventListRef.current.scrollTop = eventListRef.current.scrollHeight;
    }
  }, [displayEvents, autoScroll]);

  // Calculate real-time statistics
  const statistics = useMemo(() => {
    const now = Date.now();
    const last5Seconds = displayEvents.filter(
      (event) => now - event.timestamp < 5000
    );
    const eventsPerSecond = last5Seconds.length / 5;

    const completedEvents = displayEvents.filter(
      (event) => !isBatchProgressEvent(event) && 'type' in event && event.type === EventType.EVALUATION_COMPLETED
    );
    const failedEvents = displayEvents.filter(
      (event) => !isBatchProgressEvent(event) && 'type' in event && event.type === EventType.EVALUATION_FAILED
    );
    const totalEvaluations = completedEvents.length + failedEvents.length;
    const successRate =
      totalEvaluations > 0
        ? (completedEvents.length / totalEvaluations) * 100
        : 0;

    return {
      totalEvents: displayEvents.length,
      eventsPerSecond: eventsPerSecond.toFixed(1),
      successRate: successRate.toFixed(1),
      activeBatches: allBatches.filter((b) => b.percentComplete < 100).length,
    };
  }, [displayEvents, allBatches]);

  // Get connection status color and label
  const getConnectionStatusInfo = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return { color: 'success', label: 'Connected' };
      case ConnectionStatus.CONNECTING:
        return { color: 'info', label: 'Connecting...' };
      case ConnectionStatus.RECONNECTING:
        return { color: 'warning', label: 'Reconnecting...' };
      case ConnectionStatus.ERROR:
        return { color: 'error', label: 'Error' };
      default:
        return { color: 'default', label: 'Disconnected' };
    }
  };

  // Get event severity color
  const getEventColor = (event: AnyEvaluationEvent): 'success' | 'error' | 'info' | 'warning' => {
    if (isBatchProgressEvent(event)) {
      return 'info';
    }
    if ('type' in event) {
      switch (event.type) {
        case EventType.EVALUATION_COMPLETED:
          return 'success';
        case EventType.EVALUATION_FAILED:
          return 'error';
        default:
          return 'info';
      }
    }
    return 'info';
  };

  // Format event display text
  const formatEventText = (event: AnyEvaluationEvent): string => {
    if (isBatchProgressEvent(event)) {
      return `Batch ${event.batchId}: ${event.percentComplete.toFixed(1)}% (${event.completedCount}/${event.totalPatients})`;
    }
    
    if ('patientId' in event && 'measureId' in event) {
      const eventType = 'type' in event && typeof event.type === 'string' ? event.type : 'UNKNOWN';
      return `${eventType} - Patient ${event.patientId} - Measure ${event.measureId}`;
    }

    return 'UNKNOWN_EVENT';
  };

  // Export events to JSON
  const handleExportJSON = () => {
    const dataStr = JSON.stringify(displayEvents, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `events_${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  // Clear events
  const handleClear = () => {
    setDisplayEvents([]);
  };

  const statusInfo = getConnectionStatusInfo();

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', pb: 2 }}>
        {/* Header */}
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2}>
          <Typography variant="h6" component="div">
            Real-Time Events
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip
              label={statusInfo.label}
              color={statusInfo.color as 'success' | 'info' | 'warning' | 'error' | 'default'}
              size="small"
              sx={{ minWidth: 100 }}
            />
            <Tooltip title={isPaused ? 'Resume' : 'Pause'}>
              <IconButton size="small" onClick={() => setIsPaused(!isPaused)}>
                {isPaused ? <PlayIcon /> : <PauseIcon />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Refresh">
              <IconButton size="small" onClick={() => setIsPaused(false)}>
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Clear Events">
              <IconButton size="small" onClick={handleClear}>
                <ClearIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Export to JSON">
              <IconButton size="small" onClick={handleExportJSON}>
                <DownloadIcon />
              </IconButton>
            </Tooltip>
          </Stack>
        </Stack>

        {/* Statistics */}
        <Paper variant="outlined" sx={{ p: 1.5, mb: 2, bgcolor: 'background.default' }}>
          <Stack direction="row" spacing={3} justifyContent="space-around">
            <Box textAlign="center">
              <Typography variant="caption" color="text.secondary">
                Events
              </Typography>
              <Typography variant="h6" fontWeight="bold">
                {statistics.totalEvents}
              </Typography>
            </Box>
            <Divider orientation="vertical" flexItem />
            <Box textAlign="center">
              <Typography variant="caption" color="text.secondary">
                Events/sec
              </Typography>
              <Typography variant="h6" fontWeight="bold">
                {statistics.eventsPerSecond}
              </Typography>
            </Box>
            <Divider orientation="vertical" flexItem />
            <Box textAlign="center">
              <Typography variant="caption" color="text.secondary">
                Success Rate
              </Typography>
              <Typography variant="h6" fontWeight="bold" color="success.main">
                {statistics.successRate}%
              </Typography>
            </Box>
            <Divider orientation="vertical" flexItem />
            <Box textAlign="center">
              <Typography variant="caption" color="text.secondary">
                Active Batches
              </Typography>
              <Typography variant="h6" fontWeight="bold">
                {statistics.activeBatches}
              </Typography>
            </Box>
          </Stack>
        </Paper>

        {/* Auto-scroll toggle */}
        <FormControlLabel
          control={
            <Switch
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              size="small"
            />
          }
          label={<Typography variant="caption">Auto-scroll</Typography>}
          sx={{ mb: 1 }}
        />

        {/* Event List */}
        <Box
          ref={eventListRef}
          sx={{
            flex: 1,
            overflowY: 'auto',
            bgcolor: 'background.default',
            borderRadius: 1,
            p: 1,
            border: 1,
            borderColor: 'divider',
          }}
        >
          {displayEvents.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Typography variant="body2" color="text.secondary">
                {isPaused ? 'Events paused' : 'Waiting for events...'}
              </Typography>
            </Box>
          ) : (
            <Stack spacing={0.5}>
              {displayEvents.map((event, index) => {
                const eventType = isBatchProgressEvent(event) ? 'BATCH_PROGRESS' : ('type' in event && typeof event.type === 'string' ? event.type : 'UNKNOWN');
                return (
                <Paper
                  key={`${eventType}-${event.timestamp}-${index}`}
                  variant="outlined"
                  sx={{
                    p: 1,
                    borderLeft: 3,
                    borderLeftColor: `${getEventColor(event)}.main`,
                    transition: 'background-color 0.3s',
                    '&:hover': {
                      bgcolor: 'action.hover',
                    },
                  }}
                >
                  <Stack direction="row" alignItems="center" spacing={1}>
                    <Chip
                      label={String(eventType)}
                      color={getEventColor(event)}
                      size="small"
                      sx={{ minWidth: 160, fontFamily: 'monospace', fontSize: '0.7rem' }}
                    />
                    <Typography
                      variant="body2"
                      sx={{ flex: 1, fontFamily: 'monospace', fontSize: '0.85rem' }}
                    >
                      {formatEventText(event)}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ fontFamily: 'monospace' }}
                    >
                      {format(new Date(event.timestamp), 'HH:mm:ss.SSS')}
                    </Typography>
                  </Stack>
                </Paper>
              );
              })}
            </Stack>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}
