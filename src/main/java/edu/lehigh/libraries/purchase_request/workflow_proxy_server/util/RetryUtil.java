package edu.lehigh.libraries.purchase_request.workflow_proxy_server.util;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for executing operations with retry logic and exponential backoff.
 */
public class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long DEFAULT_BASE_DELAY_MS = 1000;
    private static final long DEFAULT_MAX_DELAY_MS = 32000;

    private RetryUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Execute an operation with default retry settings (5 retries, 1s base delay, 32s max delay).
     */
    public static <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * Execute an operation with custom retry settings.
     *
     * @param operation    The operation to execute
     * @param maxRetries   Maximum number of retry attempts
     * @param baseDelayMs  Initial delay in milliseconds (doubles with each retry)
     * @param maxDelayMs   Maximum delay cap in milliseconds
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long baseDelayMs, long maxDelayMs) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("Failed after {} retries", maxRetries);
                    throw new RuntimeException("Operation failed after retries", e);
                }

                long delay = Math.min(baseDelayMs * (1L << attempt), maxDelayMs);
                log.warn("Operation failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt + 1, maxRetries, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        throw new RuntimeException("Unreachable");
    }

    /**
     * Execute a void operation with default retry settings.
     */
    public static void executeWithRetry(Runnable operation) {
        executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * Execute a void operation with custom retry settings.
     */
    public static void executeWithRetry(Runnable operation, int maxRetries, long baseDelayMs, long maxDelayMs) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxRetries, baseDelayMs, maxDelayMs);
    }
}
