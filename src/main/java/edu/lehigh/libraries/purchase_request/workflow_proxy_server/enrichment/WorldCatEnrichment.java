package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WorldCatEnrichment implements EnrichmentService {

    private static final String SCOPE = "wcapi";

    private final WorkflowService workflowService;
    private final OclcConnection oclcConnection;

    WorldCatEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.oclcConnection = new OclcConnection(config, SCOPE);
        this.workflowService = workflowService;

        manager.addListener(this, 1);
        log.debug("WorldCatEnrichment ready");
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
        String url = OclcConnection.WORLDCAT_BASE_URL + "/bibs?q=(bn:" + isbn + ")";

        JsonObject responseObject;
        try {
            responseObject = oclcConnection.execute(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting bib data from OCLC: ", e);
            return;
        }

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

}
