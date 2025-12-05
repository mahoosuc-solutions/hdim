/**
 * BatchSelector Component
 *
 * A dropdown component for selecting and switching between batch evaluations.
 * Displays batch information including measure name, timestamp, and status.
 */

import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Box,
  Typography,
  Stack,
} from '@mui/material';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';
import type { SelectChangeEvent } from '@mui/material/Select';
import { formatDistanceToNow } from 'date-fns';

export interface Batch {
  batchId: string;
  measureName: string;
  timestamp: number;
  status: 'active' | 'completed';
}

export interface BatchSelectorProps {
  batches: Batch[];
  onBatchSelect: (batchId: string) => void;
  selectedBatchId?: string;
}

/**
 * BatchSelector component for selecting active or completed batch evaluations
 */
export function BatchSelector({
  batches,
  onBatchSelect,
  selectedBatchId = '',
}: BatchSelectorProps) {
  // Sort batches: active first (by timestamp desc), then completed (by timestamp desc)
  const sortedBatches = [...batches].sort((a, b) => {
    // First, sort by status (active before completed)
    if (a.status === 'active' && b.status === 'completed') return -1;
    if (a.status === 'completed' && b.status === 'active') return 1;

    // Within the same status, sort by timestamp descending (most recent first)
    return b.timestamp - a.timestamp;
  });

  const handleChange = (event: SelectChangeEvent<string>) => {
    onBatchSelect(event.target.value);
  };

  // Get the measure name for the selected batch to display in the Select
  const selectedBatchMeasureName = batches.find(
    (batch) => batch.batchId === selectedBatchId
  )?.measureName || '';

  return (
    <FormControl fullWidth>
      <InputLabel id="batch-selector-label">Select Batch</InputLabel>
      <Select
        labelId="batch-selector-label"
        id="batch-selector"
        value={selectedBatchId}
        label="Select Batch"
        onChange={handleChange}
        renderValue={(value) => {
          const batch = batches.find((b) => b.batchId === value);
          return batch ? batch.measureName : '';
        }}
      >
        {batches.length === 0 ? (
          <MenuItem disabled>
            <Stack direction="row" spacing={1} alignItems="center">
              <InboxOutlinedIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                No batches available
              </Typography>
            </Stack>
          </MenuItem>
        ) : (
          sortedBatches.map((batch) => (
            <MenuItem key={batch.batchId} value={batch.batchId}>
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  width: '100%',
                  gap: 0.5,
                }}
              >
                {/* Measure name with truncation */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography
                    variant="body1"
                    sx={{
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      flex: 1,
                    }}
                  >
                    {batch.measureName}
                  </Typography>
                  <Chip
                    label={batch.status === 'active' ? 'Active' : 'Completed'}
                    color={batch.status === 'active' ? 'primary' : 'default'}
                    size="small"
                  />
                </Box>

                {/* Timestamp */}
                <Typography variant="caption" color="text.secondary">
                  {formatDistanceToNow(batch.timestamp, { addSuffix: true })}
                </Typography>
              </Box>
            </MenuItem>
          ))
        )}
      </Select>
    </FormControl>
  );
}
