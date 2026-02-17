import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Card, CardContent, Stack, Typography } from '@mui/material';

const DEFAULT_AUTH_TOKEN_KEY = 'authToken';
const DEFAULT_AUTH_QUERY_PARAM = 'access_token';
const SESSION_EXPIRED_MARKER_KEY = 'sessionExpiredAt';
const PENDING_SMART_AUTH_KEY = 'pendingSmartAuth';

type PendingSmartAuth = {
  state: string;
  codeVerifier?: string;
  createdAt: string;
};

function toBase64Url(input: Uint8Array): string {
  const base64 = btoa(String.fromCharCode(...input));
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function generateRandomString(size = 32): string {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return toBase64Url(bytes);
}

async function createPkcePair(): Promise<{ verifier: string; challenge: string }> {
  const verifier = generateRandomString(48);
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  const challenge = toBase64Url(new Uint8Array(digest));
  return { verifier, challenge };
}

function readTokenFromCallback(url: URL, tokenQueryParam: string): string | null {
  const fromQuery = url.searchParams.get(tokenQueryParam);
  if (fromQuery) {
    return fromQuery;
  }

  const hashParams = new URLSearchParams(url.hash.startsWith('#') ? url.hash.slice(1) : url.hash);
  return hashParams.get(tokenQueryParam);
}

function readPendingSmartAuth(): PendingSmartAuth | null {
  const raw = localStorage.getItem(PENDING_SMART_AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as PendingSmartAuth;
  } catch {
    return null;
  }
}

function clearPendingSmartAuth() {
  localStorage.removeItem(PENDING_SMART_AUTH_KEY);
}

function persistPendingSmartAuth(value: PendingSmartAuth) {
  localStorage.setItem(PENDING_SMART_AUTH_KEY, JSON.stringify(value));
}

async function exchangeAuthorizationCode(params: {
  tokenUrl: string;
  code: string;
  clientId: string;
  redirectUri: string;
  codeVerifier?: string;
}): Promise<string | null> {
  const body = new URLSearchParams();
  body.set('grant_type', 'authorization_code');
  body.set('code', params.code);
  body.set('client_id', params.clientId);
  body.set('redirect_uri', params.redirectUri);
  if (params.codeVerifier) {
    body.set('code_verifier', params.codeVerifier);
  }

  const response = await fetch(params.tokenUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  });

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as { access_token?: string };
  return payload.access_token ?? null;
}

