package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.requester;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.requester", havingValue="ldap")
@ConditionalOnWebApplication
public class RequesterEnrichment implements EnrichmentService {

    @Autowired
    private LdapTemplate ldapTemplate;

    private final String LDAP_USERNAME_QUERY_FIELD;
    private final String LDAP_INFO_RESULT_FIELD;
    private final Map<String, String> LDAP_INFO_RESULT_OVERRIDES;

    private final WorkflowService workflowService;

    RequesterEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;

        LDAP_USERNAME_QUERY_FIELD = config.getLdap().getUsernameQueryField();
        LDAP_INFO_RESULT_FIELD = config.getLdap().getInfoResultField();
        LDAP_INFO_RESULT_OVERRIDES = config.getLdap().getInfoResultOverrides();

        manager.addListener(this, 800);
        log.debug("RequesterEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String username = purchaseRequest.getRequesterUsername();
        if (username == null) {
            log.debug("Skipping Requester enrichment, no username provided.");
            return;
        }

        String roleOverride = LDAP_INFO_RESULT_OVERRIDES.get(username);
        if (roleOverride != null) {
            log.debug("Using LDAP override for user " + username + ": " + roleOverride);
            workflowService.enrich(purchaseRequest, EnrichmentType.REQUESTER_INFO, roleOverride);
            return;
        }

        LdapQuery query = LdapQueryBuilder.query()
            .where(LDAP_USERNAME_QUERY_FIELD).is(username);
        List<String> result = ldapTemplate.search(query, 
            (AttributesMapper<String>) attributes -> (String)attributes.get(LDAP_INFO_RESULT_FIELD).get()
        );

        if (result.size() == 0) {
            log.error("No user found: " + username);
            return;
        }

        String role = result.get(0);
        workflowService.enrich(purchaseRequest, EnrichmentType.REQUESTER_INFO, role);        
    }
  
}
