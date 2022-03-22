package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.OclcConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.groupHoldings.oclcSymbols")
public class OclcGroupHoldingsEnrichment extends OclcHoldingsEnrichment {
    
    private final List<String> GROUP_OCLC_SYMBOLS;

    OclcGroupHoldingsEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) 
        throws Exception {
        
        super(workflowService, config);

        GROUP_OCLC_SYMBOLS = config.getGroupHoldings().getOclcSymbols();

        manager.addListener(this);
        log.debug("OclcGroupHoldingsEnrichment listening.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getIsbn() == null) {
            log.debug("No ISBN, skipping OclcGroupLocalHoldingsEnrichment.");
            return;
        }

        for (String groupOclcSymbol: GROUP_OCLC_SYMBOLS) {
            enrichWithSymbol(purchaseRequest, groupOclcSymbol);
        }
    }

    @Override
    String getQueryUrl(String isbn, String oclcSymbol) {
        return OclcConnection.WORLDCAT_BASE_URL
            + "/bibs-holdings?holdingsAllEditions=true&isbn=" + isbn + "&heldByGroup=" + oclcSymbol;
    }

    @Override
    String buildEnrichmentMessage(long totalRecords, String identifier, IdentifierType identifierType,
        String identifierForWebsiteUrl, String holdingsType) {

        String message;
        if (totalRecords > 0) {
            String recordsUrl = buildRecordsUrl(identifier, identifierType, identifierForWebsiteUrl);
            String recordsLink = "<a href=\"" + recordsUrl.toString() + "\">" + totalRecords + " instances</a>";
            message = holdingsType + " holdings: " + recordsLink + " instances matching this " + identifierType + ".\n";
        }
        else {
            message = "NO " + holdingsType + " holdings: No instances matching this " + identifierType + ".\n";
        }
        return message;
    }

}
