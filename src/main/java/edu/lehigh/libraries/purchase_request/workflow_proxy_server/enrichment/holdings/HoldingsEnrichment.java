package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;

abstract class HoldingsEnrichment implements EnrichmentService {
    
    final Config config;

    enum IdentifierType { 
        TitleAndContributor, ISBN, OclcNumber; 
    }

    HoldingsEnrichment(Config config) {
        this.config = config;
    }

    abstract String buildEnrichmentMessage(long totalRecords, String identifier, String secondaryIdentifier, 
        IdentifierType identifierType, String identifierForWebsiteUrl, String holdingsType);

    String buildRecordsUrl(String identifier, String secondaryIdentifier, 
        IdentifierType identifierType, String identifierForWebsiteUrl) {
            
        if (Config.LocalHoldings.LinkDestination.VuFind == config.getLocalHoldings().getLinkTo() &&
            // VuFind may not support OCLC number search
            IdentifierType.OclcNumber != identifierType) {
            
            if (IdentifierType.ISBN == identifierType) {
                return config.getVuFind().getBaseUrl()
                    + "/Search/Results?lookfor=" + identifier + "&type=AllFields&limit=20";
            }
            else if (IdentifierType.TitleAndContributor == identifierType) {
                return config.getVuFind().getBaseUrl()
                    + "/Search/Results?"
                    + "join=AND"
                    + "&" + ConnectionUtil.encodeUrl("lookfor0[]") + "=" + ConnectionUtil.encodeUrl(identifier)
                    + "&" + ConnectionUtil.encodeUrl("type0[]") + "=" + "Title" 
                    + "&" + ConnectionUtil.encodeUrl("lookfor0[]") + "=" + ConnectionUtil.encodeUrl(secondaryIdentifier)
                    + "&" + ConnectionUtil.encodeUrl("type0[]") + "=" + "Author"
                    + "&" + ConnectionUtil.encodeUrl("bool0[]") + "=" + "AND";
            }
            else {
                throw new IllegalArgumentException("Unexpected identifier type: " + identifierType);
            }
        }
        else {
            return config.getFolio().getWebsiteBaseUrl() 
                + "/inventory?qindex=identifier&query=" 
                + (identifierForWebsiteUrl != null ? identifierForWebsiteUrl : identifier) 
                + "&sort=title";
        }
    }

}
