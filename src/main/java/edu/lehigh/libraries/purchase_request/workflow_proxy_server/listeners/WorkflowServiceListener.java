package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public interface WorkflowServiceListener {

    void purchaseRequested(PurchaseRequest purchaseRequest);

    void purchaseApproved(PurchaseRequest purchaseRequest);
    
    void purchaseDenied(PurchaseRequest purchaseRequest);

    void purchaseArrived(PurchaseRequest purchaseRequest);
    
}
