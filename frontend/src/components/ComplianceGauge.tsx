/**
 * ComplianceGauge Component
 *
 * Circular gauge showing compliance rate with color-coded status
 * Displays percentage in center and optional counts below
 */

import { Box, CircularProgress, Typography } from '@mui/material';

interface ComplianceGaugeProps {
  /** Compliance rate (0-100) */
  complianceRate: number;
  /** Optional label for description */
  label?: string;
  /** Optional numerator count */
  numerator?: number;
  /** Optional denominator count */
  denominator?: number;
}

/**
 * Get color based on compliance rate
 * - 0-50%: Red (error)
 * - 50-75%: Orange (warning)
 * - 75-90%: Blue (info)
 * - 90-100%: Green (success)
 */
function getComplianceColor(rate: number): 'error' | 'warning' | 'info' | 'success' {
  if (rate < 50) return 'error';
  if (rate < 75) return 'warning';
  if (rate < 90) return 'info';
  return 'success';
}

export function ComplianceGauge({
  complianceRate,
  label,
  numerator,
  denominator,
}: ComplianceGaugeProps) {
  const color = getComplianceColor(complianceRate);
  const displayRate = complianceRate.toFixed(1);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      gap={1}
    >
      {label && (
        <Typography variant="subtitle2" color="text.secondary">
          {label}
        </Typography>
      )}

      <Box position="relative" display="inline-flex">
        {/* Background circle (gray) */}
        <CircularProgress
          variant="determinate"
          value={100}
          size={120}
          thickness={4}
          sx={{
            color: 'action.disabled',
            position: 'absolute',
          }}
        />

        {/* Progress circle (colored) */}
        <CircularProgress
          variant="determinate"
          value={complianceRate}
          size={120}
          thickness={4}
          color={color}
          sx={{
            transition: 'transform 0.3s ease-in-out',
          }}
        />

        {/* Center text */}
        <Box
          sx={{
            top: 0,
            left: 0,
            bottom: 0,
            right: 0,
            position: 'absolute',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography variant="h4" component="div" fontWeight="bold">
            {displayRate}%
          </Typography>
        </Box>
      </Box>

      {/* Counts display */}
      {numerator !== undefined && denominator !== undefined && (
        <Typography variant="body2" color="text.secondary">
          {numerator} / {denominator}
        </Typography>
      )}
    </Box>
  );
}
