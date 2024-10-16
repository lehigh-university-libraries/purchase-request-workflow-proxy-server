package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.amazon_axesso;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmazonAxessoQuotaMonitor {

    private static final String HEADER_REMAINING = "X-RateLimit-Requests-Remaining";

    private final int OVERAGE_ALLOWED;

    private Integer overage;

    AmazonAxessoQuotaMonitor(Config config) {
        OVERAGE_ALLOWED = config.getAmazonAxesso().getQuotaMonitor().getOverageAllowed();

        overage = null;
    }

    /**
     * Increment the amount used and check against the quota.
     * 
     * @return true to allow the call to proceed, false to block it.
     */
    public boolean incrementUsage(CloseableHttpResponse response) {
        Header[] headers = response.getHeaders(HEADER_REMAINING);
        if (headers.length < 1) {
            log.warn("Quota remaining not available.");
            return false;
        }
        Header header = headers[0];
        String remainingValue = header.getValue();
        int remaining = Integer.parseInt(remainingValue);

        log.info("Quota remaining: " + remaining + ", overage: " + overage);

        if (remaining == 0) {
            if (overage == null) {
                overage = 0;
            }
            overage++;

            if (overage > OVERAGE_ALLOWED) {
                log.warn("Exceeded overage allowed.");
                return false;
            }
        }
        return true;
    }

    public static class QuotaException extends RuntimeException {

        QuotaException(String message) {
            super(message);
        }

    }

}
