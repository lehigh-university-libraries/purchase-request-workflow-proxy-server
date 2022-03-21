package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.net.URI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.localHoldings.dataSource", havingValue="FOLIO")
public class FolioLocalHoldingsEnrichment implements EnrichmentService {

    private static final String LOGIN_PATH = "/authn/login";
    private static final String INSTANCES_PATH = "/inventory/instances";

    private static final String TENANT_HEADER = "x-okapi-tenant";
    private static final String TOKEN_HEADER = "x-okapi-token";

    private final WorkflowService workflowService;
    private final Config config;
    private CloseableHttpClient client;
    private String token;

    private enum IdentifierType { 
        ISBN, OclcNumber; 
    }

    FolioLocalHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.config = config;
        this.workflowService = workflowService;
        initConnection();
        initToken();

        manager.addListener(this);
        log.debug("LocalHoldingsEnrichment listening.");
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
        String url = config.getFolio().getOkapiBaseUrl() + LOGIN_PATH;
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

        log.debug("got auth response from folio with response code: " + responseCode);

        if (responseCode > 399) {
            throw new Exception(responseString);
        }
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String message = StringUtils.EMPTY;
        message += findMatchesOnIdentifier(purchaseRequest, purchaseRequest.getIsbn(), IdentifierType.ISBN, null);
        message += findMatchesOnIdentifier(purchaseRequest, purchaseRequest.getOclcNumber(), IdentifierType.OclcNumber, 
            purchaseRequest.getPrefixedOclcNumber());
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    private String findMatchesOnIdentifier(PurchaseRequest purchaseRequest, String identifier, 
        IdentifierType identifierType, String identifierForWebsiteUrl) {
        
        if (identifier == null) {
            log.debug("Cannot enrich with local holdings, no " + identifierType);
            return StringUtils.EMPTY;
        }

        log.debug("Enriching local holdings by " + identifierType + " request for " + purchaseRequest);

        String url = config.getFolio().getOkapiBaseUrl() + INSTANCES_PATH;
        String queryString = "(identifiers =/@value '" + identifier + "')";
        HttpUriRequest getRequest = RequestBuilder.get()
            .setUri(url)
            .setHeader(TENANT_HEADER, config.getFolio().getTenantId())
            .setHeader(TOKEN_HEADER, token)
            .addParameter("query", queryString)
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
            return StringUtils.EMPTY;
        }
        log.debug("Got response with code " + response.getStatusLine() + " and entity " + response.getEntity());

        JsonObject responseObject = JsonParser.parseString(responseString).getAsJsonObject();
        long totalRecords = responseObject.get("totalRecords").getAsLong();

        String message;
        if (totalRecords > 0) {
            String recordsUrl;
            if (Config.LocalHoldings.LinkDestination.VuFind == (config.getLocalHoldings().getLinkTo()) &&
                // VuFind may only support ISBN search
                IdentifierType.ISBN == identifierType) {
                recordsUrl = config.getVuFind().getBaseUrl()
                    + "/Search/Results?lookfor=" + identifier + "&type=AllFields&limit=20";
            }
            else {
                recordsUrl = config.getFolio().getWebsiteBaseUrl() 
                    + "/inventory?qindex=identifier&query=" 
                    + (identifierForWebsiteUrl != null ? identifierForWebsiteUrl : identifier) 
                    + "&sort=title";
            }
            String recordsLink = "<a href=\"" + recordsUrl.toString() + "\">" + totalRecords + " instances</a>";
            message = "Local holdings: Lehigh (FOLIO) has " + recordsLink + " instances matching this " + identifierType + ".\n";
        }
        else {
            message = "NO Local holdings: Lehigh (FOLIO) has no instances matching this " + identifierType + ".\n";
        }
        return message;
    }

}

