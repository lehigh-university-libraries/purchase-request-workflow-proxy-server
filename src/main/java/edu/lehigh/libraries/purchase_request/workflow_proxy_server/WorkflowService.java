package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;

public interface WorkflowService {
    
    List<PurchaseRequest> findAll();

    PurchaseRequest findByKey(String key);

    PurchaseRequest save(PurchaseRequest purchaseRequest);

    List<PurchaseRequest> search(SearchQuery query);

    void addListener(WorkflowServiceListener listener);

}
