package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public interface EnrichmentService {
    
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest);

}
