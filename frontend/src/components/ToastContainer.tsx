/**
 * ToastContainer Component
 *
 * Displays toast notifications from the unified UI store
 * Automatically positions and animates toasts
 * Supports multiple types: success, error, warning, info
 */

import { Alert, Snackbar, Stack } from '@mui/material';
import { useUIStore, selectToasts } from '../store/uiStore';

export function ToastContainer() {
  const toasts = useUIStore(selectToasts);
  const removeToast = useUIStore((state) => state.removeToast);

  return (
    <Stack
      spacing={1}
      sx={{
        position: 'fixed',
        bottom: 24,
        right: 24,
        zIndex: 9999,
        maxWidth: '400px',
      }}
    >
      {toasts.map((toast) => (
        <Snackbar
          key={toast.id}
          open={true}
          autoHideDuration={toast.duration}
          onClose={() => removeToast(toast.id)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          sx={{
            position: 'relative',
            bottom: 'auto',
            right: 'auto',
          }}
        >
          <Alert
            onClose={() => removeToast(toast.id)}
            severity={toast.type}
            variant="filled"
            elevation={6}
            sx={{
              width: '100%',
              minWidth: '300px',
            }}
          >
            {toast.message}
          </Alert>
        </Snackbar>
      ))}
    </Stack>
  );
}
