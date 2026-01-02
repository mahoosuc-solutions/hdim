import React, { useState, useEffect } from 'react';
import {
  Drawer,
  Box,
  Typography,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Slider,
  Switch,
  FormControlLabel,
  Button,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  Chip,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { useSettings, UserSettings } from '../hooks/useSettings';

export interface SettingsPanelProps {
  open: boolean;
  onClose: () => void;
}

const debounceMarks = [
  { value: 100, label: '100ms' },
  { value: 300, label: '300ms' },
  { value: 500, label: '500ms' },
  { value: 1000, label: '1000ms' },
];

export const SettingsPanel: React.FC<SettingsPanelProps> = ({ open, onClose }) => {
  const {
    settings,
    updateSettings,
    saveSettings,
    resetSettings,
    hasUnsavedChanges,
  } = useSettings();

  const [localSettings, setLocalSettings] = useState<UserSettings>(settings);
  const [resetDialogOpen, setResetDialogOpen] = useState(false);

  // Sync local settings with hook settings when panel opens or settings change
  useEffect(() => {
    if (open) {
      setLocalSettings(settings);
    }
  }, [open, settings]);

  const handleThemeChange = (event: any) => {
    const newSettings = { ...localSettings, theme: event.target.value as UserSettings['theme'] };
    setLocalSettings(newSettings);
    updateSettings({ theme: event.target.value });
  };

  const handleDebounceChange = (event: Event, value: number | number[]) => {
    const debounceValue = value as number;
    const newSettings = { ...localSettings, searchDebounceMs: debounceValue };
    setLocalSettings(newSettings);
    updateSettings({ searchDebounceMs: debounceValue });
  };

  const handleNotificationsChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newSettings = { ...localSettings, notificationsEnabled: event.target.checked };
    setLocalSettings(newSettings);
    updateSettings({ notificationsEnabled: event.target.checked });
  };

  const handleSave = () => {
    saveSettings();
    onClose();
  };

  const handleCancel = () => {
    setLocalSettings(settings);
    // Reset to saved settings
    const savedSettingsStr = localStorage.getItem('userSettings');
    if (savedSettingsStr) {
      try {
        const savedSettings = JSON.parse(savedSettingsStr);
        updateSettings(savedSettings);
      } catch {
        // If parse fails, reset to defaults
        resetSettings();
      }
    } else {
      resetSettings();
    }
    onClose();
  };

  const handleResetClick = () => {
    setResetDialogOpen(true);
  };

  const handleResetConfirm = () => {
    resetSettings();
    setResetDialogOpen(false);
  };

  const handleResetCancel = () => {
    setResetDialogOpen(false);
  };

  return (
    <>
      <Drawer
        anchor="right"
        open={open}
        onClose={onClose}
        role="dialog"
        PaperProps={{
          sx: { width: 400, p: 3 },
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5" component="h2">
            Settings
          </Typography>
          <IconButton onClick={onClose} aria-label="close" size="small">
            <CloseIcon />
          </IconButton>
        </Box>

        {hasUnsavedChanges && (
          <Chip
            label="Unsaved changes"
            color="warning"
            size="small"
            sx={{ mb: 2, alignSelf: 'flex-start' }}
          />
        )}

        {/* Appearance Section */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Appearance
          </Typography>
          <FormControl fullWidth>
            <InputLabel id="theme-select-label">Theme preference</InputLabel>
            <Select
              labelId="theme-select-label"
              id="theme-select"
              value={localSettings.theme}
              label="Theme preference"
              onChange={handleThemeChange}
            >
              <MenuItem value="auto">Auto</MenuItem>
              <MenuItem value="light">Light</MenuItem>
              <MenuItem value="dark">Dark</MenuItem>
            </Select>
          </FormControl>
        </Box>

        <Divider sx={{ mb: 3 }} />

        {/* Search Section */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Search
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Search debounce delay: {localSettings.searchDebounceMs}ms
          </Typography>
          <Slider
            value={localSettings.searchDebounceMs}
            onChange={handleDebounceChange}
            min={100}
            max={1000}
            step={50}
            marks={debounceMarks}
            valueLabelDisplay="auto"
            aria-label="Search debounce delay"
          />
        </Box>

        <Divider sx={{ mb: 3 }} />

        {/* Notifications Section */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Notifications
          </Typography>
          <FormControlLabel
            control={
              <Switch
                checked={localSettings.notificationsEnabled}
                onChange={handleNotificationsChange}
              />
            }
            label="Enable notifications"
          />
        </Box>

        <Divider sx={{ mb: 3 }} />

        {/* Data Section */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Data
          </Typography>
          <Button
            variant="outlined"
            color="warning"
            fullWidth
            onClick={handleResetClick}
          >
            Reset to defaults
          </Button>
        </Box>

        {/* Action Buttons */}
        <Box sx={{ display: 'flex', gap: 2, mt: 'auto', pt: 2 }}>
          <Button variant="outlined" fullWidth onClick={handleCancel}>
            Cancel
          </Button>
          <Button variant="contained" fullWidth onClick={handleSave}>
            Save
          </Button>
        </Box>
      </Drawer>

      {/* Reset Confirmation Dialog */}
      <Dialog open={resetDialogOpen} onClose={handleResetCancel}>
        <DialogTitle>Reset Settings</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to reset all settings to their default values? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleResetCancel}>Cancel</Button>
          <Button onClick={handleResetConfirm} color="warning" autoFocus>
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
