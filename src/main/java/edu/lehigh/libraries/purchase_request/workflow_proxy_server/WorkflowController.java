package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.Match;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.MatchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.match.MatchService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@RestController
@ConditionalOnWebApplication
@Validated
@Slf4j
public class WorkflowController {
    
    private final WorkflowService service;
    private final MatchService matchService;
    private final EnrichmentManager enrichmentManager;

    WorkflowController(WorkflowService service, MatchService matchService, 
        EnrichmentManager enrichmentManager) {
    
        this.service = service;
        this.matchService = matchService;
        this.enrichmentManager = enrichmentManager;
    }

    @GetMapping("/purchase-requests")
    List<PurchaseRequest> all() {
        log.debug("Request: GET /purchase-requests");
        return service.findAll();
    }

    @GetMapping("/purchase-requests/{key}")
    ResponseEntity<PurchaseRequest> getByKey(@PathVariable @Pattern(regexp = PurchaseRequest.KEY_PATTERN) String key) {
        log.debug("Request: GET /purchase-requests/" + key);
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
            
        log.debug("Request: POST /purchase-requests " + purchaseRequest);
        purchaseRequest.setClientName(authentication.getName());
        PurchaseRequest savedRequest = service.save(purchaseRequest); 
        enrichmentManager.notifyNewPurchaseRequest(savedRequest);

        return new ResponseEntity<PurchaseRequest>(savedRequest, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    List<PurchaseRequest> search(SearchQuery query) {
        log.debug("Request: GET /search/ " + query);
        return service.search(query);
    }

    @GetMapping("/search-matches")
    List<Match> searchMatches(MatchQuery query) {
        log.debug("Request: GET /search-matches? " + query);
        return matchService.search(query);
    }

    @ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Constraint violation")
    @ExceptionHandler(ConstraintViolationException.class)
    public void constraintViolation() {
        // no op
        log.debug("found constraint violation");
    }

}
