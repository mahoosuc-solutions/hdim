/**
 * Connection status indicator component
 *
 * Displays current WebSocket connection status with visual feedback
 */

import { useMemo } from 'react';
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

export const ConnectionStatus = () => {
  const connectionStatus = useEvaluationStore(state => state.connectionStatus);
  const lastError = useEvaluationStore(state => state.lastError);

  const indicator = useMemo(() => {
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
  }, [connectionStatus]);

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
    if (lastError) {
      return `${indicator.text}: ${lastError.message}`;
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
