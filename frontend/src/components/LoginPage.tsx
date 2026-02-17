import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';

const SESSION_EXPIRED_MARKER_KEY = 'sessionExpiredAt';
const DEFAULT_AUTH_TOKEN_KEY = 'authToken';

interface LoginPageProps {
  redirectToAuth?: (url: string) => void;
}

export function LoginPage({ redirectToAuth }: LoginPageProps = {}) {
  const navigate = useNavigate();

  const sessionExpiredAt = useMemo(() => localStorage.getItem(SESSION_EXPIRED_MARKER_KEY), []);
  const authLoginUrl = import.meta.env.VITE_AUTH_LOGIN_URL?.trim() || '';
  const authTokenStorageKey = import.meta.env.VITE_AUTH_TOKEN_STORAGE_KEY?.trim() || DEFAULT_AUTH_TOKEN_KEY;
  const redirect = redirectToAuth ?? ((url: string) => window.location.assign(url));

  const handleSignIn = () => {
    if (authLoginUrl) {
      redirect(authLoginUrl);
      return;
    }

    localStorage.removeItem(SESSION_EXPIRED_MARKER_KEY);
    localStorage.setItem(authTokenStorageKey, `dev-session-${Date.now()}`);
    navigate('/evaluations', { replace: true });
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 520 }}>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h5" component="h1">
              Sign in required
            </Typography>
            {sessionExpiredAt && (
              <Alert severity="warning">
                Session expired. Please sign in again to resume integration health checks.
              </Alert>
            )}
            <Typography variant="body2" color="text.secondary">
              {authLoginUrl
                ? 'Use your organization sign-in provider to re-authenticate.'
                : 'This local environment uses a development sign-in for session recovery.'}
            </Typography>
            <Button variant="contained" onClick={handleSignIn}>
              {authLoginUrl ? 'Sign in' : 'Sign in (dev)'}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
