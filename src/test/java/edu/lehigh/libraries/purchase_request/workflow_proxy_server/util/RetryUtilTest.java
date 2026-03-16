package edu.lehigh.libraries.purchase_request.workflow_proxy_server.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryUtilTest {

    /**
     * Mock HTTP client that can be configured to fail a specified number of times
     * before returning a successful response. Simulates real HTTP client behavior.
     */
    static class MockHttpClient {
        private final int failuresBeforeSuccess;
        private final AtomicInteger attempts = new AtomicInteger(0);
        private final Class<? extends Exception> exceptionType;

        MockHttpClient(int failuresBeforeSuccess) {
            this(failuresBeforeSuccess, IOException.class);
        }

        MockHttpClient(int failuresBeforeSuccess, Class<? extends Exception> exceptionType) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.exceptionType = exceptionType;
        }

        public String executeGet(String url) {
            int attempt = attempts.incrementAndGet();
            if (attempt <= failuresBeforeSuccess) {
                throw createException("Connection failed on attempt " + attempt);
            }
            return "{\"status\": \"success\", \"data\": \"response from " + url + "\"}";
        }

        private RuntimeException createException(String message) {
            if (exceptionType == ConnectException.class) {
                return new RuntimeException(new ConnectException(message));
            } else if (exceptionType == SocketTimeoutException.class) {
                return new RuntimeException(new SocketTimeoutException(message));
            } else {
                return new RuntimeException(new IOException(message));
            }
        }

        public int getAttemptCount() {
            return attempts.get();
        }
    }

    // === Tests using MockHttpClient ===

    @Test
    void mockClient_successOnFirstAttempt() {
        MockHttpClient client = new MockHttpClient(0);

        String result = RetryUtil.executeWithRetry(() -> client.executeGet("/api/test"));

        assertTrue(result.contains("success"));
        assertEquals(1, client.getAttemptCount());
    }

    @Test
    void mockClient_successAfterTwoFailures() {
        MockHttpClient client = new MockHttpClient(2);

        String result = RetryUtil.executeWithRetry(
            () -> client.executeGet("/api/test"),
            6, 10, 100  // maxAttempts=6, initialInterval=10ms, maxInterval=100ms
        );

        assertTrue(result.contains("success"));
        assertEquals(3, client.getAttemptCount());
    }

    @Test
    void mockClient_failsAfterMaxAttempts() {
        MockHttpClient client = new MockHttpClient(10); // Always fails within attempt limit

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            RetryUtil.executeWithRetry(
                () -> client.executeGet("/api/test"),
                4, 10, 100  // maxAttempts=4
            )
        );

        assertEquals(4, client.getAttemptCount()); // 4 total attempts
        assertTrue(exception.getMessage().contains("Operation failed after retries"));
    }

    @Test
    void mockClient_connectException() {
        MockHttpClient client = new MockHttpClient(2, ConnectException.class);

        String result = RetryUtil.executeWithRetry(
            () -> client.executeGet("/api/test"),
            6, 10, 100
        );

        assertTrue(result.contains("success"));
        assertEquals(3, client.getAttemptCount());
    }

    @Test
    void mockClient_socketTimeoutException() {
        MockHttpClient client = new MockHttpClient(1, SocketTimeoutException.class);

        String result = RetryUtil.executeWithRetry(
            () -> client.executeGet("/api/test"),
            6, 10, 100
        );

        assertTrue(result.contains("success"));
        assertEquals(2, client.getAttemptCount());
    }

    @Test
    void mockClient_exactlyMaxAttemptsNeeded() {
        MockHttpClient client = new MockHttpClient(3); // Fails 3 times, succeeds on 4th

        String result = RetryUtil.executeWithRetry(
            () -> client.executeGet("/api/test"),
            4, 10, 100  // maxAttempts=4 (exactly enough)
        );

        assertTrue(result.contains("success"));
        assertEquals(4, client.getAttemptCount());
    }

    // === Original unit tests ===

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
        }, 6, 10, 100);

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void executeWithRetry_failsAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            RetryUtil.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 4, 10, 100);  // maxAttempts=4
        });

        assertEquals(4, attempts.get());
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
        }, 4, 10, 100);

        assertEquals(2, attempts.get());
    }

    @Test
    void executeWithRetry_runnable_failsAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            RetryUtil.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 3, 10, 100);  // maxAttempts=3
        });

        assertEquals(3, attempts.get());
    }

    @Test
    void executeWithRetry_withOperationName() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryUtil.executeWithRetry("Test operation", () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("Simulated failure");
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }
}
