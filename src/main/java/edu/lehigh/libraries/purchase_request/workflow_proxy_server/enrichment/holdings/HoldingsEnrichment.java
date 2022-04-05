package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.holdings;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
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
                    + ConnectionUtil.encodeUrl("lookfor0_0[]") + "=" + ConnectionUtil.encodeUrl(identifier)
                    + "&" + ConnectionUtil.encodeUrl("lookfor1_0[]") + "=" 
                    + "&" + ConnectionUtil.encodeUrl("lookfor2_0[]") + "=" 
                    + "&" + ConnectionUtil.encodeUrl("type0[]") + "=" + "Title" 
                    + "&" + "join1=AND"
                    + "&" + ConnectionUtil.encodeUrl("lookfor0_1[]") + "=" + ConnectionUtil.encodeUrl(secondaryIdentifier)
                    + "&" + ConnectionUtil.encodeUrl("lookfor1_1[]") + "=" 
                    + "&" + ConnectionUtil.encodeUrl("lookfor2_1[]") + "=" 
                    + "&" + ConnectionUtil.encodeUrl("type1[]") + "=" + "Author";
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
