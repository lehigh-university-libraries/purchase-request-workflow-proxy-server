package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WorldCatEnrichment implements EnrichmentService {

    private static final String SCOPE = "wcapi";

    private final WorkflowService workflowService;
    private final Config config;
    private OAuth2AccessToken token;
    private OAuth20Service oclcService;

    WorldCatEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.config = config;
        this.workflowService = workflowService;

        initConnection();

        manager.addListener(this, 1);
        log.debug("WorldCatEnrichment ready");
    }

    private void initConnection() {
        String clientId = config.getOclc().getWsKey();
        String clientSecret = config.getOclc().getSecret();
        oclcService = new ServiceBuilder(clientId)
            .apiSecret(clientSecret)
            .defaultScope(SCOPE)
            .build(OclcApi.instance());
        try {
            token = oclcService.getAccessTokenClientCredentialsGrant();
        }
        catch (Exception e) {
            log.error("Error connecting to OCLC: ", e);
            return;
        }
        log.debug("Connected to OCLC");
        log.debug("Response was: " + token.getRawResponse());
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getOclcNumber() != null) {
            log.debug("Skipping OCLC enrichment, item already has an OCLC number.");
            return;
        }

        String isbn = purchaseRequest.getIsbn();
        if (isbn == null) {
            // TODO Change to enrich if both title and contributor are present and there's exactly one query result.
            log.debug("Skipping OCLC enrichment since ISBN is null.");
            return;
        }

        // TODO Change logic to use MatchMARC or something else to choose the right result
        String url = "https://americas.discovery.api.oclc.org/worldcat/search/v2";
        url += "/bibs?heldBy=DLC&q=(bn:" + isbn + ")";

        OAuthRequest request = new OAuthRequest(Verb.GET, url);
        request.addHeader("Accept", "application/json");
        oclcService.signRequest(token, request);
        Response response;
        String responseBody;
        try {
            response = oclcService.execute(request);
            log.debug("got bib response from oclc:" + response);
            responseBody = response.getBody();
            }
        catch (Exception e) {
            log.error("Caught exception getting bib data from OCLC: ", e);
            return;
        }

        JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
        long totalRecords = responseObject.get("numberOfRecords").getAsLong();
        if (totalRecords == 0) {
            log.debug("No WorldCat records found, cannot enrich.");
            return;
        }
        JsonArray bibRecords = responseObject.getAsJsonArray("bibRecords");
        JsonObject bibRecord = (JsonObject)bibRecords.get(0);
        JsonObject identifier = bibRecord.getAsJsonObject("identifier");
        if (identifier == null) {
            log.debug("No identifier found, cannot enrich.");
            return;
        }
        String oclcNumber = identifier.get("oclcNumber").getAsString();
        log.debug("Found OCLC number for item: " + oclcNumber);

        workflowService.enrich(purchaseRequest, EnrichmentType.OCLC_NUMBER, oclcNumber);
    }

    private static class OclcApi extends DefaultApi20 {

        private static final String TOKEN_URL = "https://oauth.oclc.org/token";

        private static final OclcApi INSTANCE = new OclcApi();
        public static OclcApi instance() {
            return INSTANCE;
        }

        @Override
        public String getAccessTokenEndpoint() {
            return TOKEN_URL;
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            throw new UnsupportedOperationException("No BaseURL needed for Client Credentials API.");
        }

    }

}
