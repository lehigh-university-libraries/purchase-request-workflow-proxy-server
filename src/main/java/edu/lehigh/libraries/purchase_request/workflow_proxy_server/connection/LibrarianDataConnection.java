package edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LibrarianDataConnection {

    private CloseableHttpClient client;    

    private final String BASE_URL;

    public LibrarianDataConnection(Config config) {
        BASE_URL = config.getLibrarian().getBaseUrl();

        initConnection();
    }

    private void initConnection() {
        client = HttpClientBuilder.create()
            .build();
    }

    public JSONArray executeGetForArray(String url) {
        String responseString;
        try {
            responseString = executeGet(url);
        }
        catch (Exception e) {
            log.error("Could not get data from Librarian Call Numbers.", e);
            return null;
        }
        JSONArray jsonArray = new JSONArray(responseString);
        return jsonArray;
    }

    private String executeGet(String url) throws Exception {
        HttpUriRequest getRequest = RequestBuilder.get()
            .setUri(BASE_URL + url)
            .build();

        CloseableHttpResponse response = client.execute(getRequest);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

}
