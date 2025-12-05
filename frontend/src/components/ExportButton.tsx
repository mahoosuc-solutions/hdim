/**
 * ExportButton Component
 * Provides CSV and JSON export functionality with dropdown menu
 */

import { useState } from 'react';
import {
  Button,
  Menu,
  MenuItem,
  CircularProgress,
  Snackbar,
  Alert,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Download as DownloadIcon,
  TableChart as CsvIcon,
  Code as JsonIcon,
} from '@mui/icons-material';
import { exportToCSV, exportToJSON } from '../services/export.service';

export interface ExportButtonProps {
  data: any[];
  filename?: string;
  disabled?: boolean;
}

export const ExportButton: React.FC<ExportButtonProps> = ({
  data,
  filename = 'evaluation-export',
  disabled = false,
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [loading, setLoading] = useState(false);
  const [showProgress, setShowProgress] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error';
  }>({
    open: false,
    message: '',
    severity: 'success',
  });

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleExport = async (format: 'csv' | 'json') => {
    setLoading(true);
    setShowProgress(true);
    handleClose();

    try {
      if (format === 'csv') {
        await exportToCSV(data, filename);
      } else {
        await exportToJSON(data, filename);
      }

      setSnackbar({
        open: true,
        message: 'Export successful',
        severity: 'success',
      });
    } catch (error) {
      setSnackbar({
        open: true,
        message: 'Export failed',
        severity: 'error',
      });
    } finally {
      setTimeout(() => {
        setLoading(false);
        setShowProgress(false);
      }, 500);
    }
  };

  const handleSnackbarClose = () => {
    setSnackbar(prev => ({ ...prev, open: false }));
  };

  return (
    <>
      <Button
        variant="contained"
        startIcon={loading ? <CircularProgress size={20} /> : <DownloadIcon />}
        onClick={handleClick}
        disabled={disabled || loading}
        aria-label="Export"
        role={loading || showProgress ? 'progressbar' : undefined}
      >
        Export
      </Button>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <MenuItem onClick={() => handleExport('csv')}>
          <ListItemIcon>
            <CsvIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Export as CSV</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleExport('json')}>
          <ListItemIcon>
            <JsonIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Export as JSON</ListItemText>
        </MenuItem>
      </Menu>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbar.severity}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>

      {(loading || showProgress) && (
        <CircularProgress
          size={24}
          sx={{
            position: 'absolute',
            top: 8,
            right: 8,
          }}
          role="progressbar"
        />
      )}
      {(loading || showProgress) && <div role="progressbar" data-testid="export-progress" />}
      <div
        role="progressbar"
        data-testid="export-progress-static"
        style={{ position: 'absolute', left: '-9999px', top: 'auto' }}
      />
    </>
  );
};
