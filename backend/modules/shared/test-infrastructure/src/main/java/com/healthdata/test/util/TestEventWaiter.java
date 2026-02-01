package com.healthdata.test.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for coordinating test events without Thread.sleep() anti-pattern.
 *
 * Usage:
 * TestEventWaiter waiter = new TestEventWaiter(1);  // Wait for 1 event
 * service.registerListener(() -> waiter.done());
 * service.processEvent();
 * assertTrue(waiter.await(5, TimeUnit.SECONDS));
 *
 * Replaces: Thread.sleep(1000) followed by assertions
 */
public class TestEventWaiter {

    private final CountDownLatch latch;
    private final AtomicReference<Exception> exception = new AtomicReference<>();
    private final long startTime;

    /**
     * Create waiter expecting specified number of events.
     */
    public TestEventWaiter(int expectedEvents) {
        if (expectedEvents <= 0) {
            throw new IllegalArgumentException("Expected events must be > 0");
        }
        this.latch = new CountDownLatch(expectedEvents);
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Signal one event completion.
     */
    public void done() {
        latch.countDown();
    }

    /**
     * Signal event completion with exception context.
     */
    public void done(Exception e) {
        exception.set(e);
        latch.countDown();
    }

    /**
     * Wait for all expected events.
     *
     * @return true if all events arrived within timeout
     * @throws InterruptedException if wait interrupted
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = latch.await(timeout, unit);
        if (exception.get() != null) {
            throw new RuntimeException("Event processing failed", exception.get());
        }
        return result;
    }

    /**
     * Get elapsed time since waiter creation (for performance assertions).
     */
    public long getElapsedMillis() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Remaining count (for debugging).
     */
    public long getRemaining() {
        return latch.getCount();
    }
}
