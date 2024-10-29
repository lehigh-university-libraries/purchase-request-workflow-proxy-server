package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.identifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
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
    private final String TITLE_ISBN_ONLY_PREFIX;

    private final WorkflowService workflowService;
    private final OclcConnection oclcConnection;

    OclcIdentifiersEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        this.oclcConnection = new OclcConnection(config, SCOPE);
        this.workflowService = workflowService;

        CLASSIFICATION_TYPE = config.getOclc().getClassificationType();
        TITLE_ISBN_ONLY_PREFIX = config.getCoreData().getTitle().getIsbnOnlyPrefix();

        manager.addListener(this, 100);
        log.debug("OclcIdentifiersEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getOclcNumber() != null && 
            purchaseRequest.getCallNumber() != null &&
            purchaseRequest.getTitle() != null && 
            !purchaseRequest.getTitle().startsWith(TITLE_ISBN_ONLY_PREFIX) &&
            purchaseRequest.getContributor() != null) {

            log.debug("Skipping OCLC enrichment, item already has an OCLC number, call number, title and contributor.");
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
        String title = truncateTitle(purchaseRequest.getTitle());
        String url = OclcConnection.WORLDCAT_BASE_URL + "/bibs?"
            + "heldBySymbol=DLC"
            + "&q=("
            + "ti:" + ConnectionUtil.encodeUrl(title)
            + ConnectionUtil.encodeUrl(" AND au:\"" + purchaseRequest.getContributor() + "\"")
            + ")";
        
        enrichWithFirstResult(purchaseRequest, url);            
    }

    private String truncateTitle(String title) {
        // The API reports an error if the total query length is greater than some unspecified amount,
        // but possibly matching the 30-word limit described here.
        // https://help.oclc.org/Discovery_and_Reference/WorldCat_Discovery/Search_in_WorldCat_Discovery/010Search_and_use_query_syntax
        // In case of long titles, limit the title to 20 words to be safe.
        return title.replaceAll("^((?:\\W*\\w+){20}).*$", "$1");
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
        enrichTitle(purchaseRequest, bibRecord);
        enrichContributor(purchaseRequest, bibRecord);
    }

    private void enrichOclcNumber(PurchaseRequest purchaseRequest, JsonObject bibRecord) {
        if (purchaseRequest.getOclcNumber() != null) {
            log.debug("OCLC number already present, skipping enrichment.");
            return;
        }

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
        if (purchaseRequest.getCallNumber() != null) {
            log.debug("Call number already present, skipping enrichment.");
            return;
        }

        JsonObject classification = bibRecord.getAsJsonObject("classification");
        if (classification == null) {
            log.debug("No classification found, cannot enrich call number.");
            return;
        }
        JsonElement callNumberElement = classification.get(CLASSIFICATION_TYPE);
        if (callNumberElement == null) {
            log.debug("No classification found of type " + CLASSIFICATION_TYPE);
            return;
        }
        String callNumber = callNumberElement.getAsString();
        log.debug("found call number for item: " + callNumber);
        workflowService.enrich(purchaseRequest, EnrichmentType.CALL_NUMBER, callNumber);
    }

    private void enrichTitle(PurchaseRequest purchaseRequest, JsonObject bibRecord) {
        if (purchaseRequest.getTitle() != null && 
            !purchaseRequest.getTitle().startsWith(TITLE_ISBN_ONLY_PREFIX)) {
                
            log.debug("Real title already present, skipping enrichment: " + purchaseRequest.getTitle());
            return;
        }

        JsonObject title = bibRecord.getAsJsonObject("title");
        if (title == null) {
            log.debug("No title found, cannot enrich title.");
            return;
        }

        JsonArray mainTitles = title.getAsJsonArray("mainTitles");
        if (mainTitles == null || mainTitles.size() < 1) {
            log.debug("No mainTitles found, cannot enrich title.");
            return;
        }

        JsonObject mainTitle = mainTitles.get(0).getAsJsonObject();
        String titleText = mainTitle.get("text").getAsString();
        if (titleText == null) {
            log.debug("No title text found, cannot enrich title.");
            return;
        }
        log.debug("found title for item: " + titleText);
        titleText = PurchaseRequest.normalizeTitle(titleText);
        log.debug("normalized title: " + titleText);
        workflowService.enrich(purchaseRequest, EnrichmentType.TITLE, titleText);
    }

    private void enrichContributor(PurchaseRequest purchaseRequest, JsonObject bibRecord) {
        if (purchaseRequest.getContributor() != null) {
            log.debug("Contributor already present, skipping enrichment: " + purchaseRequest.getContributor());
            return;
        }

        JsonObject contributorNode = bibRecord.getAsJsonObject("contributor");
        if (contributorNode == null) {
            log.debug("No contributorNode found, cannot enrich contributor.");
            return;
        }

        JsonArray creators = contributorNode.getAsJsonArray("creators");
        if (creators == null || creators.size() < 1) {
            log.debug("No creators found, cannot enrich contributor.");
            return;
        }

        JsonObject creator = creators.get(0).getAsJsonObject();
        JsonObject firstName = creator.getAsJsonObject("firstName");
        if (firstName == null) {
            log.debug("No firstName found, cannot enrich contributor.");
            return;
        }
        JsonObject secondName = creator.getAsJsonObject("secondName");
        if (secondName == null) {
            log.debug("No secondName found, cannot enrich contributor.");
            return;
        }
        String contributor = firstName.get("text").getAsString() +
            " " +
            secondName.get("text").getAsString();
        log.debug("found contributor for item: " + contributor);
        workflowService.enrich(purchaseRequest, EnrichmentType.CONTRIBUTOR, contributor);
    }

}