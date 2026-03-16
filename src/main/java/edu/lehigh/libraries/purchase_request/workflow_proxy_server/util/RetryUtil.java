package edu.lehigh.libraries.purchase_request.workflow_proxy_server.util;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Utility class for executing operations with retry logic and exponential backoff.
 * Uses Spring Retry's RetryTemplate for idiomatic Spring retry handling.
 */
public class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private static final int DEFAULT_MAX_ATTEMPTS = 6;
    private static final long DEFAULT_INITIAL_INTERVAL_MS = 1000;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_INTERVAL_MS = 32000;

    private RetryUtil() {
        // Utility class, prevent instantiation
    }

    private static RetryTemplate createRetryTemplate(int maxAttempts, long initialInterval,
                                                      double multiplier, long maxInterval) {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    /**
     * Execute an operation with default retry settings (6 attempts, 1s initial delay, 32s max delay).
     */
    public static <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(null, operation);
    }

    /**
     * Execute an operation with a description and default retry settings.
     */
    public static <T> T executeWithRetry(String operationName, Supplier<T> operation) {
        return executeWithRetry(operationName, operation, DEFAULT_MAX_ATTEMPTS,
                DEFAULT_INITIAL_INTERVAL_MS, DEFAULT_MAX_INTERVAL_MS);
    }

    /**
     * Execute an operation with custom retry settings.
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxAttempts,
                                          long initialIntervalMs, long maxIntervalMs) {
        return executeWithRetry(null, operation, maxAttempts, initialIntervalMs, maxIntervalMs);
    }

    /**
     * Execute an operation with custom retry settings.
     *
     * @param operationName     Optional name for the operation (for logging)
     * @param operation         The operation to execute
     * @param maxAttempts       Maximum number of attempts (including initial)
     * @param initialIntervalMs Initial delay in milliseconds
     * @param maxIntervalMs     Maximum delay cap in milliseconds
     */
    public static <T> T executeWithRetry(String operationName, Supplier<T> operation,
                                          int maxAttempts, long initialIntervalMs, long maxIntervalMs) {
        String opName = operationName != null ? operationName : "operation";

        RetryTemplate retryTemplate = createRetryTemplate(maxAttempts, initialIntervalMs,
                DEFAULT_MULTIPLIER, maxIntervalMs);

        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                                                        Throwable throwable) {
                if (throwable == null && context.getRetryCount() > 0) {
                    log.info("[Retry] {} succeeded after {} attempts", opName, context.getRetryCount() + 1);
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                          Throwable throwable) {
                int attempt = context.getRetryCount();
                if (attempt < maxAttempts - 1) {
                    long delay = Math.min(initialIntervalMs * (1L << attempt), maxIntervalMs);
                    log.warn("[Retry] {} failed (attempt {}/{}), retrying in {}ms. Error: {} - {}",
                            opName, attempt + 1, maxAttempts, delay,
                            throwable.getClass().getSimpleName(), throwable.getMessage());
                } else {
                    log.error("[Retry] {} failed after {} attempts. Final error: {} - {}",
                            opName, maxAttempts, throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }
        });

        try {
            return retryTemplate.execute(context -> operation.get());
        } catch (Exception e) {
            throw new RuntimeException("Operation failed after retries: " + opName, e);
        }
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
        });
    }

    /**
     * Execute a void operation with custom retry settings.
     */
    public static void executeWithRetry(Runnable operation, int maxAttempts,
                                         long initialIntervalMs, long maxIntervalMs) {
        executeWithRetry(null, () -> {
            operation.run();
            return null;
        }, maxAttempts, initialIntervalMs, maxIntervalMs);
    }
}
