package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

@Service
public class EnrichmentManager {
    
    private SortedMap<Integer, List<EnrichmentService>> enrichmentServices;

    EnrichmentManager() {
        enrichmentServices = new TreeMap<Integer, List<EnrichmentService>>();
    }

    public void addListener(EnrichmentService service) {
        addListener(service, 0);
    }

    public void addListener(EnrichmentService service, int priority) {
        List<EnrichmentService> listAtPriority = enrichmentServices.get(Integer.valueOf(priority));
        if (listAtPriority == null) {
            listAtPriority = new LinkedList<EnrichmentService>();
            enrichmentServices.put(Integer.valueOf(priority), listAtPriority);
        }
        listAtPriority.add(service);
    }

    @Async
    public void notifyNewPurchaseRequest(PurchaseRequest purchaseRequest) {
        for (Map.Entry<Integer, List<EnrichmentService>> entry: enrichmentServices.entrySet()) {
            List<EnrichmentService> listAtPriority = entry.getValue();
            for (EnrichmentService service : listAtPriority) {
                service.enrichPurchaseRequest(purchaseRequest);
            }
        }
    }

}
