package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage;

import java.util.List;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;

public interface WorkflowService {
    
    List<PurchaseRequest> findAll();

    PurchaseRequest findByKey(String key);

    PurchaseRequest save(PurchaseRequest purchaseRequest);

    List<PurchaseRequest> search(SearchQuery query);

    void enrich(PurchaseRequest purchaseRequest, EnrichmentType enrichmentType, Object data);

    void enrichmentComplete(PurchaseRequest purchaseRequest);

    void addListener(WorkflowServiceListener listener);

}
