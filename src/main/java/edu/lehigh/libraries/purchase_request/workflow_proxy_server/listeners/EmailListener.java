package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailListener implements WorkflowServiceListener {
    
    EmailListener(WorkflowService service) {
        service.addListener(this);
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        // TODO Actually send email
        log.debug("Pretending to send mail for approved request: " + purchaseRequest);
    }

}
