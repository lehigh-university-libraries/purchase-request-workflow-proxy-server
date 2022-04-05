package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;

abstract class HoldingsEnrichment implements EnrichmentService {
    
    final Config config;

    enum IdentifierType { 
        TitleAndContributor, ISBN, OclcNumber; 
    }

    HoldingsEnrichment(Config config) {
        this.config = config;
    }

    abstract String buildEnrichmentMessage(long totalRecords, String identifier, IdentifierType identifierType,
        String identifierForWebsiteUrl, String holdingsType);

    String buildRecordsUrl(String identifier, IdentifierType identifierType, String identifierForWebsiteUrl) {
        if (Config.LocalHoldings.LinkDestination.VuFind == (config.getLocalHoldings().getLinkTo()) &&
            // VuFind may only support ISBN search
            IdentifierType.ISBN == identifierType) {
            
            return config.getVuFind().getBaseUrl()
                + "/Search/Results?lookfor=" + identifier + "&type=AllFields&limit=20";
        }
        else {
            return config.getFolio().getWebsiteBaseUrl() 
                + "/inventory?qindex=identifier&query=" 
                + (identifierForWebsiteUrl != null ? identifierForWebsiteUrl : identifier) 
                + "&sort=title";
        }
    }

}
