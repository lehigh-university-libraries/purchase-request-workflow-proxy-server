package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import lombok.extern.slf4j.Slf4j;

@RestController
@Validated
@Slf4j
public class WorkflowController {
    
    private final WorkflowService service;

    WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping("/purchase-requests")
    List<PurchaseRequest> all() {
        log.info("Request: GET /purchase-requests");
        return service.findAll();
    }

    @GetMapping("/purchase-requests/{key}")
    ResponseEntity<PurchaseRequest> getByKey(@PathVariable String key) {
        log.info("Request: GET /purchase-requests/" + key);
        PurchaseRequest purchaseRequest = service.findByKey(key);
        if (purchaseRequest == null) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<PurchaseRequest>(purchaseRequest, HttpStatus.OK);
    }    

    @PostMapping("/purchase-requests")
    ResponseEntity<PurchaseRequest> addPurchaseRequest(
        @Valid @RequestBody PurchaseRequest purchaseRequest,
        Authentication authentication) {
            
        log.info("Request: POST /purchase-requests " + purchaseRequest);
        purchaseRequest.setClientName(authentication.getName());
        PurchaseRequest savedRequest = service.save(purchaseRequest); 
        return new ResponseEntity<PurchaseRequest>(savedRequest, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    List<PurchaseRequest> search(@RequestParam String reporterName) {
        log.info("Request: GET /search/ " + reporterName);
        SearchQuery query = new SearchQuery();
        if (reporterName != null) {
            query.setReporterName(reporterName);
        }
        return service.search(query);
    }

}
