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
        return executeWithRetry(null, operation, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * Execute an operation with a description and default retry settings.
     */
    public static <T> T executeWithRetry(String operationName, Supplier<T> operation) {
        return executeWithRetry(operationName, operation, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * Execute an operation with custom retry settings.
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long baseDelayMs, long maxDelayMs) {
        return executeWithRetry(null, operation, maxRetries, baseDelayMs, maxDelayMs);
    }

    /**
     * Execute an operation with custom retry settings.
     *
     * @param operationName Optional name for the operation (for logging)
     * @param operation     The operation to execute
     * @param maxRetries    Maximum number of retry attempts
     * @param baseDelayMs   Initial delay in milliseconds (doubles with each retry)
     * @param maxDelayMs    Maximum delay cap in milliseconds
     */
    public static <T> T executeWithRetry(String operationName, Supplier<T> operation, int maxRetries, long baseDelayMs, long maxDelayMs) {
        String opName = operationName != null ? operationName : "operation";

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 1) {
                    log.info("[Retry] {} succeeded after {} attempts", opName, attempt);
                }
                return result;
            } catch (Exception e) {
                if (attempt > maxRetries) {
                    log.error("[Retry] {} failed after {} attempts. Final error: {} - {}",
                            opName, attempt, e.getClass().getSimpleName(), e.getMessage());
                    throw new RuntimeException("Operation failed after retries: " + opName, e);
                }

                long delay = Math.min(baseDelayMs * (1L << (attempt - 1)), maxDelayMs);
                log.warn("[Retry] {} failed (attempt {}/{}), retrying in {}ms. Error: {} - {}",
                        opName, attempt, maxRetries + 1, delay, e.getClass().getSimpleName(), e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("[Retry] {} interrupted during retry backoff", opName);
                    throw new RuntimeException("Retry interrupted: " + opName, ie);
                }
            }
        }
        throw new RuntimeException("Unreachable");
    }

    /**
     * Execute a void operation with default retry settings.
     */
    public static void executeWithRetry(Runnable operation) {
        executeWithRetry((String) null, operation);
    }

    /**
     * Execute a void operation with a description and default retry settings.
     */
    public static void executeWithRetry(String operationName, Runnable operation) {
        executeWithRetry(operationName, () -> {
            operation.run();
            return null;
        }, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * Execute a void operation with custom retry settings.
     */
    public static void executeWithRetry(Runnable operation, int maxRetries, long baseDelayMs, long maxDelayMs) {
        executeWithRetry(null, () -> {
            operation.run();
            return null;
        }, maxRetries, baseDelayMs, maxDelayMs);
    }
}
