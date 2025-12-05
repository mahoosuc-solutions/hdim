/**
 * AdvancedExportDialog component
 * Provides advanced export functionality with column selection, format options, and preview
 */

import React, { useState, useMemo, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Radio,
  RadioGroup,
  TextField,
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  CircularProgress,
  Divider,
  Stack,
  Chip,
} from '@mui/material';
import { exportToCSV, exportToJSON } from '../services/export.service';
import type { AnyEvaluationEvent } from '../types/events';

interface AdvancedExportDialogProps {
  open: boolean;
  onClose: () => void;
  data: AnyEvaluationEvent[];
  defaultFilename?: string;
}

// All possible columns from event types
const ALL_COLUMNS = [
  'eventId',
  'eventType',
  'tenantId',
  'timestamp',
  'evaluationId',
  'measureId',
  'measureName',
  'patientId',
  'batchId',
  'inDenominator',
  'inNumerator',
  'complianceRate',
  'score',
  'durationMs',
  'careGapCount',
  'errorMessage',
  'errorCategory',
  'stackTrace',
  'totalPatients',
  'completedCount',
  'successCount',
  'failedCount',
  'pendingCount',
  'percentComplete',
  'avgDurationMs',
  'currentThroughput',
  'elapsedTimeMs',
  'estimatedTimeRemainingMs',
  'denominatorCount',
  'numeratorCount',
  'cumulativeComplianceRate',
];

// Preset configurations
const PRESETS = {
  basic: ['eventType', 'timestamp', 'measureId'],
  clinical: ['patientId', 'measureId', 'complianceRate', 'inDenominator', 'inNumerator'],
  detailed: ALL_COLUMNS,
};

type ExportFormat = 'csv' | 'json';

