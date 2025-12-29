/**
 * WebSocket Helpers for Event-Driven Testing
 *
 * Provides utilities to connect to and interact with WebSocket
 * endpoints for testing real-time updates and Kafka event-driven flows.
 */

export interface WebSocketHelpersOptions {
  endpoint: string;
  tenantId: string;
  reconnectAttempts?: number;
  reconnectDelay?: number;
  messageTimeout?: number;
}

export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp: number;
  correlationId?: string;
}

export interface EventSubscription {
  eventType: string;
  callback: (message: WebSocketMessage) => void;
  filter?: (message: WebSocketMessage) => boolean;
}

export class WebSocketHelpers {
  private options: Required<WebSocketHelpersOptions>;
  private ws: WebSocket | null = null;
  private messageQueue: WebSocketMessage[] = [];
  private subscriptions: Map<string, EventSubscription[]> = new Map();
  private pendingMessages: Map<string, {
    resolve: (message: WebSocketMessage) => void;
    reject: (error: Error) => void;
    timeout: NodeJS.Timeout;
  }> = new Map();
  private connectionPromise: Promise<void> | null = null;
  private isConnected: boolean = false;

  constructor(options: WebSocketHelpersOptions) {
    this.options = {
      endpoint: options.endpoint,
      tenantId: options.tenantId,
      reconnectAttempts: options.reconnectAttempts ?? 3,
      reconnectDelay: options.reconnectDelay ?? 1000,
      messageTimeout: options.messageTimeout ?? 30000,
    };
  }

  /**
   * Connect to WebSocket endpoint
   */
  async connect(): Promise<void> {
    if (this.isConnected) {
      return;
    }

    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.connectionPromise = this.establishConnection();
    await this.connectionPromise;
  }

  private async establishConnection(attempt: number = 1): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        // Construct WebSocket URL with tenant context
        const url = new URL(this.options.endpoint);
        url.searchParams.set('tenantId', this.options.tenantId);

        this.ws = new WebSocket(url.toString());

