import { WebSocketMessageQueue } from './websocket-message-queue';
import { WebSocketMessage, ConnectionState } from '../models/websocket-message.model';

describe('WebSocketMessageQueue', () => {
  let queue: WebSocketMessageQueue;

  const createMockMessage = (type: string, index: number): WebSocketMessage => ({
    type,
    timestamp: Date.now() + index,
    priority: 'low'
  });

  beforeEach(() => {
    queue = new WebSocketMessageQueue(5);
  });

  it('should create instance with specified max size', () => {
    expect(queue).toBeTruthy();
    expect(queue.isEmpty()).toBe(true);
    expect(queue.size()).toBe(0);
  });

  it('should enqueue messages in FIFO order', () => {
    const msg1 = createMockMessage('TYPE_1', 1);
    const msg2 = createMockMessage('TYPE_2', 2);

    queue.enqueue(msg1);
    queue.enqueue(msg2);

    expect(queue.size()).toBe(2);
    expect(queue.dequeue()?.type).toBe('TYPE_1');
    expect(queue.dequeue()?.type).toBe('TYPE_2');
  });

  it('should respect max queue size by removing oldest message', () => {
    const maxSize = 3;
    queue = new WebSocketMessageQueue(maxSize);

    for (let i = 1; i <= 5; i++) {
      queue.enqueue(createMockMessage(`TYPE_${i}`, i));
    }

    // Should only have last 3 messages (TYPE_3, TYPE_4, TYPE_5)
    expect(queue.size()).toBe(maxSize);
    expect(queue.dequeue()?.type).toBe('TYPE_3');
    expect(queue.dequeue()?.type).toBe('TYPE_4');
    expect(queue.dequeue()?.type).toBe('TYPE_5');
  });

  it('should dequeue all messages at once', () => {
    const msg1 = createMockMessage('TYPE_1', 1);
    const msg2 = createMockMessage('TYPE_2', 2);
    const msg3 = createMockMessage('TYPE_3', 3);

    queue.enqueue(msg1);
    queue.enqueue(msg2);
    queue.enqueue(msg3);

    const all = queue.dequeueAll();

    expect(all.length).toBe(3);
    expect(all[0].type).toBe('TYPE_1');
    expect(all[1].type).toBe('TYPE_2');
    expect(all[2].type).toBe('TYPE_3');
    expect(queue.isEmpty()).toBe(true);
  });

  it('should clear all queued messages', () => {
    queue.enqueue(createMockMessage('TYPE_1', 1));
    queue.enqueue(createMockMessage('TYPE_2', 2));

    expect(queue.size()).toBe(2);

    queue.clear();

    expect(queue.size()).toBe(0);
    expect(queue.isEmpty()).toBe(true);
  });

  it('should report correct queue size', () => {
    expect(queue.size()).toBe(0);

    queue.enqueue(createMockMessage('TYPE_1', 1));
    expect(queue.size()).toBe(1);

    queue.enqueue(createMockMessage('TYPE_2', 2));
    expect(queue.size()).toBe(2);

    queue.dequeue();
    expect(queue.size()).toBe(1);
  });

  it('should correctly identify empty queue', () => {
    expect(queue.isEmpty()).toBe(true);

    queue.enqueue(createMockMessage('TYPE_1', 1));
    expect(queue.isEmpty()).toBe(false);

    queue.dequeue();
    expect(queue.isEmpty()).toBe(true);
  });
});
