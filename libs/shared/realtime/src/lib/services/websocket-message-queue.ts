import { WebSocketMessage } from '../models/websocket-message.model';

/**
 * WebSocket Message Queue
 *
 * Queues messages when WebSocket is disconnected.
 * Delivers messages when connection is re-established.
 *
 * Features:
 * - FIFO message ordering
 * - Max size limit with automatic oldest message removal
 * - Batch dequeue for efficient delivery
 * - Thread-safe operations
 */
export class WebSocketMessageQueue {
  private queue: WebSocketMessage[] = [];

  constructor(private maxSize: number = 100) {}

  /**
   * Add message to queue
   * If queue is at max size, removes oldest message
   */
  enqueue(message: WebSocketMessage): void {
    if (this.queue.length >= this.maxSize) {
      // Remove oldest message (FIFO)
      this.queue.shift();
    }

    this.queue.push(message);
  }

  /**
   * Remove and return first message from queue
   */
  dequeue(): WebSocketMessage | undefined {
    return this.queue.shift();
  }

  /**
   * Remove and return all messages from queue at once
   * Clears the queue after returning
   */
  dequeueAll(): WebSocketMessage[] {
    const messages = [...this.queue];
    this.queue = [];
    return messages;
  }

  /**
   * Remove all messages from queue
   */
  clear(): void {
    this.queue = [];
  }

  /**
   * Get current queue size
   */
  size(): number {
    return this.queue.length;
  }

  /**
   * Check if queue is empty
   */
  isEmpty(): boolean {
    return this.queue.length === 0;
  }
}
