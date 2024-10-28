package edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JiraConnection {

    // Both V2 and V3 are supported and functionally identical.  V3 uses Atlassian Document Format.  V2 allows
    // wiki document formatting for where that is easier.
    private static final String API_PREFIX_V2 = "rest/api/2/";
    private static final String API_PREFIX_V3 = "rest/api/3/";

    private final Config config;

    private CloseableHttpClient client;

    public JiraConnection(Config config) {
        this.config = config;

        initConnection();

        log.debug("Jira service ready");
    }

    private Credentials credentials;

    private void initConnection() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        credentials = new UsernamePasswordCredentials(config.getJira().getUsername(), config.getJira().getToken());
        provider.setCredentials(AuthScope.ANY, credentials);
        int timeout = 5000; // milliseconds
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
        client = HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .setDefaultRequestConfig(requestConfig)
            .build();                
    }

    public JsonObject executeGet(String url) throws Exception {
        return executeGet(url, null);
    }

    public JsonObject executeGet(String url, Map<String, String> extraParameters) throws Exception {
        RequestBuilder requestBuilder = RequestBuilder.get()
            .setUri(config.getJira().getUrl() + API_PREFIX_V3 + url);
        if (extraParameters != null) {
            for (Map.Entry<String, String> entry : extraParameters.entrySet()) {
                requestBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }
        HttpUriRequest getRequest = requestBuilder.build();
        getRequest.addHeader(new BasicScheme().authenticate(credentials, getRequest, null));

        log.debug(getRequest.toString());
        try (CloseableHttpResponse response = client.execute(getRequest)) {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode > 399) {
                throw new Exception(response.getStatusLine().getReasonPhrase());
            }

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            log.debug("Got response with code " + response.getStatusLine() + " and entity " + response.getEntity());

            JsonObject responseObject = JsonParser.parseString(responseString).getAsJsonObject();
            return responseObject;
        }
    }

    public JsonObject executePost(String url, JsonObject body) throws Exception {
        return executeMutation(HttpPost.METHOD_NAME, url, body, 2);
    }

    public JsonObject executePost(String url, JsonObject body, int apiVersion) throws Exception {
        return executeMutation(HttpPost.METHOD_NAME, url, body, apiVersion);
    }

    public JsonObject executePut(String url, JsonObject body) throws Exception {
        return executeMutation(HttpPut.METHOD_NAME, url, body, 2);
    }

    private JsonObject executeMutation(String methodName, String url, JsonObject body, int apiVersion) throws Exception {
        HttpEntityEnclosingRequestBase mutation;
        String apiPrefix = (apiVersion == 2) ? API_PREFIX_V2 : API_PREFIX_V3;
        if (HttpPost.METHOD_NAME.equals(methodName)) {
            mutation = new HttpPost(config.getJira().getUrl() + apiPrefix + url);
        }
        else if (HttpPut.METHOD_NAME.equals(methodName)) {
            mutation = new HttpPut(config.getJira().getUrl() + apiPrefix + url);
        }
        else {
            throw new IllegalStateException("unhandled method: " + methodName);
        }
        mutation.addHeader(new BasicScheme().authenticate(credentials, mutation, null));
        mutation.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        log.debug(methodName + " to URL " + url + "; entity: " + body.toString());
        mutation.setEntity(new StringEntity(body.toString(), "UTF-8"));
        try (CloseableHttpResponse response = client.execute(mutation)) {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode > 399) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseString = EntityUtils.toString(entity);
                    throw new Exception(responseString);
                }
                else {
                    throw new Exception(response.getStatusLine().getReasonPhrase());
                }
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                log.debug("Got response with code " + response.getStatusLine() + " and entity " + response.getEntity());
        
                JsonObject responseObject = JsonParser.parseString(responseString).getAsJsonObject();
                return responseObject;
            }
            else {
                log.debug("Got response with code " + response.getStatusLine() + " and no entity");
                return null;
            }
        }
    }

}
