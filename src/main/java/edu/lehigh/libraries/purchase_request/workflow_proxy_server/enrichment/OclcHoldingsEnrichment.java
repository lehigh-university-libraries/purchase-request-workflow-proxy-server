package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
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

    abstract String getQueryUrl(String isbn, String oclcSymbol);

    void enrichWithSymbol(PurchaseRequest purchaseRequest, String oclcSymbol) {
        String isbn = purchaseRequest.getIsbn();
        String url = getQueryUrl(isbn, oclcSymbol);
        JsonObject responseObject;
        try {
            responseObject = oclcConnection.execute(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting local holdings from OCLC: ", e);
            return;
        }
    
        JsonArray briefRecords = responseObject.getAsJsonArray("briefRecords");
        JsonObject briefRecord = (JsonObject)briefRecords.get(0);
        JsonObject institutionHolding = briefRecord.getAsJsonObject("institutionHolding");
        long totalHoldingCount = institutionHolding.get("totalHoldingCount").getAsLong();

        String message = buildEnrichmentMessage(totalHoldingCount, isbn, IdentifierType.ISBN, null,
            oclcSymbol);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

}
