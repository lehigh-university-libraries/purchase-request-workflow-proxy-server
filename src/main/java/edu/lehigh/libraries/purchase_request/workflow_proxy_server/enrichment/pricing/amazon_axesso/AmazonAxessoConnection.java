package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.amazon_axesso;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmazonAxessoConnection {
    
    private final String API_HOST;
    private final String API_KEY;

    private AmazonAxessoQuotaMonitor quotaMonitor;
    private boolean futureCallsAllowed = true;
    private CloseableHttpClient client;
 
    AmazonAxessoConnection(Config config, String API_HOST) {
        this.API_HOST = API_HOST;
        this.API_KEY = config.getAmazonAxesso().getApiKey();
        this.quotaMonitor = new AmazonAxessoQuotaMonitor(config);
        this.futureCallsAllowed = true;
        initConnection();
    }

    private void initConnection() {
        client = HttpClientBuilder.create().build();
    }

    public JSONObject execute(String url) {
        if (!futureCallsAllowed) {
            throw new AmazonAxessoQuotaMonitor.QuotaException("Blocking call since futureCallsAllowed is false.");
        }

        HttpUriRequest getRequest = RequestBuilder.get()
            .setUri(url)
            .setHeader("X-RapidAPI-Key", API_KEY)
            .setHeader("X-RapidAPI-Host", API_HOST)
            .build();

        CloseableHttpResponse response;
        String responseString;
        try {
            response = client.execute(getRequest);
            futureCallsAllowed = quotaMonitor.incrementUsage(response);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);

            // For testing: load result JSON from a local file
            // Resource resource = new ClassPathResource("fake_amazon_result.json");
            // responseString = new String(Files.readString(Paths.get(resource.getFile().getPath())));
        }
        catch (Exception e) {
            log.error("Could not read fake amazon file.", e);
            return null;
        }
        log.debug("Axesso response string: " + responseString);
        JSONObject jsonObject = new JSONObject(responseString);
        return jsonObject;
    }

}
