package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import java.util.List;

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
@ConditionalOnProperty(name="workflow.groupHoldings.dataSource", havingValue="OCLC")
@ConditionalOnWebApplication
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
        log.debug("Enriching OCLC Group Holdings");
        if (purchaseRequest.getTitle() != null && purchaseRequest.getContributor() != null) {
            for (String groupOclcSymbol: GROUP_OCLC_SYMBOLS) {
                enrichByTitleAndContributorWithSymbol(purchaseRequest, groupOclcSymbol, true);
            }
        }
        else if (purchaseRequest.getIsbn() != null) {
            for (String groupOclcSymbol: GROUP_OCLC_SYMBOLS) {
                enrichByIsbnWithSymbol(purchaseRequest, groupOclcSymbol);
            }
        }
        else if (purchaseRequest.getOclcNumber() != null) {
            for (String groupOclcSymbol: GROUP_OCLC_SYMBOLS) {
                enrichByOclcNumberWithSymbol(purchaseRequest, groupOclcSymbol);
            }
        }   
        else { 
            log.debug("No ISBN or OCLC number, skipping OclcGroupLocalHoldingsEnrichment.");
            return;
        }
    }

    @Override
    String buildEnrichmentMessage(long totalRecords, String identifier, String secondaryIdentifier,
        IdentifierType identifierType, String identifierForWebsiteUrl, String holdingsType) {

        String message;
        if (totalRecords > 0) {
            message = holdingsType + " holdings: " + totalRecords + " instances matching this " + identifierType + ".\n";
        }
        else {
            message = "NO " + holdingsType + " holdings: No instances matching this " + identifierType + ".\n";
        }
        return message;
    }

}
