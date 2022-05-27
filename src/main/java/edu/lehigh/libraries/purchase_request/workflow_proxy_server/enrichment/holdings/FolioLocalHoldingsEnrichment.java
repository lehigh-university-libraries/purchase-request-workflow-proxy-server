package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.FolioConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.localHoldings", havingValue="FOLIO")
@ConditionalOnWebApplication
public class FolioLocalHoldingsEnrichment extends HoldingsEnrichment {

    private static final String INSTANCES_PATH = "/inventory/instances";

    private final WorkflowService workflowService;
    private final FolioConnection connection;

    FolioLocalHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        super(config);
        this.workflowService = workflowService;
        this.connection = new FolioConnection(config);

        manager.addListener(this);
        log.debug("FolioLocalHoldingsEnrichment listening.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String message = StringUtils.EMPTY;
        if (purchaseRequest.getTitle() != null && purchaseRequest.getContributor() != null) {
            message += findMatchesOnTitleAndContributor(purchaseRequest);
        }
        else {
            message += findMatchesOnIdentifier(purchaseRequest, purchaseRequest.getIsbn(), IdentifierType.ISBN, null);
            message += findMatchesOnIdentifier(purchaseRequest, purchaseRequest.getOclcNumber(), IdentifierType.OclcNumber, 
                purchaseRequest.getPrefixedOclcNumber());    
        }
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    private String findMatchesOnTitleAndContributor(PurchaseRequest purchaseRequest) {
        log.debug("Enriching local holdings by title & contributor for " + purchaseRequest);
        String title = purchaseRequest.getTitle();
        String contributor = purchaseRequest.getContributor();

        String queryString = "("
            + "title = \"" + title + "\"" 
            + " and" 
            + " contributors all \"" + contributor + "\""
            + ")";
        JSONObject responseObject;
            try {
            responseObject = findMatches(queryString);
        }
        catch(Exception e) {
            log.error("Exception enriching request: ", e);
            return StringUtils.EMPTY;
        }
        long totalRecords = responseObject.getLong("totalRecords");

        return buildEnrichmentMessage(totalRecords, title, contributor, IdentifierType.TitleAndContributor, null, null);
    }

    private String findMatchesOnIdentifier(PurchaseRequest purchaseRequest, String identifier, 
        IdentifierType identifierType, String identifierForWebsiteUrl) {
        
        if (identifier == null) {
            log.debug("Cannot enrich with local holdings, no " + identifierType);
            return StringUtils.EMPTY;
        }

        log.debug("Enriching local holdings by " + identifierType + " request for " + purchaseRequest);
        String queryString = "(identifiers =/@value '" + identifier + "')";
        JSONObject responseObject;
        try {
            responseObject = findMatches(queryString);
        }
        catch(Exception e) {
            log.error("Exception enriching request: ", e);
            return StringUtils.EMPTY;
        }
        long totalRecords = responseObject.getLong("totalRecords");

        return buildEnrichmentMessage(totalRecords, identifier, null, identifierType, identifierForWebsiteUrl, null);
    }

    private JSONObject findMatches(String queryString) throws Exception {
        JSONObject responseObject = connection.executeGet(INSTANCES_PATH, queryString);
        return responseObject;
    }

    @Override
    String buildEnrichmentMessage(long totalRecords, String identifier, String secondaryIdentifier, 
        IdentifierType identifierType, String identifierForWebsiteUrl, String holdingsType) {

        String message;
        if (totalRecords > 0) {
            String recordsUrl = buildRecordsUrl(identifier, secondaryIdentifier, identifierType, identifierForWebsiteUrl);
            String recordsLink = "[" + totalRecords + " instances|" + recordsUrl + "]";
            message = "Local holdings found in FOLIO: " + recordsLink + " matching this " + identifierType + ".\n";
        }
        else {
            message = "NO Local holdings found in FOLIO: No instances matching this " + identifierType + ".\n";
        }
        return message;
    }

}
