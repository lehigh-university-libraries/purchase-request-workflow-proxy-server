package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.identifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.identifiers", havingValue="OCLC")
@ConditionalOnWebApplication
public class OclcIdentifiersEnrichment implements EnrichmentService {

    private static final String SCOPE = "wcapi";

    private final String CLASSIFICATION_TYPE;

    private final WorkflowService workflowService;
    private final OclcConnection oclcConnection;

    OclcIdentifiersEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.oclcConnection = new OclcConnection(config, SCOPE);
        this.workflowService = workflowService;

        CLASSIFICATION_TYPE = config.getOclc().getClassificationType();

        manager.addListener(this, 1);
        log.debug("OclcIdentifiersEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getOclcNumber() != null) {
            log.debug("Skipping OCLC enrichment, item already has an OCLC number.");
            return;
        }

        if (purchaseRequest.getIsbn() != null) {
            enrichByIsbn(purchaseRequest);
        }
        else if (purchaseRequest.getTitle() != null && purchaseRequest.getContributor() != null) {
            enrichByTitleAndContributor(purchaseRequest);
        }
        else {
            log.debug("Skipping OCLC enrichment since ISBN is null and title & contributor are not both supplied.");
            return;
        }
    }

    private void enrichByTitleAndContributor(PurchaseRequest purchaseRequest) {
        log.debug("Enriching by title and contributor.");
        String url = OclcConnection.WORLDCAT_BASE_URL + "/bibs?"
            + "heldBySymbol=DLC"
            + "&q=("
            + "ti:" + ConnectionUtil.encodeUrl(purchaseRequest.getTitle())
            + ConnectionUtil.encodeUrl(" AND au:\"" + purchaseRequest.getContributor() + "\"")
            + ")";
        
        enrichWithFirstResult(purchaseRequest, url);            
    }

    private void enrichByIsbn(PurchaseRequest purchaseRequest) {
        log.debug("Enriching by ISBN.");
        String isbn = purchaseRequest.getIsbn();

        // TODO Change logic to use MatchMARC or something else to choose the right result
        String url = OclcConnection.WORLDCAT_BASE_URL + "/bibs?q=(bn:" + isbn + ")";

        enrichWithFirstResult(purchaseRequest, url);
    }

    private void enrichWithFirstResult(PurchaseRequest purchaseRequest, String url) {
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

        enrichOclcNumber(purchaseRequest, bibRecord);
        enrichCallNumber(purchaseRequest, bibRecord);
    }

    private void enrichOclcNumber(PurchaseRequest purchaseRequest, JsonObject bibRecord) {
        JsonObject identifier = bibRecord.getAsJsonObject("identifier");
        if (identifier == null) {
            log.debug("No identifier found, cannot enrich OCLC number.");
            return;
        }
        String oclcNumber = identifier.get("oclcNumber").getAsString();
        log.debug("Found OCLC number for item: " + oclcNumber);
        workflowService.enrich(purchaseRequest, EnrichmentType.OCLC_NUMBER, oclcNumber);
    }

    private void enrichCallNumber(PurchaseRequest purchaseRequest, JsonObject bibRecord) {
        JsonObject classification = bibRecord.getAsJsonObject("classification");
        if (classification == null) {
            log.debug("No classification found, cannot enrich call number.");
            return;
        }
        String callNumber = classification.get(CLASSIFICATION_TYPE).getAsString();
        log.debug("found call number for item: " + callNumber);
        workflowService.enrich(purchaseRequest, EnrichmentType.CALL_NUMBER, callNumber);
    }

}