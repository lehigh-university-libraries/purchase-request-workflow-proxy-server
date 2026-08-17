package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public interface WorkflowServiceListener {

    default void purchaseRequested(PurchaseRequest purchaseRequest) {}

    default void purchaseApproved(PurchaseRequest purchaseRequest) {}

    default void purchaseDenied(PurchaseRequest purchaseRequest) {}

    default void purchaseReceived(PurchaseRequest purchaseRequest) {}

    default void purchaseArrived(PurchaseRequest purchaseRequest) {}
    
}
