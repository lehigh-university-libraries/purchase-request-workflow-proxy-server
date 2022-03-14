package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public interface WorkflowServiceListener {

    void purchaseRequested(PurchaseRequest purchaseRequest);

    void purchaseApproved(PurchaseRequest purchaseRequest);
    
}
