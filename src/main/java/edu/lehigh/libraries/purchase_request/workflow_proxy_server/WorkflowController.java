package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

@RestController
@Validated
public class WorkflowController {
    
    private final WorkflowService service;

    WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping("/purchase-requests")
    List<PurchaseRequest> all() {
        return service.findAll();
    }

    @PostMapping("/purchase-requests")
    ResponseEntity<String> addPurchaseRequest(@Valid @RequestBody PurchaseRequest purchaseRequest) {
        service.save(purchaseRequest); 
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
