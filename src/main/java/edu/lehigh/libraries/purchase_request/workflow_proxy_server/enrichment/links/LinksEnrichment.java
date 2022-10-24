package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.links;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
@ConditionalOnProperty(name = "workflow.links.enabled", havingValue = "true")
@ConditionalOnWebApplication
public class LinksEnrichment implements EnrichmentService {

    private static final String GOOGLE_SCHOLAR_BASE_URL = "https://scholar.google.com/scholar?q=";

    private final boolean USE_GOOGLE_SCHOLAR;

    private final WorkflowService workflowService;

    LinksEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;

        USE_GOOGLE_SCHOLAR = config.getLinks().isGoogleScholarEnabled();

        manager.addListener(this);
        log.debug("LinksEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title == null) {
            log.debug("Skipping links enrichment, no title provided.");
            return;
        }

        String comment = "Links to Additional Information:";
        comment += enrichGoogleScholar(title);
        workflowService.enrich(purchaseRequest, EnrichmentType.LINKS, comment);
    }

    private String enrichGoogleScholar(String title) {
        if (!USE_GOOGLE_SCHOLAR) {
            return "";
        }

        String url = GOOGLE_SCHOLAR_BASE_URL + URLEncoder.encode(title, StandardCharsets.UTF_8);
        return "\n\n[Google Scholar|" + url + "]";
    }
}
