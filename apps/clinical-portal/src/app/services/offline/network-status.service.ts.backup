/**
 * Issue #24: Offline Mode & Sync
 * Network status detection service
 *
 * Monitors network connectivity and provides reactive state updates
 * for components to respond to online/offline transitions.
 */
import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, fromEvent, merge, Subject, timer } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, takeUntil, tap } from 'rxjs/operators';

export interface NetworkState {
  isOnline: boolean;
  effectiveType: 'slow-2g' | '2g' | '3g' | '4g' | 'unknown';
  downlink: number | null;
  rtt: number | null;
  saveData: boolean;
  lastChecked: number;
}

interface NetworkInformation {
  effectiveType: 'slow-2g' | '2g' | '3g' | '3g' | '4g';
  downlink: number;
  rtt: number;
  saveData: boolean;
  addEventListener: (type: string, listener: EventListener) => void;
  removeEventListener: (type: string, listener: EventListener) => void;
}

@Injectable({
  providedIn: 'root',
})
export class NetworkStatusService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly networkState = new BehaviorSubject<NetworkState>(this.getCurrentState());
  private readonly connectionChangeListener: EventListener;

  // Public observables
  readonly state$: Observable<NetworkState> = this.networkState.asObservable();
  readonly isOnline$: Observable<boolean> = this.state$.pipe(
    map((state) => state.isOnline),
    distinctUntilChanged()
  );
  readonly isOffline$: Observable<boolean> = this.isOnline$.pipe(map((online) => !online));
  readonly connectionQuality$: Observable<'good' | 'poor' | 'offline'> = this.state$.pipe(
    map((state) => this.getConnectionQuality(state)),
    distinctUntilChanged()
  );

  constructor(private ngZone: NgZone) {
    this.connectionChangeListener = () => this.updateNetworkState();
    this.initializeListeners();
    this.startPeriodicCheck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.removeNetworkInfoListener();
  }

  /**
   * Get current online status synchronously
   */
  get isOnline(): boolean {
    return this.networkState.value.isOnline;
  }

  /**
   * Get current network state synchronously
   */
  get currentState(): NetworkState {
    return this.networkState.value;
  }

  /**
   * Force a network check
   */
  checkNow(): void {
    this.updateNetworkState();
  }

  /**
   * Perform an actual connectivity test by hitting the server
   */
  async performConnectivityTest(testUrl?: string): Promise<boolean> {
    const url = testUrl || '/api/health';

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);

      const response = await fetch(url, {
        method: 'HEAD',
        cache: 'no-cache',
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      return response.ok;
    } catch {
      return false;
    }
  }

  /**
   * Initialize event listeners for network changes
   */
  private initializeListeners(): void {
    // Listen for online/offline events
    this.ngZone.runOutsideAngular(() => {
      merge(
        fromEvent(window, 'online'),
        fromEvent(window, 'offline')
      )
        .pipe(
          debounceTime(100),
          takeUntil(this.destroy$)
        )
        .subscribe(() => {
          this.ngZone.run(() => this.updateNetworkState());
        });
    });

    // Listen for Network Information API changes if available
    this.addNetworkInfoListener();
  }

  /**
   * Add listener for Network Information API (if available)
   */
  private addNetworkInfoListener(): void {
    const connection = this.getNetworkInformation();
    if (connection) {
      connection.addEventListener('change', this.connectionChangeListener);
    }
  }

  /**
   * Remove Network Information API listener
   */
  private removeNetworkInfoListener(): void {
    const connection = this.getNetworkInformation();
    if (connection) {
      connection.removeEventListener('change', this.connectionChangeListener);
    }
  }

  /**
   * Get Network Information API object if available
   */
  private getNetworkInformation(): NetworkInformation | null {
    const nav = navigator as Navigator & {
      connection?: NetworkInformation;
      mozConnection?: NetworkInformation;
      webkitConnection?: NetworkInformation;
    };

    return nav.connection || nav.mozConnection || nav.webkitConnection || null;
  }

  /**
   * Start periodic connectivity checks
   */
  private startPeriodicCheck(): void {
    // Check every 30 seconds when online, every 5 seconds when offline
    timer(0, 5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        const currentlyOnline = this.networkState.value.isOnline;
        // Only do periodic checks if offline (more aggressive reconnection detection)
        if (!currentlyOnline) {
          this.updateNetworkState();
        }
      });

    // Less frequent check when online
    timer(30000, 30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.networkState.value.isOnline) {
          this.updateNetworkState();
        }
      });
  }

  /**
   * Update network state
   */
  private updateNetworkState(): void {
    const newState = this.getCurrentState();
    const currentState = this.networkState.value;

    // Only emit if state actually changed
    if (
      newState.isOnline !== currentState.isOnline ||
      newState.effectiveType !== currentState.effectiveType ||
      newState.saveData !== currentState.saveData
    ) {
      this.networkState.next(newState);

      // Log significant changes
      if (newState.isOnline !== currentState.isOnline) {
        console.log(
          `Network status changed: ${newState.isOnline ? 'ONLINE' : 'OFFLINE'}`,
          newState
        );
      }
    }
  }

  /**
   * Get the current network state
   */
  private getCurrentState(): NetworkState {
    const connection = this.getNetworkInformation();

    return {
      isOnline: navigator.onLine,
      effectiveType: connection?.effectiveType || 'unknown',
      downlink: connection?.downlink || null,
      rtt: connection?.rtt || null,
      saveData: connection?.saveData || false,
      lastChecked: Date.now(),
    };
  }

  /**
   * Determine connection quality from state
   */
  private getConnectionQuality(state: NetworkState): 'good' | 'poor' | 'offline' {
    if (!state.isOnline) {
      return 'offline';
    }

    if (state.effectiveType === 'slow-2g' || state.effectiveType === '2g') {
      return 'poor';
    }

    if (state.rtt !== null && state.rtt > 500) {
      return 'poor';
    }

    if (state.downlink !== null && state.downlink < 1) {
      return 'poor';
    }

    return 'good';
  }
}
