package edu.lehigh.libraries.purchase_request.workflow_proxy_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {

    public static final int DEFAULT_MAX_ATTEMPTS = 6;
    public static final long DEFAULT_INITIAL_INTERVAL_MS = 1000;
    public static final double DEFAULT_MULTIPLIER = 2.0;
    public static final long DEFAULT_MAX_INTERVAL_MS = 32000;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(DEFAULT_INITIAL_INTERVAL_MS);
        backOffPolicy.setMultiplier(DEFAULT_MULTIPLIER);
        backOffPolicy.setMaxInterval(DEFAULT_MAX_INTERVAL_MS);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
