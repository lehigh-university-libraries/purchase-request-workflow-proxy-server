package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.localHoldings.dataSource", havingValue="OCLC")
@ConditionalOnWebApplication
public class OclcLocalHoldingsEnrichment extends OclcHoldingsEnrichment {

    private final String LOCAL_OCLC_SYMBOL;

    OclcLocalHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) throws Exception {
        super(workflowService, config);

        LOCAL_OCLC_SYMBOL = config.getOclc().getLocalInstitutionSymbol();

        manager.addListener(this);
        log.debug("OclcLocalHoldingsEnrichment listening.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        log.debug("Enriching OCLC Local Holdings");
        if (purchaseRequest.getTitle() != null && purchaseRequest.getContributor() != null) {
            enrichByTitleAndContributorWithSymbol(purchaseRequest, LOCAL_OCLC_SYMBOL, false);
        }
        else if (purchaseRequest.getIsbn() != null) {
            enrichByIsbnWithSymbol(purchaseRequest, LOCAL_OCLC_SYMBOL);
        }
        else if (purchaseRequest.getOclcNumber() != null) {
            enrichByOclcNumberWithSymbol(purchaseRequest, LOCAL_OCLC_SYMBOL);
        }
        else {
            log.debug("No ISBN or OCLC number, skipping OclcLocalHoldingsEnrichment.");
            return;
        }
    }

    @Override
    String buildEnrichmentMessage(long totalRecords, String identifier, String secondaryIdentifier,
        IdentifierType identifierType, String identifierForWebsiteUrl, String holdingsType) {

        String message;
        if (totalRecords > 0) {
            String recordsUrl = buildRecordsUrl(identifier, secondaryIdentifier, identifierType, identifierForWebsiteUrl);
            String recordsLink = "[" + totalRecords + " instances|" + recordsUrl + "]";
            message = "Local holdings found in OCLC: " + recordsLink + " loosely related to this " + identifierType + ".\n";
        }
        else {
            message = "NO Local holdings found in OCLC: No instances loosely related to this " + identifierType + ".\n";
        }
        return message;
    }

}
