package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.restyaboard;

import java.net.URI;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestyaboardConnection {
    
    private static final String API_PATH = "/api/v1";
    private static final String LOGIN_PATH = "/users/login.json";

    private final Config config;

    private CloseableHttpClient client;
    private String token;

    public RestyaboardConnection(Config config) throws Exception {
        this.config = config;

        initConnection();
        initToken();

        log.debug("Restyaboard connection ready");
    }

    private void initConnection() {
        client = HttpClientBuilder.create()
            .build();                
    }

    private void initToken() throws Exception {
        String url = config.getRestyaboard().getBaseUrl() + API_PATH + LOGIN_PATH;
        URI uri = new URIBuilder(url).build();

        JSONObject postData = new JSONObject();
        postData.put("email", config.getRestyaboard().getUsername());
        postData.put("password", config.getRestyaboard().getPassword());

        HttpUriRequest post = RequestBuilder.post()
            .setUri(uri)
            .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
            .setEntity(new StringEntity(postData.toString()))
            .build();
        CloseableHttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        JSONObject jsonObject = new JSONObject(responseString);

        int responseCode = response.getStatusLine().getStatusCode();
        log.debug("got auth response from restyaboard with response code: " + responseCode);
        if (responseCode > 399) {
            throw new Exception(responseString);
        }

        token = jsonObject.getString("access_token");
    }

    public JSONObject executePost(String url, JSONObject body) throws Exception {
        HttpUriRequest postRequest = RequestBuilder.post()
            .setUri(config.getRestyaboard().getBaseUrl() + API_PATH + url)
            .addParameter("token", token)
            .setEntity(new StringEntity(body.toString()))
            .build();
        CloseableHttpResponse response = client.execute(postRequest);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode > 399) {
            throw new Exception(responseString);
        }
        return new JSONObject(responseString);
    }

    public JSONObject executeGet(String url) throws Exception {
        return executeGet(url, null);
    }

    public JSONObject executeGet(String url, Map<String, String> requestParams) throws Exception {
        String responseString = executeGetRawResponse(url, requestParams);
        JSONObject jsonObject = new JSONObject(responseString);
        return jsonObject;
    }

    public JSONArray executeGetForArray(String url, Map<String, String> requestParams) throws Exception {
        String responseString = executeGetRawResponse(url, requestParams);
        JSONArray jsonArray = new JSONArray(responseString);
        return jsonArray;
    }

    private String executeGetRawResponse(String url, Map<String, String> requestParams) throws Exception {
        RequestBuilder builder = RequestBuilder.get()
            .setUri(config.getRestyaboard().getBaseUrl() + API_PATH + url)
            .addParameter("token", token);
        if (requestParams != null) {
            requestParams.forEach((key, value) -> builder.addParameter(key, value));
        }
        HttpUriRequest getRequest = builder.build();

        CloseableHttpResponse response;
        response = client.execute(getRequest);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        log.debug("Got response with code " + response.getStatusLine() + " and entity " + response.getEntity());
        return responseString;
    }

}
