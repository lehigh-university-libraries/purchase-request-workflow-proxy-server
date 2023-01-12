package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.oasis;

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
public class OasisConnection {
    
    public static final int STATUS_FOUND = 0;
    public static final int STATUS_NOT_FOUND = 92;

    private static final String BASE_URL = "https://oasis-services.proquest.com/api/v1";

    private final Config config;

    private CloseableHttpClient client;
    private String API_KEY_PARAM; 

    OasisConnection(Config config) {
        this.config = config;
        initConnection();
    }

    private void initConnection() {
        String apiKey = config.getOasis().getApiKey();
        API_KEY_PARAM = "&apiKey=" + apiKey;

        client = HttpClientBuilder.create().build();
    }

    public JSONObject query(String path) {
        String url = BASE_URL + path + API_KEY_PARAM;
        HttpUriRequest getRequest = RequestBuilder.get()
        .setUri(url)
        .build();
    
        CloseableHttpResponse response;
        String responseString;
        try {
            response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
        }
        catch (Exception e) {
            log.error("Could not get data from Oasis.", e);
            return null;
        }
        JSONObject jsonObject = new JSONObject(responseString);
        return jsonObject;
    }

}
