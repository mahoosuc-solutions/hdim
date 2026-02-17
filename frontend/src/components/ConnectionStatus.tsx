/**
 * Connection status indicator component
 *
 * Displays current WebSocket connection status with visual feedback
 */

import { useMemo, useState } from 'react';
import { Chip, Tooltip, Box } from '@mui/material';
import {
  WifiOff,
  Wifi,
  SyncProblem,
  Error as ErrorIcon,
  Sync
} from '@mui/icons-material';
import { useEvaluationStore } from '../store/evaluationStore';
import { ConnectionStatus as Status } from '../services/websocket.service';
import { ApiClient } from '../app/integrations/client';
import { useIntegrationsHealth } from '../app/integrations/useIntegrationsHealth';
import { createSessionExpiryHandler } from '../app/integrations/sessionExpiry';

const DEV_FORCE_401_KEY = 'devForceIntegrations401';

export const ConnectionStatus = () => {
  const connectionStatus = useEvaluationStore(state => state.connectionStatus);
  const lastError = useEvaluationStore(state => state.lastError);
  const isDev = import.meta.env.DEV;
  const integrationsHealthEnabled = import.meta.env.VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK === 'true';
  const integrationsPollIntervalMs = Number.parseInt(
    import.meta.env.VITE_INTEGRATIONS_HEALTH_POLL_MS ?? '5000',
    10
  );
  const integrationsBaseUrl = import.meta.env.VITE_INTEGRATIONS_API_URL ?? '';
  const sessionExpiredRedirectUrl = import.meta.env.VITE_SESSION_EXPIRED_REDIRECT_URL ?? '/login';
  const [force401Enabled, setForce401Enabled] = useState(() => localStorage.getItem(DEV_FORCE_401_KEY) === 'true');

  const integrationsClient = useMemo(
    () =>
      new ApiClient({
        baseUrl: integrationsBaseUrl,
        tokenProvider: () => localStorage.getItem('authToken'),
        shouldForceUnauthorized: (path) =>
          isDev &&
          force401Enabled &&
          path === '/health/integrations',
      }),
    [integrationsBaseUrl, isDev, force401Enabled]
  );
  const handleSessionExpired = useMemo(
    () =>
      createSessionExpiryHandler({
        redirectUrl: sessionExpiredRedirectUrl,
      }),
    [sessionExpiredRedirectUrl]
  );
  const { status: integrationsStatus, sessionExpired } = useIntegrationsHealth({
    client: integrationsClient,
    enabled: integrationsHealthEnabled,
    intervalMs: Number.isNaN(integrationsPollIntervalMs) ? 5000 : integrationsPollIntervalMs,
    onSessionExpired: handleSessionExpired,
  });

  const toggleForce401 = () => {
    const next = !force401Enabled;
    setForce401Enabled(next);
    localStorage.setItem(DEV_FORCE_401_KEY, `${next}`);
  };

  const indicator = useMemo(() => {
    if (sessionExpired) {
      return { status: Status.ERROR, color: 'error', text: 'Session Expired' };
    }

    switch (connectionStatus) {
      case Status.CONNECTED:
        return { status: connectionStatus, color: 'success', text: 'Connected' };
      case Status.CONNECTING:
        return { status: connectionStatus, color: 'info', text: 'Connecting...' };
      case Status.RECONNECTING:
        return { status: connectionStatus, color: 'warning', text: 'Reconnecting...' };
      case Status.DISCONNECTED:
        return { status: connectionStatus, color: 'default', text: 'Disconnected' };
      case Status.ERROR:
        return { status: connectionStatus, color: 'error', text: 'Connection Error' };
      default:
        return { status: connectionStatus, color: 'default', text: 'Unknown' };
    }
  }, [connectionStatus, sessionExpired]);

  const getIcon = () => {
    switch (indicator.status) {
      case Status.CONNECTED:
        return <Wifi />;
      case Status.CONNECTING:
        return <Sync className="rotating" />;
      case Status.RECONNECTING:
        return <SyncProblem />;
      case Status.DISCONNECTED:
        return <WifiOff />;
      case Status.ERROR:
        return <ErrorIcon />;
      default:
        return <WifiOff />;
    }
  };

  const getTooltip = () => {
    if (sessionExpired) {
      return 'Unauthorized - session expired. Re-authenticate to resume integration health checks.';
    }
    if (lastError) {
      return `${indicator.text}: ${lastError.message}`;
    }
    if (integrationsHealthEnabled && integrationsStatus === 'error') {
      return `${indicator.text} (integrations health unavailable)`;
    }
    return indicator.text;
  };

  return (
    <Box>
      <Tooltip title={getTooltip()} arrow>
        <Chip
          icon={getIcon()}
          label={indicator.text}
          color={indicator.color as any}
          size="small"
          variant={indicator.status === Status.CONNECTED ? 'filled' : 'outlined'}
        />
      </Tooltip>
      {isDev && integrationsHealthEnabled && (
        <Chip
          size="small"
          variant="outlined"
          color={force401Enabled ? 'warning' : 'default'}
          label={`Force 401: ${force401Enabled ? 'On' : 'Off'}`}
          onClick={toggleForce401}
          sx={{ ml: 1 }}
        />
      )}
      <style>{`
        @keyframes rotate {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        .rotating {
          animation: rotate 2s linear infinite;
        }
      `}</style>
    </Box>
  );
};
