package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
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

    void enrichByTitleAndContributorWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol,
        boolean isGroupSymbol) {

        log.debug("Enriching by title and contributor.");
        String title = purchaseRequest.getTitle();
        String contributor = purchaseRequest.getContributor();
        String url = OclcConnection.WORLDCAT_BASE_URL 
            + "/brief-bibs"
            + "?" + (isGroupSymbol ? "heldByGroup" : "heldBySymbol") + "=" + oclcSymbol
            + "&q=("
            + "ti:" + ConnectionUtil.encodeUrl(title)
            + ConnectionUtil.encodeUrl(" AND ")
            + "au:" + ConnectionUtil.encodeUrl(contributor) + ""
            + ")";

        long totalHoldingCount;
        try {
            totalHoldingCount = getNumberOfRecords(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }

        String message = buildEnrichmentMessage(totalHoldingCount, title, contributor, 
            IdentifierType.TitleAndContributor, null, oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    void enrichByIsbnWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol) {
        log.debug("Enriching by ISBN.");
        String isbn = purchaseRequest.getIsbn();
        String url = OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&isbn=" + isbn + "&heldByGroup=" + oclcSymbol;

        long totalHoldingCount;
        try {
            totalHoldingCount = getTotalHoldingCount(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }

        String message = buildEnrichmentMessage(totalHoldingCount, isbn, null, IdentifierType.ISBN, null,
            oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    void enrichByOclcNumberWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol) {
        log.debug("Enriching by OCLC number.");
        String oclcNumber = purchaseRequest.getOclcNumber();
        String url = OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&oclcNumber=" + oclcNumber + "&heldBySymbol=" + oclcSymbol;

        long totalHoldingCount;
        try {
            totalHoldingCount = getTotalHoldingCount(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }

        String message = buildEnrichmentMessage(totalHoldingCount, oclcNumber, null, IdentifierType.OclcNumber, null,
            oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

    long getNumberOfRecords(String url) throws Exception {
        JsonObject responseObject = oclcConnection.execute(url);
        long numberOfRecords = responseObject.get("numberOfRecords").getAsLong();
        return numberOfRecords;
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
