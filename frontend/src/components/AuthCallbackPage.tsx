import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Card, CardContent, Stack, Typography } from '@mui/material';

const DEFAULT_AUTH_TOKEN_KEY = 'authToken';
const DEFAULT_AUTH_QUERY_PARAM = 'access_token';
const SESSION_EXPIRED_MARKER_KEY = 'sessionExpiredAt';

function readTokenFromCallback(url: URL, tokenQueryParam: string): string | null {
  const fromQuery = url.searchParams.get(tokenQueryParam);
  if (fromQuery) {
    return fromQuery;
  }

  const hashParams = new URLSearchParams(url.hash.startsWith('#') ? url.hash.slice(1) : url.hash);
  return hashParams.get(tokenQueryParam);
}

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const tokenStorageKey = import.meta.env.VITE_AUTH_TOKEN_STORAGE_KEY?.trim() || DEFAULT_AUTH_TOKEN_KEY;
  const tokenQueryParam = import.meta.env.VITE_AUTH_TOKEN_QUERY_PARAM?.trim() || DEFAULT_AUTH_QUERY_PARAM;

  const callbackUrl = new URL(
    `${location.pathname}${location.search}${location.hash}`,
    window.location.origin
  );
  const token = readTokenFromCallback(callbackUrl, tokenQueryParam);

  useEffect(() => {
    if (!token) return;

    localStorage.setItem(tokenStorageKey, token);
    localStorage.removeItem(SESSION_EXPIRED_MARKER_KEY);
    navigate('/evaluations', { replace: true });
  }, [navigate, token, tokenStorageKey]);

  if (token) {
    return null;
  }

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
              Sign in callback error
            </Typography>
            <Alert severity="error">
              Authentication callback missing token. Confirm your IdP callback configuration.
            </Alert>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
