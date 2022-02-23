package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public interface WorkflowServiceListener {

    void purchaseApproved(PurchaseRequest purchaseRequest);
    
}
