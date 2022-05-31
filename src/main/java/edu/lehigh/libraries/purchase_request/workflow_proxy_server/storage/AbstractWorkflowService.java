package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage;

import java.util.LinkedList;
import java.util.List;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;

abstract public class AbstractWorkflowService implements WorkflowService {

    private List<WorkflowServiceListener> listeners;

    protected AbstractWorkflowService() {        
        listeners = new LinkedList<WorkflowServiceListener>();
    }

    @Override
    public void addListener(WorkflowServiceListener listener) {
        listeners.add(listener);
    }

    protected void notifyPurchaseRequestCreated(PurchaseRequest createdRequest) {
        for (WorkflowServiceListener listener : listeners) {
            listener.purchaseRequested(createdRequest);
        }
    }
    
    protected void notifyPurchaseRequestApproved(PurchaseRequest purchaseRequest) {
        for (WorkflowServiceListener listener : listeners) {
            listener.purchaseApproved(purchaseRequest);
        }
    }

}
