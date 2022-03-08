package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.net.URI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LehighHoldingsEnrichment implements EnrichmentService {

    private static final String LOGIN_PATH = "/authn/login";
    private static final String INSTANCES_PATH = "/inventory/instances";

    private static final String TENANT_HEADER = "x-okapi-tenant";
    private static final String TOKEN_HEADER = "x-okapi-token";

    private final WorkflowService workflowService;
    private final Config config;
    private CloseableHttpClient client;
    private String token;

    LehighHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.config = config;
        this.workflowService = workflowService;
        initConnection();
        initToken();

        manager.addListener(this);
        log.debug("LehighHoldingsEnrichment listening.");
    }

    private void initConnection() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, 
            new UsernamePasswordCredentials(config.getFolio().getUsername(), config.getFolio().getPassword()));
        client = HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .build();                
    }

    private void initToken() throws Exception {
        String url = config.getFolio().getBaseUrl() + LOGIN_PATH;
        URI uri = new URIBuilder(url).build();

        JsonObject postData = new JsonObject();
        postData.addProperty("username", config.getFolio().getUsername());
        postData.addProperty("password", config.getFolio().getPassword());
        postData.addProperty("tenant", config.getFolio().getTenantId());

        HttpUriRequest post = RequestBuilder.post()
            .setUri(uri)
            .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
            .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()).setVersion(HttpVersion.HTTP_1_1)
            .setHeader(TENANT_HEADER, config.getFolio().getTenantId())
            .setEntity(new StringEntity(postData.toString()))
            .build();
        CloseableHttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        int responseCode = response.getStatusLine().getStatusCode();
        token = response.getFirstHeader(TOKEN_HEADER).getValue();

        log.debug("got auth response from folio with response code: " + responseCode + 
            " and body: " + responseString);

        if (responseCode > 399) {
            throw new Exception(responseString);
        }
    }

    @Override
    @Async
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        log.debug("Enriching request for " + purchaseRequest);

        String url = config.getFolio().getBaseUrl() + INSTANCES_PATH;
        String isbn = purchaseRequest.getIsbn();
        HttpUriRequest getRequest = RequestBuilder.get()
            .setUri(url)
            .setHeader(TENANT_HEADER, config.getFolio().getTenantId())
            .setHeader(TOKEN_HEADER, token)
            .addParameter("query", "(identifiers =/@value '" + isbn + "')")
            .build();

        CloseableHttpResponse response;
        String responseString;
        try {
            response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
            }
        catch(Exception e) {
            log.error("Exception enriching request: ", e);
            return;
        }
        log.debug("Got response with code " + response.getStatusLine() + " and entity " + response.getEntity());

        JsonObject responseObject = JsonParser.parseString(responseString).getAsJsonObject();
        long totalRecords = responseObject.get("totalRecords").getAsLong();

        String message;
        if (totalRecords > 0) {
            message = "Local holdings: Lehigh (FOLIO) has " + totalRecords + " instances matching this ISBN.";
        }
        else {
            message = "NO Local holdings: Lehigh (FOLIO) has no instances matching this ISBN.";
        }
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

}

