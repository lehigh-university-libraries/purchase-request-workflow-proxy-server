package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.localHoldings.dataSource", havingValue="OCLC")
public class OclcLocalHoldingsEnrichment extends LocalHoldingsEnrichment {

    private static final String SCOPE = "wcapi";

    private final WorkflowService workflowService;
    private final OclcConnection oclcConnection;

    private final String LOCAL_OCLC_SYMBOL;

    OclcLocalHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        super(config);
        this.workflowService = workflowService;
        this.oclcConnection = new OclcConnection(config, SCOPE);

        LOCAL_OCLC_SYMBOL = config.getOclc().getLocalInstitutionSymbol();

        manager.addListener(this);
        log.debug("OclcLocalHoldingsEnrichment listening.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String isbn = purchaseRequest.getIsbn();
        if (isbn == null) {
            log.debug("No ISBN, skipping OclcLocalHoldingsEnrichment.");
        }

        String url = OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&isbn=" + isbn + "&heldBySymbol=" + LOCAL_OCLC_SYMBOL;
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

        String message = buildEnrichmentMessage(totalHoldingCount, isbn, IdentifierType.ISBN, null);
        workflowService.enrich(purchaseRequest, EnrichmentType.LOCAL_HOLDINGS, message);
        log.debug("Done creating enrichment for " + purchaseRequest);
    }

}
