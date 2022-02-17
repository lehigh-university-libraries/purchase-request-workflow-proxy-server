package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

@RestController
public class WorkflowController {
    
    private final WorkflowService service;

    WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping("/purchase-requests")
    List<PurchaseRequest> all() {
        return service.findAll();
    }

}
