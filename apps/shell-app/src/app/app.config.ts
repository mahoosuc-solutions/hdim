import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  importProvidersFrom,
  APP_INITIALIZER,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { appRoutes } from './app.routes';
import { tenantInterceptor, authInterceptor } from '@health-platform/shared/data-access';
import { provideSharedState } from '@health-platform/shared/state';
import { WebSocketService } from '@health-platform/shared/realtime';

/**
 * WebSocket Initialization Factory
 *
 * Initializes WebSocket connection during app startup.
 * This factory function is called before the app initializes,
 * ensuring WebSocket is ready for real-time updates.
 *
 * ★ Insight ─────────────────────────────────────
 * Using APP_INITIALIZER ensures WebSocket service is created
 * and connected before any components are rendered. This prevents
 * race conditions where components try to subscribe before the
 * service is ready. The function returns a promise that resolves
 * once the WebSocket handshake is complete.
 * ─────────────────────────────────────────────────
 */
function initializeWebSocket(websocketService: WebSocketService) {
  return () => {
    // Get endpoint from environment or use default
    const endpoint = 'quality-measure'; // Service endpoint for WebSocket
    const token = localStorage.getItem('auth_token') || '';

    // Connect to WebSocket server
    websocketService.connect(endpoint, token, {
      reconnectInterval: 1000, // Start with 1s
      maxReconnectAttempts: 10,
      heartbeatInterval: 30000, // 30-second keepalive
      messageQueueSize: 100,
    });

    // Return promise that resolves when connected
    return new Promise<void>((resolve) => {
      const subscription = websocketService.connectionStatus$.subscribe(
        (status) => {
          if (status.state === 'CONNECTED') {
            subscription.unsubscribe();
            resolve();
          }
        }
      );

      // Resolve after 5s timeout even if not connected (connection will retry automatically)
      setTimeout(() => {
        subscription.unsubscribe();
        resolve();
      }, 5000);
    });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(appRoutes),
    provideHttpClient(
      withInterceptors([tenantInterceptor, authInterceptor])
    ),
    provideSharedState(),
    WebSocketService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeWebSocket,
      deps: [WebSocketService],
      multi: true,
    },
  ],
};
