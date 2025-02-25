package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnWebApplication
@Slf4j
public class EnrichmentManager {
    
    private WorkflowService workflowService;
    private SortedMap<Integer, List<EnrichmentService>> enrichmentServices;

    EnrichmentManager(WorkflowService workflowService) {
        this.workflowService = workflowService;

        // TreeMap is naturally sorted by its key, so priority order is maintained
        enrichmentServices = new TreeMap<Integer, List<EnrichmentService>>();
    }

    /**
     * Add a listener.
     * 
     * @param priority Lower number is *more* important.
     */
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
        enrich(purchaseRequest, null);
    }

    @Async
    public void notifyRepeatEnrichment(PurchaseRequest purchaseRequest, EnrichmentRequest repeatEnrichmentRequest) {
        enrich(purchaseRequest, repeatEnrichmentRequest);
    }
 
    private void enrich(PurchaseRequest purchaseRequest, EnrichmentRequest repeatEnrichmentRequest) {
        try {
            MDC.put("key", purchaseRequest.getKey());
            for (Map.Entry<Integer, List<EnrichmentService>> entry: enrichmentServices.entrySet()) {
                List<EnrichmentService> listAtPriority = entry.getValue();
                for (EnrichmentService service : listAtPriority) {
                    if (repeatEnrichmentRequest == null ||
                        repeatEnrichmentRequest.getEnrichments().contains(service.getClass().getSimpleName())) {
                        try {
                            service.enrichPurchaseRequest(purchaseRequest);

                            // get updated purchase request
                            purchaseRequest = workflowService.findByKey(purchaseRequest.getKey());
                        }
                        catch (Exception e) {
                            log.error("Caught exception during enrichment: ", e);
                        }
                    }
                }
            }
            log.debug("Done with all enrichment.");
            if (repeatEnrichmentRequest == null) {
                workflowService.initialEnrichmentComplete(purchaseRequest);
            }
        }
        finally {
            MDC.remove("key");
        }
    }
}