const AdvancedExportDialog: React.FC<AdvancedExportDialogProps> = ({
  open,
  onClose,
  data,
  defaultFilename = 'evaluation-export',
}) => {
  const [selectedColumns, setSelectedColumns] = useState<Set<string>>(
    new Set(['eventType', 'timestamp', 'measureId', 'patientId', 'batchId'])
  );
  const [format, setFormat] = useState<ExportFormat>('csv');
  const [filename, setFilename] = useState<string>(`${defaultFilename}.csv`);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Update filename extension when format changes
  useEffect(() => {
    const baseFilename = filename.replace(/\.(csv|json)$/, '');
    setFilename(`${baseFilename}.${format}`);
  }, [format]);

  // Update filename when defaultFilename prop changes
  useEffect(() => {
    setFilename(`${defaultFilename}.${format}`);
  }, [defaultFilename]);

  // Get available columns from actual data
  const availableColumns = useMemo(() => {
    const columns = new Set<string>();
    data.forEach(item => {
      Object.keys(item).forEach(key => columns.add(key));
    });
    return Array.from(columns).filter(col => ALL_COLUMNS.includes(col));
  }, [data]);

  const handleColumnToggle = (column: string) => {
    const newSelected = new Set(selectedColumns);
    if (newSelected.has(column)) {
      // Don't allow deselecting if it's the last column
      if (newSelected.size > 1) {
        newSelected.delete(column);
      }
    } else {
      newSelected.add(column);
    }
    setSelectedColumns(newSelected);
  };

  const handleSelectAll = () => {
    setSelectedColumns(new Set(availableColumns));
  };

  const handleDeselectAll = () => {
    // Keep at least one column selected
    setSelectedColumns(new Set([availableColumns[0]]));
  };

  const handlePreset = (preset: keyof typeof PRESETS) => {
    const presetColumns = PRESETS[preset].filter(col => availableColumns.includes(col));
    setSelectedColumns(new Set(presetColumns));
  };

  const handleFormatChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormat(event.target.value as ExportFormat);
  };

  const handleFilenameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFilename(event.target.value);
  };

  const getFilteredData = () => {
    return data.map(item => {
      const filtered: any = {};
      selectedColumns.forEach(col => {
        if (col in item) {
          filtered[col] = (item as any)[col];
        }
      });
      return filtered;
    });
  };

  const getPreviewData = () => {
    return getFilteredData().slice(0, 3);
  };

  const handleExport = async () => {
    if (selectedColumns.size === 0) {
      setError('Please select at least one column');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const filteredData = getFilteredData();
      const baseFilename = filename.replace(/\.(csv|json)$/, '');

      if (format === 'csv') {
        exportToCSV(filteredData, baseFilename);
      } else {
        exportToJSON(filteredData, baseFilename);
      }

      // Close dialog after successful export
      setTimeout(() => {
        setLoading(false);
        onClose();
      }, 500);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="advanced-export-dialog-title"
    >
      <DialogTitle id="advanced-export-dialog-title">
        Advanced Export Options
      </DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 2 }}>
          {/* Error Alert */}
          {error && (
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          {/* Presets */}
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Quick Presets
            </Typography>
            <Stack direction="row" spacing={1}>
              <Button
                size="small"
                variant="outlined"
                onClick={() => handlePreset('basic')}
              >
                Basic
              </Button>
              <Button
                size="small"
                variant="outlined"
                onClick={() => handlePreset('detailed')}
              >
                Detailed
              </Button>
              <Button
                size="small"
                variant="outlined"
                onClick={() => handlePreset('clinical')}
              >
                Clinical
              </Button>
            </Stack>
          </Box>

          <Divider />

          {/* Column Selection */}
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle2">
                Select Columns ({selectedColumns.size} selected)
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" onClick={handleSelectAll}>
                  Select All
                </Button>
                <Button size="small" onClick={handleDeselectAll}>
                  Deselect All
                </Button>
              </Stack>
            </Box>
            <Paper variant="outlined" sx={{ p: 2, maxHeight: 300, overflow: 'auto' }}>
              <FormGroup>
                {availableColumns.map(column => (
                  <FormControlLabel
                    key={column}
                    control={
                      <Checkbox
                        checked={selectedColumns.has(column)}
                        onChange={() => handleColumnToggle(column)}
                        size="small"
                      />
                    }
                    label={column}
                  />
                ))}
              </FormGroup>
            </Paper>
          </Box>

          <Divider />

          {/* Format Selection */}
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Export Format
            </Typography>
            <RadioGroup row value={format} onChange={handleFormatChange}>
              <FormControlLabel
                value="csv"
                control={<Radio />}
                label="CSV"
              />
              <FormControlLabel
                value="json"
                control={<Radio />}
                label="JSON"
              />
            </RadioGroup>
          </Box>

          {/* Filename Input */}
          <TextField
            label="Filename"
            value={filename}
            onChange={handleFilenameChange}
            fullWidth
            size="small"
            helperText={`File will be saved as ${filename}`}
          />

          <Divider />

          {/* Preview */}
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Preview (first 3 rows)
            </Typography>
            <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 300 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    {Array.from(selectedColumns).map(col => (
                      <TableCell key={col}>
                        <Typography variant="caption" fontWeight="bold">
                          {col}
                        </Typography>
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {getPreviewData().map((row, idx) => (
                    <TableRow key={idx}>
                      {Array.from(selectedColumns).map(col => (
                        <TableCell key={col}>
                          <Typography variant="caption">
                            {row[col] !== undefined && row[col] !== null
                              ? String(row[col])
                              : '-'}
                          </Typography>
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                  {getPreviewData().length === 0 && (
                    <TableRow>
                      <TableCell colSpan={selectedColumns.size} align="center">
                        <Typography variant="caption" color="text.secondary">
                          No data to preview
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          onClick={handleExport}
          variant="contained"
          disabled={loading || selectedColumns.size === 0}
          startIcon={loading ? <CircularProgress size={20} /> : null}
        >
          {loading ? 'Exporting...' : 'Export'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default AdvancedExportDialog;
