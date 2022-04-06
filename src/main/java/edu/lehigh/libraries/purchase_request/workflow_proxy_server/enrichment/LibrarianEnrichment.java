package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.LibrarianCallNumbersConnection;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LibrarianEnrichment implements EnrichmentService {

    private final WorkflowService workflowService;
    private final LibrarianCallNumbersConnection connection;

    private final String BASE_URL;

    LibrarianEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        connection = new LibrarianCallNumbersConnection();

        BASE_URL = config.getLibrarianCallNumbers().getBaseUrl();

        manager.addListener(this);
        log.debug("LibrarianEnrichment ready.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String callNumber = purchaseRequest.getCallNumber();
        if (callNumber == null) {
            log.debug("Skipping LibrarianEnrichment, no call number provided.");
        }

        String url = BASE_URL + "/search?callNumber=" + callNumber;
        JSONArray responseArray;
        try {
            responseArray = connection.executeGetForArray(url);
        }
        catch (Exception e) {
            log.error("Caught exception getting librarians for call number.", e);
            return;
        }

        for (int i=0; i < responseArray.length(); i++) {
            JSONObject librarian = responseArray.getJSONObject(i);
            String username = librarian.getString("username");
            log.debug("Found a Librarian: " + username);
            workflowService.enrich(purchaseRequest, EnrichmentType.LIBRARIAN, username);
        }
    }
    
}
