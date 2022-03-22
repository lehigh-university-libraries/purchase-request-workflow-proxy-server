package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RequesterEnrichment implements EnrichmentService {

    @Autowired
    private LdapTemplate ldapTemplate;

    private final WorkflowService workflowService;

    RequesterEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        manager.addListener(this);

        log.debug("RequesterEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String username = purchaseRequest.getRequesterUsername();
        if (username == null) {
            log.debug("Skipping Requester enrichment, no username provided.");
            return;
        }

        LdapQuery query = LdapQueryBuilder.query()
            .where("uid").is(username);
        List<String> result = ldapTemplate.search(query, 
            (AttributesMapper<String>) attributes -> (String)attributes.get("description").get()
        );

        String description = result.get(0);
        String role = trimToRole(description);
        workflowService.enrich(purchaseRequest, EnrichmentType.REQUESTER_ROLE, role);        
    }

    /**
     * Trim anything after a hyphen.
     */
    private String trimToRole(String description){
        int end = description.indexOf("-");
        if (end > -1) {
            return description.substring(0, end).trim();
        }
        else {
            return description;
        }
    }
    
}
