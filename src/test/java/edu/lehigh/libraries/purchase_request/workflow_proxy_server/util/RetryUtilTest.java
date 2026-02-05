package edu.lehigh.libraries.purchase_request.workflow_proxy_server.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryUtilTest {

    @Test
    void executeWithRetry_successOnFirstAttempt() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryUtil.executeWithRetry(() -> {
            attempts.incrementAndGet();
            return "success";
        });

        assertEquals("success", result);
        assertEquals(1, attempts.get());
    }

    @Test
    void executeWithRetry_successAfterRetries() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryUtil.executeWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Simulated failure");
            }
            return "success";
        }, 5, 10, 100); // Short delays for testing

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void executeWithRetry_failsAfterMaxRetries() {
        AtomicInteger attempts = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            RetryUtil.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 3, 10, 100);
        });

        assertEquals(4, attempts.get()); // Initial + 3 retries
        assertTrue(exception.getMessage().contains("Operation failed after retries"));
    }

    @Test
    void executeWithRetry_runnable_successOnFirstAttempt() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryUtil.executeWithRetry(() -> {
            attempts.incrementAndGet();
        });

        assertEquals(1, attempts.get());
    }

    @Test
    void executeWithRetry_runnable_successAfterRetries() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryUtil.executeWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("Simulated failure");
            }
        }, 3, 10, 100);

        assertEquals(2, attempts.get());
    }

    @Test
    void executeWithRetry_runnable_failsAfterMaxRetries() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            RetryUtil.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 2, 10, 100);
        });

        assertEquals(3, attempts.get()); // Initial + 2 retries
    }

    @Test
    void executeWithRetry_respectsMaxDelay() {
        AtomicInteger attempts = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        RetryUtil.executeWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 4) {
                throw new RuntimeException("Simulated failure");
            }
            return "success";
        }, 5, 50, 100); // baseDelay=50, maxDelay=100

        long elapsed = System.currentTimeMillis() - startTime;
        // With delays: 50 + 100 + 100 = 250ms (capped at 100 after 2nd retry)
        // Allow some tolerance for test execution
        assertTrue(elapsed >= 200, "Expected at least 200ms delay, got " + elapsed);
        assertTrue(elapsed < 500, "Expected less than 500ms delay, got " + elapsed);
    }
}
