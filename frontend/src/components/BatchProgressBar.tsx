/**
 * BatchProgressBar Component
 * Displays real-time batch evaluation progress with animated linear progress bar
 */

import { Box, LinearProgress, Typography, Stack } from '@mui/material';
import type { BatchProgressEvent } from '../types/events';

interface BatchProgressBarProps {
  progress: BatchProgressEvent;
}

/**
 * Formats milliseconds to human-readable time string
 * @param ms - Time in milliseconds
 * @returns Formatted string like "15s" or "2m 5s"
 */
function formatETA(ms: number): string {
  const seconds = Math.floor(ms / 1000);

  if (seconds < 60) {
    return `${seconds}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}m ${remainingSeconds}s`;
}

/**
 * Determines the color of the progress bar based on progress and error rate
 * @param progress - Batch progress event data
 * @returns MUI color prop value
 */
function getProgressColor(progress: BatchProgressEvent): 'primary' | 'success' | 'warning' {
  // If completed, show success (green)
  if (progress.percentComplete >= 100) {
    return 'success';
  }

  // If high error rate (>20%), show warning
  if (progress.completedCount > 0) {
    const errorRate = progress.failedCount / progress.completedCount;
    if (errorRate > 0.2) {
      return 'warning';
    }
  }

  // Default: in progress (blue)
  return 'primary';
}

export function BatchProgressBar({ progress }: BatchProgressBarProps) {
  const color = getProgressColor(progress);

  return (
    <Box sx={{ width: '100%', p: 2 }}>
      {/* Measure Name */}
      <Typography variant="h6" gutterBottom>
        {progress.measureName}
      </Typography>

      {/* Progress Bar with Percentage */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
        <Box sx={{ flex: 1 }}>
          <LinearProgress
            variant="determinate"
            value={progress.percentComplete}
            color={color}
            sx={{
              height: 10,
              borderRadius: 5,
              transition: 'all 0.3s ease-in-out',
            }}
          />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ minWidth: 40 }}>
          {Math.round(progress.percentComplete)}%
        </Typography>
      </Box>

      {/* Progress Details */}
      <Stack direction="row" spacing={3} sx={{ mt: 2 }}>
        {/* Completed / Total */}
        <Typography variant="body2" color="text.secondary">
          {progress.completedCount} / {progress.totalPatients}
        </Typography>

        {/* Success / Failed Counts */}
        <Typography variant="body2" color="success.main">
          Success: {progress.successCount}
        </Typography>
        <Typography variant="body2" color="error.main">
          Failed: {progress.failedCount}
        </Typography>

        {/* Throughput */}
        <Typography variant="body2" color="text.secondary">
          {progress.currentThroughput.toFixed(1)} eval/s
        </Typography>

        {/* ETA */}
        <Typography variant="body2" color="text.secondary">
          ETA: {formatETA(progress.estimatedTimeRemainingMs)}
        </Typography>
      </Stack>
    </Box>
  );
}
