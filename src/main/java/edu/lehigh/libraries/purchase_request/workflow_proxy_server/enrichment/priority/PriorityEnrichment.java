package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.priority;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
@ConditionalOnProperty(name="workflow.priority.enabled", havingValue="true")
@ConditionalOnWebApplication
public class PriorityEnrichment implements EnrichmentService {
 
    private String KEY_SEPARATOR = "__";
    private String KEY_DEFAULT = "default";

    private final WorkflowService workflowService;

    private final Map<String, Long> PRIORITY_BY_REQUEST_TYPE;
    private final Pattern REQUESTER_INFO_ROLE_PATTERN;


    PriorityEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;

        PRIORITY_BY_REQUEST_TYPE = config.getPriority().getByRequestType();
        REQUESTER_INFO_ROLE_PATTERN = config.getLdap().getRequesterInfoRolePattern();

        manager.addListener(this, 900);
        log.debug("PriorityEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String requestType = purchaseRequest.getRequestType();
        if (requestType == null) {
            log.debug("Skipping priority enrichment, no request type provided.");
            return;
        }

        if (PRIORITY_BY_REQUEST_TYPE == null) {
            log.debug("Skipping priority enrichment, no priority mapping by request type provided.");
            return;
        }

        // Default for request type
        Long priority = PRIORITY_BY_REQUEST_TYPE.get(requestType + KEY_SEPARATOR + KEY_DEFAULT);
        if (priority != null) {
            log.debug("Found default priority for request type: " + priority);
        }
        
        // Narrow by requester role if possible.
        if (REQUESTER_INFO_ROLE_PATTERN != null 
            && purchaseRequest.getRequesterInfo() != null) {

            String requesterInfo = purchaseRequest.getRequesterInfo();
            Matcher matcher = REQUESTER_INFO_ROLE_PATTERN.matcher(requesterInfo);
            if (matcher.find()) {
                String requesterRole = matcher.group("ROLE");
                if (requesterRole != null) {
                    Long requesterRolePriority = 
                        PRIORITY_BY_REQUEST_TYPE.get(requestType + KEY_SEPARATOR + requesterRole);
                    if (requesterRolePriority != null) {
                        log.debug("Found more specific priority by request type + role: " + requesterRolePriority);
                        priority = requesterRolePriority;
                    }
                }
            }
        }

        if (priority == null) {
            log.debug("No priority rules found for this request.");
            return;
        }

        workflowService.enrich(purchaseRequest, EnrichmentType.PRIORITY, priority);
    }

}
