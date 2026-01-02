import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  TextField,
  List,
  ListItem,
  ListSubheader,
  Chip,
  Box,
  Typography,
  InputAdornment,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ClearIcon from '@mui/icons-material/Clear';

interface Shortcut {
  keys: string[];
  description: string;
  category: 'Search & Navigation' | 'Theme' | 'Help' | 'Modal' | 'Actions' | 'View';
}

export interface KeyboardShortcutsPanelProps {
  open: boolean;
  onClose: () => void;
}

const shortcuts: Shortcut[] = [
  // Search & Navigation
  {
    keys: ['Ctrl', 'K'],
    description: 'Focus search',
    category: 'Search & Navigation',
  },
  {
    keys: ['Ctrl', '↑'],
    description: 'Scroll to top',
    category: 'Search & Navigation',
  },
  {
    keys: ['Ctrl', '↓'],
    description: 'Scroll to bottom',
    category: 'Search & Navigation',
  },

  // Actions
  {
    keys: ['Ctrl', 'E'],
    description: 'Export filtered events',
    category: 'Actions',
  },
  {
    keys: ['Ctrl', 'B'],
    description: 'Compare batches',
    category: 'Actions',
  },
  {
    keys: ['Ctrl', 'A'],
    description: 'Toggle analytics panel',
    category: 'Actions',
  },

  // View
  {
    keys: ['Ctrl', 'D'],
    description: 'Toggle dark mode',
    category: 'Theme',
  },
  {
    keys: ['Ctrl', ','],
    description: 'Open settings',
    category: 'View',
  },

  // Help
  {
    keys: ['Ctrl', '?'],
    description: 'Open keyboard shortcuts',
    category: 'Help',
  },

  // Modal
  {
    keys: ['ESC'],
    description: 'Close any modal',
    category: 'Modal',
  },
];

export const KeyboardShortcutsPanel: React.FC<KeyboardShortcutsPanelProps> = ({
  open,
  onClose,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  // Filter shortcuts based on search query
  const filteredShortcuts = shortcuts.filter((shortcut) => {
    if (!searchQuery) return true;

    const query = searchQuery.toLowerCase();
    const descriptionMatch = shortcut.description.toLowerCase().includes(query);
    const keysMatch = shortcut.keys.some((key) =>
      key.toLowerCase().includes(query)
    );

    return descriptionMatch || keysMatch;
  });

  // Group shortcuts by category
  const groupedShortcuts = filteredShortcuts.reduce(
    (acc, shortcut) => {
      if (!acc[shortcut.category]) {
        acc[shortcut.category] = [];
      }
      acc[shortcut.category].push(shortcut);
      return acc;
    },
    {} as Record<string, Shortcut[]>
  );

  // Determine if running on Mac
  const isMac =
    typeof navigator !== 'undefined' &&
    navigator.platform.toUpperCase().indexOf('MAC') >= 0;

  // Replace Ctrl with Cmd on Mac
  const getDisplayKeys = (keys: string[]): string[] => {
    return keys.map((key) => (key === 'Ctrl' && isMac ? 'Cmd' : key));
  };

  // Handle clear search
  const handleClearSearch = () => {
    setSearchQuery('');
  };

  // Reset search when dialog closes
  useEffect(() => {
    if (!open) {
      setSearchQuery('');
    }
  }, [open]);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="keyboard-shortcuts-title"
    >
      <DialogTitle id="keyboard-shortcuts-title">
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">Keyboard Shortcuts</Typography>
          <IconButton
            aria-label="close"
            onClick={onClose}
            size="small"
            edge="end"
          >
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <TextField
          fullWidth
          placeholder="Search shortcuts..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          variant="outlined"
          size="small"
          sx={{ mb: 2 }}
          InputProps={{
            endAdornment: searchQuery && (
              <InputAdornment position="end">
                <IconButton
                  aria-label="clear search"
                  onClick={handleClearSearch}
                  edge="end"
                  size="small"
                >
                  <ClearIcon />
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        <List sx={{ width: '100%' }}>
          {Object.entries(groupedShortcuts).map(([category, categoryShortcuts]) => (
            <React.Fragment key={category}>
              <ListSubheader
                sx={{
                  backgroundColor: 'transparent',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  lineHeight: '2.5',
                  color: 'text.primary',
                }}
              >
                {category}
              </ListSubheader>
              {categoryShortcuts.map((shortcut, index) => (
                <ListItem
                  key={`${category}-${index}`}
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    py: 1.5,
                  }}
                >
                  <Typography variant="body2" sx={{ flex: 1 }}>
                    {shortcut.description}
                  </Typography>
                  <Box display="flex" gap={0.5}>
                    {getDisplayKeys(shortcut.keys).map((key, keyIndex) => (
                      <Chip
                        key={keyIndex}
                        label={key}
                        size="small"
                        sx={{
                          fontFamily: 'monospace',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          height: 24,
                          backgroundColor: 'action.selected',
                          border: '1px solid',
                          borderColor: 'divider',
                        }}
                      />
                    ))}
                  </Box>
                </ListItem>
              ))}
            </React.Fragment>
          ))}
        </List>

        {filteredShortcuts.length === 0 && (
          <Box textAlign="center" py={4}>
            <Typography variant="body2" color="text.secondary">
              No shortcuts found
            </Typography>
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
};
