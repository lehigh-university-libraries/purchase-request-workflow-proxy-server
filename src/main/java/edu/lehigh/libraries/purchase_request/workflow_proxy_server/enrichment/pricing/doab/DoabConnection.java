package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.doab;

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
        log.debug("Executing query: " + url);
        HttpUriRequest getRequest = RequestBuilder.get(url).build();
        CloseableHttpResponse response;
        JSONArray jsonArray;
        try {
            response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            log.debug("Response string: " + responseString);
            jsonArray = new JSONArray(responseString);
        }
        catch (Exception e) {
            log.error("Could not query DOAB.", e);
            return null;
        }
        return jsonArray;
    }

}