        this.ws.onopen = () => {
          console.log(`WebSocket connected to ${url.toString()}`);
          this.isConnected = true;
          this.connectionPromise = null;
          resolve();
        };

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data);
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          if (!this.isConnected) {
            reject(new Error('WebSocket connection failed'));
          }
        };

        this.ws.onclose = () => {
          console.log('WebSocket disconnected');
          this.isConnected = false;
          this.ws = null;

          // Attempt reconnection if not intentionally closed
          if (attempt < this.options.reconnectAttempts) {
            setTimeout(() => {
              this.establishConnection(attempt + 1);
            }, this.options.reconnectDelay * attempt);
          }
        };

        // Connection timeout
        setTimeout(() => {
          if (!this.isConnected) {
            this.ws?.close();
            reject(new Error(`WebSocket connection timeout after ${this.options.messageTimeout}ms`));
          }
        }, this.options.messageTimeout);

      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket
   */
  async disconnect(): Promise<void> {
    if (this.ws) {
      this.ws.close(1000, 'Test completed');
      this.ws = null;
      this.isConnected = false;
    }

    // Clear pending messages
    for (const [, pending] of this.pendingMessages) {
      clearTimeout(pending.timeout);
      pending.reject(new Error('WebSocket disconnected'));
    }
    this.pendingMessages.clear();

    // Clear subscriptions
    this.subscriptions.clear();

    // Clear message queue
    this.messageQueue = [];
  }

  /**
   * Handle incoming message
   */
  private handleMessage(data: string): void {
    try {
      const message: WebSocketMessage = JSON.parse(data);
      message.timestamp = message.timestamp || Date.now();

      // Add to queue
      this.messageQueue.push(message);

      // Check pending messages
      const pending = this.pendingMessages.get(message.type);
      if (pending) {
        clearTimeout(pending.timeout);
        pending.resolve(message);
        this.pendingMessages.delete(message.type);
      }

      // Notify subscribers
      const subs = this.subscriptions.get(message.type) || [];
      for (const sub of subs) {
        if (!sub.filter || sub.filter(message)) {
          sub.callback(message);
        }
      }

      // Also notify wildcard subscribers
      const wildcardSubs = this.subscriptions.get('*') || [];
      for (const sub of wildcardSubs) {
        if (!sub.filter || sub.filter(message)) {
          sub.callback(message);
        }
      }

    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
    }
  }

  /**
   * Send a message through WebSocket
   */
  async send(type: string, payload: any, correlationId?: string): Promise<void> {
    if (!this.isConnected || !this.ws) {
      throw new Error('WebSocket not connected');
    }

    const message: WebSocketMessage = {
      type,
      payload,
      timestamp: Date.now(),
      correlationId: correlationId || crypto.randomUUID(),
    };

    this.ws.send(JSON.stringify(message));
  }

  /**
   * Wait for a specific event type
   */
  async waitForEvent(
    eventType: string,
    timeout?: number,
    filter?: (message: WebSocketMessage) => boolean
  ): Promise<WebSocketMessage> {
    const timeoutMs = timeout || this.options.messageTimeout;

    // Check if event already in queue
    const existing = this.messageQueue.find(
      (m) => m.type === eventType && (!filter || filter(m))
    );
    if (existing) {
      this.messageQueue = this.messageQueue.filter((m) => m !== existing);
      return existing;
    }

    return new Promise((resolve, reject) => {
      const timeoutHandle = setTimeout(() => {
        this.pendingMessages.delete(eventType);
        reject(new Error(`Timeout waiting for event: ${eventType}`));
      }, timeoutMs);

      this.pendingMessages.set(eventType, {
        resolve: (message) => {
          if (!filter || filter(message)) {
            resolve(message);
          }
        },
        reject,
        timeout: timeoutHandle,
      });
    });
  }

  /**
   * Subscribe to an event type
   */
  subscribe(
    eventType: string,
    callback: (message: WebSocketMessage) => void,
    filter?: (message: WebSocketMessage) => boolean
  ): () => void {
    const subscription: EventSubscription = { eventType, callback, filter };

    const existing = this.subscriptions.get(eventType) || [];
    this.subscriptions.set(eventType, [...existing, subscription]);

    // Return unsubscribe function
    return () => {
      const subs = this.subscriptions.get(eventType) || [];
      this.subscriptions.set(
        eventType,
        subs.filter((s) => s !== subscription)
      );
    };
  }

  /**
   * Get all received messages of a type
   */
  getMessages(eventType?: string): WebSocketMessage[] {
    if (eventType) {
      return this.messageQueue.filter((m) => m.type === eventType);
    }
    return [...this.messageQueue];
  }

  /**
   * Clear message queue
   */
  clearMessages(): void {
    this.messageQueue = [];
  }

  // ==================== HDIM-Specific Event Helpers ====================

  /**
   * Wait for evaluation complete event
   */
  async waitForEvaluationComplete(
    patientId?: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'EVALUATION_COMPLETE',
      timeout,
      patientId ? (m) => m.payload?.patientId === patientId : undefined
    );
  }

  /**
   * Wait for care gap created event
   */
  async waitForCareGapCreated(
    patientId?: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'CARE_GAP_CREATED',
      timeout,
      patientId ? (m) => m.payload?.patientId === patientId : undefined
    );
  }

  /**
   * Wait for care gap closed event
   */
  async waitForCareGapClosed(
    gapId?: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'CARE_GAP_CLOSED',
      timeout,
      gapId ? (m) => m.payload?.gapId === gapId : undefined
    );
  }

  /**
   * Wait for batch evaluation progress event
   */
  async waitForBatchProgress(
    batchId: string,
    expectedProgress?: number,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'BATCH_PROGRESS',
      timeout,
      (m) =>
        m.payload?.batchId === batchId &&
        (expectedProgress === undefined || m.payload?.progress >= expectedProgress)
    );
  }

  /**
   * Wait for batch evaluation complete event
   */
  async waitForBatchComplete(
    batchId: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'BATCH_COMPLETE',
      timeout,
      (m) => m.payload?.batchId === batchId
    );
  }

  /**
   * Wait for patient data sync event
   */
  async waitForPatientSync(
    patientId?: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'PATIENT_DATA_SYNCED',
      timeout,
      patientId ? (m) => m.payload?.patientId === patientId : undefined
    );
  }

  /**
   * Wait for report generation complete
   */
  async waitForReportComplete(
    reportId?: string,
    timeout?: number
  ): Promise<WebSocketMessage> {
    return this.waitForEvent(
      'REPORT_GENERATED',
      timeout,
      reportId ? (m) => m.payload?.reportId === reportId : undefined
    );
  }

  /**
   * Collect events during a test action
   */
  async collectEventsDuring<T>(
    eventTypes: string[],
    action: () => Promise<T>
  ): Promise<{ result: T; events: WebSocketMessage[] }> {
    const collectedEvents: WebSocketMessage[] = [];

    // Subscribe to all event types
    const unsubscribes = eventTypes.map((type) =>
      this.subscribe(type, (message) => {
        collectedEvents.push(message);
      })
    );

    try {
      const result = await action();

      // Small delay to catch trailing events
      await new Promise(resolve => setTimeout(resolve, 100));

      return { result, events: collectedEvents };
    } finally {
      // Unsubscribe all
      unsubscribes.forEach((unsub) => unsub());
    }
  }
}
