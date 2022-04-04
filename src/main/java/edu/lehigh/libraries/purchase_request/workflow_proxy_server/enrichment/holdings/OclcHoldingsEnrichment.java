package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class OclcHoldingsEnrichment extends HoldingsEnrichment {

    private static final String SCOPE = "wcapi";

    final OclcConnection oclcConnection;
    final WorkflowService workflowService;

    OclcHoldingsEnrichment(WorkflowService workflowService, Config config) throws Exception {
        super(config);
        this.workflowService = workflowService;
        this.oclcConnection = new OclcConnection(config, SCOPE);
    }

    String getQueryUrlForIsbn(String isbn, String oclcSymbol) {
        return OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&isbn=" + isbn + "&heldByGroup=" + oclcSymbol;
    }

    String getQueryUrlForOclcNumber(String oclcNumber, String oclcSymbol) {
        return OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&oclcNumber=" + oclcNumber + "&heldBySymbol=" + oclcSymbol;
    }

    void enrichByIsbnWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol) {
        String isbn = purchaseRequest.getIsbn();
        String url = getQueryUrlForIsbn(isbn, oclcSymbol);

        long totalHoldingCount;
        try {
            totalHoldingCount = getTotalHoldingCount(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }

        String message = buildEnrichmentMessage(totalHoldingCount, isbn, IdentifierType.ISBN, null,
            oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    void enrichByOclcNumberWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol) {
        String oclcNumber = purchaseRequest.getOclcNumber();
        String url = getQueryUrlForOclcNumber(oclcNumber, oclcSymbol);

        long totalHoldingCount;
        try {
            totalHoldingCount = getTotalHoldingCount(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }

        String message = buildEnrichmentMessage(totalHoldingCount, oclcNumber, IdentifierType.OclcNumber, null,
            oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    long getTotalHoldingCount(String url) throws Exception {
        JsonObject responseObject = oclcConnection.execute(url);
    
        JsonArray briefRecords = responseObject.getAsJsonArray("briefRecords");
        JsonObject briefRecord = (JsonObject)briefRecords.get(0);
        JsonObject institutionHolding = briefRecord.getAsJsonObject("institutionHolding");
        long totalHoldingCount = institutionHolding.get("totalHoldingCount").getAsLong();
        return totalHoldingCount;
    }

}
