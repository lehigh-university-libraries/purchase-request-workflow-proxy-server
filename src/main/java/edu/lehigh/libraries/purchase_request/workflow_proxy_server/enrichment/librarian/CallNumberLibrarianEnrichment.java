package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.librarian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.LibrarianDataConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.librarian.call-numbers.enabled", havingValue="true")
@ConditionalOnWebApplication
public class CallNumberLibrarianEnrichment implements EnrichmentService {

    private final WorkflowService workflowService;
    private final LibrarianDataConnection connection;

    private final String NO_CALL_NUMBER_USERNAME;

    CallNumberLibrarianEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        connection = new LibrarianDataConnection(config);

        NO_CALL_NUMBER_USERNAME = config.getLibrarian().getCallNumbers().getNoCallNumberUsername();

        manager.addListener(this, 1050);
        log.debug("CallNumberLibrarianEnrichment ready.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getLibrarianUsername() != null) {
            log.debug("Librarian already assigned: " + purchaseRequest.getLibrarianUsername());
            return;
        }

        String callNumber = purchaseRequest.getCallNumber();
        if (callNumber == null) {
            if (NO_CALL_NUMBER_USERNAME == null) {
                log.debug("Skipping LibrarianEnrichment, no call number provided and no default librarian.");
                return;
            }
            else {
                log.debug("No call number provided, using default librarian username.");
                workflowService.enrich(purchaseRequest, EnrichmentType.LIBRARIANS, 
                    Arrays.asList(new String[] { NO_CALL_NUMBER_USERNAME} ));
                return;
            }
        }

        log.debug("Enriching librarian by call number: " + callNumber);
        String url = "/search?callNumber=" + ConnectionUtil.encodeUrl(callNumber);
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
