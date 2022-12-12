package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.doab;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoabConnection {
    
    private CloseableHttpClient client;

    DoabConnection() {
        initConnection();
    }

    private void initConnection() {
        client = HttpClientBuilder.create().build();
    }

    public JSONArray executeForArray(String url) {
        HttpUriRequest getRequest = RequestBuilder.get(url).build();
        CloseableHttpResponse response;
        String responseString;
        try {
            response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
        }
        catch (Exception e) {
            log.error("Could not query DOAB.", e);
            return null;
        }

        log.debug("Response string: " + responseString);
        JSONArray jsonArray = new JSONArray(responseString);
        return jsonArray;
    }

    public String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

}
