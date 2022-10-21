package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.isbn_db;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
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
public class IsbnDbConnection {

    private final Config config;

    private String API_KEY;
    private CloseableHttpClient client;    

    public IsbnDbConnection(Config config) {
        this.config = config;
        initConnection();
    }

    private void initConnection() {
        API_KEY = config.getIsbnDb().getApiKey();
        client = HttpClientBuilder.create()
            .build();
    }

    public JSONObject execute(String url) {
        HttpUriRequest getRequest = RequestBuilder.get()
        .setUri(url)
        .setHeader(HttpHeaders.AUTHORIZATION, API_KEY)
        .build();
    
        CloseableHttpResponse response;
        String responseString;
        try {
            response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
        }
        catch (Exception e) {
            log.error("Could not get data from IsbnDb.", e);
            return null;
        }
        JSONObject jsonObject = new JSONObject(responseString);
        return jsonObject;
    }

    public String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

}
