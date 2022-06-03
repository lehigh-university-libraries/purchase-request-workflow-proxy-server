package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.librarian;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.LibrarianCallNumbersConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.librarian-call-numbers", havingValue="service")
@ConditionalOnWebApplication
public class LibrarianEnrichment implements EnrichmentService {

    private final WorkflowService workflowService;
    private final LibrarianCallNumbersConnection connection;

    private final String BASE_URL;

    LibrarianEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        connection = new LibrarianCallNumbersConnection();

        BASE_URL = config.getLibrarianCallNumbers().getBaseUrl();

        manager.addListener(this, 500);
        log.debug("LibrarianEnrichment ready.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getLibrarianUsername() != null) {
            log.debug("Librarian already assigned: " + purchaseRequest.getLibrarianUsername());
            return;
        }

        String callNumber = purchaseRequest.getCallNumber();
        if (callNumber == null) {
            log.debug("Skipping LibrarianEnrichment, no call number provided.");
            return;
        }

        String url = BASE_URL + "/search?callNumber=" + ConnectionUtil.encodeUrl(callNumber);
        JSONArray responseArray;
        try {
            responseArray = connection.executeGetForArray(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting librarians for call number.", e);
            return;
        }

        List<String> usernames = new ArrayList<String>();
        for (int i=0; i < responseArray.length(); i++) {
            JSONObject librarian = responseArray.getJSONObject(i);
            String username = librarian.getString("username");
            log.debug("Found a Librarian: " + username);
            usernames.add(username);
        }
        workflowService.enrich(purchaseRequest, EnrichmentType.LIBRARIANS, usernames);
    }
    
}