export function AuthCallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const tokenStorageKey = import.meta.env.VITE_AUTH_TOKEN_STORAGE_KEY?.trim() || DEFAULT_AUTH_TOKEN_KEY;
  const tokenQueryParam = import.meta.env.VITE_AUTH_TOKEN_QUERY_PARAM?.trim() || DEFAULT_AUTH_QUERY_PARAM;
  const smartAuthorizeUrl = import.meta.env.VITE_SMART_AUTHORIZE_URL?.trim() || '/oauth/authorize';
  const smartTokenUrl = import.meta.env.VITE_SMART_TOKEN_URL?.trim() || '/oauth/token';
  const smartClientId = import.meta.env.VITE_SMART_CLIENT_ID?.trim() || 'clinical-portal';
  const smartScope =
    import.meta.env.VITE_SMART_SCOPE?.trim() || 'launch/patient launch/encounter openid fhirUser';
  const smartRedirectUri =
    import.meta.env.VITE_SMART_REDIRECT_URI?.trim() || `${window.location.origin}/auth/callback`;
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [launching, setLaunching] = useState(false);
  const [exchangingCode, setExchangingCode] = useState(false);

  const callbackUrl = new URL(
    `${location.pathname}${location.search}${location.hash}`,
    window.location.origin
  );
  const token = readTokenFromCallback(callbackUrl, tokenQueryParam);
  const code = callbackUrl.searchParams.get('code');
  const iss = callbackUrl.searchParams.get('iss');
  const launch = callbackUrl.searchParams.get('launch');
  const state = callbackUrl.searchParams.get('state');
  const oauthError = callbackUrl.searchParams.get('error');
  const oauthErrorDescription = callbackUrl.searchParams.get('error_description');
  const callbackError = useMemo(() => {
    if (oauthError) {
      return oauthErrorDescription
        ? `Authorization error: ${oauthErrorDescription}`
        : `Authorization error: ${oauthError}`;
    }
    return null;
  }, [oauthError, oauthErrorDescription]);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      if (callbackError) {
        setErrorMessage(callbackError);
        return;
      }

      if (iss && !token && !code) {
        setLaunching(true);
        const pendingState = generateRandomString(24);
        const { verifier, challenge } = await createPkcePair();
        if (cancelled) return;

        persistPendingSmartAuth({
          state: pendingState,
          codeVerifier: verifier,
          createdAt: new Date().toISOString(),
        });

        const authorizeUrl = new URL(smartAuthorizeUrl, window.location.origin);
        authorizeUrl.searchParams.set('response_type', 'code');
        authorizeUrl.searchParams.set('client_id', smartClientId);
        authorizeUrl.searchParams.set('redirect_uri', smartRedirectUri);
        authorizeUrl.searchParams.set('scope', smartScope);
        authorizeUrl.searchParams.set('state', pendingState);
        authorizeUrl.searchParams.set('aud', iss);
        authorizeUrl.searchParams.set('code_challenge', challenge);
        authorizeUrl.searchParams.set('code_challenge_method', 'S256');
        if (launch) {
          authorizeUrl.searchParams.set('launch', launch);
        }

        window.location.assign(authorizeUrl.toString());
        return;
      }

      const pendingAuth = readPendingSmartAuth();
      if (state && pendingAuth?.state && pendingAuth.state !== state) {
        clearPendingSmartAuth();
        setErrorMessage('State mismatch detected during SMART callback.');
        return;
      }

      if (token) {
        localStorage.setItem(tokenStorageKey, token);
        localStorage.removeItem(SESSION_EXPIRED_MARKER_KEY);
        clearPendingSmartAuth();
        navigate('/evaluations', { replace: true });
        return;
      }

      if (code) {
        setExchangingCode(true);
        const accessToken = await exchangeAuthorizationCode({
          tokenUrl: smartTokenUrl,
          code,
          clientId: smartClientId,
          redirectUri: smartRedirectUri,
          codeVerifier: pendingAuth?.codeVerifier,
        });
        if (cancelled) return;
        setExchangingCode(false);

        if (!accessToken) {
          setErrorMessage('Authorization code exchange failed. Verify SMART token endpoint settings.');
          return;
        }

        localStorage.setItem(tokenStorageKey, accessToken);
        localStorage.removeItem(SESSION_EXPIRED_MARKER_KEY);
        clearPendingSmartAuth();
        navigate('/evaluations', { replace: true });
        return;
      }

      setErrorMessage(
        'Authentication callback missing token/code. Confirm your IdP callback configuration.'
      );
    };

    void run();
    return () => {
      cancelled = true;
    };
  }, [
    callbackError,
    code,
    iss,
    launch,
    navigate,
    smartAuthorizeUrl,
    smartClientId,
    smartRedirectUri,
    smartScope,
    smartTokenUrl,
    state,
    token,
    tokenStorageKey,
  ]);

  if (token || code) {
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
              {launching || exchangingCode ? 'Completing SMART sign in' : 'Sign in callback error'}
            </Typography>
            {launching || exchangingCode ? (
              <Alert severity="info">
                {launching
                  ? 'Starting SMART authorization flow...'
                  : 'Exchanging authorization code for access token...'}
              </Alert>
            ) : (
              <Alert severity="error">
                {errorMessage ??
                  'Authentication callback missing token/code. Confirm your IdP callback configuration.'}
              </Alert>
            )}
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
