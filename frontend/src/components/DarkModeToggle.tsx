/**
 * DarkModeToggle Component
 *
 * A toggle switch for switching between light and dark themes
 *
 * Features:
 * - MUI Switch component with sun/moon icons
 * - Tooltip for better UX
 * - Smooth transition animations (0.3s)
 * - Keyboard accessible (Space/Enter)
 * - ARIA labels for screen readers
 * - Designed for AppBar placement (top-right)
 */

import { Switch, Tooltip, Box } from '@mui/material';
import { Brightness7, Brightness4 } from '@mui/icons-material';
import { useDarkMode } from '../hooks/useDarkMode';

export function DarkModeToggle() {
  const { isDarkMode, toggleDarkMode } = useDarkMode();

  const tooltipText = isDarkMode ? 'Switch to light mode' : 'Switch to dark mode';

  return (
    <Tooltip title={tooltipText}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
        }}
      >
        {/* Sun icon for light mode */}
        <Brightness7
          sx={{
            fontSize: 20,
            color: isDarkMode ? 'text.secondary' : 'warning.main',
            transition: 'all 0.3s ease-in-out',
          }}
        />

        {/* Switch component */}
        <Switch
          checked={isDarkMode}
          onChange={toggleDarkMode}
          inputProps={
            {
              'aria-label': 'Toggle dark mode',
            } as React.InputHTMLAttributes<HTMLInputElement>
          }
          sx={{
            '& .MuiSwitch-switchBase': {
              transition: 'all 0.3s ease-in-out',
            },
            '& .MuiSwitch-thumb': {
              transition: 'all 0.3s ease-in-out',
            },
            '& .MuiSwitch-track': {
              transition: 'all 0.3s ease-in-out',
            },
          }}
        />

        {/* Moon icon for dark mode */}
        <Brightness4
          sx={{
            fontSize: 20,
            color: isDarkMode ? 'primary.main' : 'text.secondary',
            transition: 'all 0.3s ease-in-out',
          }}
        />
      </Box>
    </Tooltip>
  );
}
