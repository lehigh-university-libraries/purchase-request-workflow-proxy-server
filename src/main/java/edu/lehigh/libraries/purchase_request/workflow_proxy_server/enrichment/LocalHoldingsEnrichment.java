package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;

abstract class LocalHoldingsEnrichment implements EnrichmentService {
    
    final Config config;

    enum IdentifierType { 
        ISBN, OclcNumber; 
    }

    LocalHoldingsEnrichment(Config config) {
        this.config = config;
    }

    String buildEnrichmentMessage(long totalRecords, String identifier, IdentifierType identifierType,
        String identifierForWebsiteUrl) {

        String message;
        if (totalRecords > 0) {
            String recordsUrl;
            if (Config.LocalHoldings.LinkDestination.VuFind == (config.getLocalHoldings().getLinkTo()) &&
                // VuFind may only support ISBN search
                IdentifierType.ISBN == identifierType) {
                recordsUrl = config.getVuFind().getBaseUrl()
                    + "/Search/Results?lookfor=" + identifier + "&type=AllFields&limit=20";
            }
            else {
                recordsUrl = config.getFolio().getWebsiteBaseUrl() 
                    + "/inventory?qindex=identifier&query=" 
                    + (identifierForWebsiteUrl != null ? identifierForWebsiteUrl : identifier) 
                    + "&sort=title";
            }
            String recordsLink = "<a href=\"" + recordsUrl.toString() + "\">" + totalRecords + " instances</a>";
            message = "Local holdings: " + recordsLink + " instances matching this " + identifierType + ".\n";
        }
        else {
            message = "NO Local holdings: No instances matching this " + identifierType + ".\n";
        }
        return message;

    }

}
