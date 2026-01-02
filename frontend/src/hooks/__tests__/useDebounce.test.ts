/**
 * Tests for useDebounce hook
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDebounce } from '../useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('initial', 300));
    expect(result.current).toBe('initial');
  });

  it('updates after specified delay', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'initial', delay: 300 },
      }
    );

    expect(result.current).toBe('initial');

    // Update the value
    rerender({ value: 'updated', delay: 300 });

    // Value should still be initial before delay
    expect(result.current).toBe('initial');

    // Fast-forward time and run timers
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    // Value should now be updated
    expect(result.current).toBe('updated');
  });

  it('cancels previous update on new value', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'initial', delay: 300 },
      }
    );

    // First update
    rerender({ value: 'first', delay: 300 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });

    // Second update before first completes
    rerender({ value: 'second', delay: 300 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });

    // Third update before second completes
    rerender({ value: 'third', delay: 300 });

    // Fast-forward past the delay
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    // Should only have the last value
    expect(result.current).toBe('third');
  });

  it('cancels pending update on unmount', () => {
    const clearTimeoutSpy = vi.spyOn(global, 'clearTimeout');
    const { rerender, unmount } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'initial', delay: 300 },
      }
    );

    // Update value to trigger timeout
    rerender({ value: 'updated', delay: 300 });

    // Unmount before delay completes
    unmount();

    // Verify clearTimeout was called
    expect(clearTimeoutSpy).toHaveBeenCalled();
  });

  it('handles rapid value changes correctly', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'v0', delay: 300 },
      }
    );

    // Simulate rapid typing
    const values = ['v1', 'v2', 'v3', 'v4', 'v5'];
    for (const value of values) {
      rerender({ value, delay: 300 });
      await act(async () => {
        await vi.advanceTimersByTimeAsync(50); // Less than the delay
      });
    }

    // Still should have initial value
    expect(result.current).toBe('v0');

    // Fast-forward past the delay
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    // Should have the last value
    expect(result.current).toBe('v5');
  });

  it('works with different data types - string', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'hello', delay: 300 },
      }
    );

    rerender({ value: 'world', delay: 300 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(result.current).toBe('world');
  });

  it('works with different data types - number', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 100, delay: 300 },
      }
    );

    rerender({ value: 200, delay: 300 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(result.current).toBe(200);
  });

  it('works with different data types - object', async () => {
    const initialObj = { name: 'John', age: 30 };
    const updatedObj = { name: 'Jane', age: 25 };

    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: initialObj, delay: 300 },
      }
    );

    expect(result.current).toEqual(initialObj);

    rerender({ value: updatedObj, delay: 300 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(result.current).toEqual(updatedObj);
  });

  it('uses default delay of 300ms when not specified', async () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebounce(value),
      {
        initialProps: { value: 'initial' },
      }
    );

    rerender({ value: 'updated' });

    // Should not update before 300ms
    await act(async () => {
      await vi.advanceTimersByTimeAsync(299);
    });
    expect(result.current).toBe('initial');

    // Should update after 300ms
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1);
    });
    expect(result.current).toBe('updated');
  });

  it('handles zero delay', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'initial', delay: 0 },
      }
    );

    rerender({ value: 'updated', delay: 0 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });

    expect(result.current).toBe('updated');
  });

  it('handles changing delay value', async () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'initial', delay: 300 },
      }
    );

    // Update with longer delay
    rerender({ value: 'updated', delay: 500 });

    // Should not update after original delay
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    expect(result.current).toBe('initial');

    // Should update after new delay
    await act(async () => {
      await vi.advanceTimersByTimeAsync(200);
    });
    expect(result.current).toBe('updated');
  });
});
