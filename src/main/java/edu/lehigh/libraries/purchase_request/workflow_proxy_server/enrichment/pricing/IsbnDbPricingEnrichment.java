package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.IsbnDbConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;

abstract class IsbnDbPricingEnrichment implements EnrichmentService {
    
    static final String BASE_URL = "https://api2.isbndb.com";

    final WorkflowService workflowService;
    final IsbnDbConnection connection;

    IsbnDbPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        this.connection = new IsbnDbConnection(config);

        manager.addListener(this);
    }

}
