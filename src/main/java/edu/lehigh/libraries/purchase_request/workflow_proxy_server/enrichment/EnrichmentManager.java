package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.util.LinkedList;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

@Service
public class EnrichmentManager {
    
    private List<EnrichmentService> enrichmentServices;

    EnrichmentManager() {
        enrichmentServices = new LinkedList<EnrichmentService>();
    }

    public void addListener(EnrichmentService service) {
        enrichmentServices.add(service);
    }

    @Async
    public void notifyNewPurchaseRequest(PurchaseRequest purchaseRequest) {
        for (EnrichmentService service : enrichmentServices) {
            service.enrichPurchaseRequest(purchaseRequest);
        }
    }

}
