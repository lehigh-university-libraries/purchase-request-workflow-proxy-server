package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

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
        if (purchaseRequest.getIsbn() == null) {
            log.debug("No ISBN, skipping OclcLocalHoldingsEnrichment.");
            return;
        }
        enrichWithSymbol(purchaseRequest, LOCAL_OCLC_SYMBOL);
    }

    @Override
    String buildEnrichmentMessage(long totalRecords, String identifier, IdentifierType identifierType,
        String identifierForWebsiteUrl, String holdingsType) {

        String message;
        if (totalRecords > 0) {
            String recordsUrl = buildRecordsUrl(identifier, identifierType, identifierForWebsiteUrl);
            String recordsLink = "<a href=\"" + recordsUrl.toString() + "\">" + totalRecords + " instances</a>";
            message = "Local holdings found in OCLC: " + recordsLink + " instances loosely related to this " + identifierType + ".\n";
        }
        else {
            message = "NO Local holdings found in OCLC: No instances loosely related to this " + identifierType + ".\n";
        }
        return message;
    }

    @Override
    String getQueryUrl(String isbn, String oclcSymbol) {
        return OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&isbn=" + isbn + "&heldBySymbol=" + oclcSymbol;
    }

}
