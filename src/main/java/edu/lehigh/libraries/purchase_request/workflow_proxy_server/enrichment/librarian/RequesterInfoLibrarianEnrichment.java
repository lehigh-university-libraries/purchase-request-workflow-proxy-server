package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.librarian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@ConditionalOnProperty(name="workflow.librarian.requester-info.enabled", havingValue="true")
@ConditionalOnWebApplication
public class RequesterInfoLibrarianEnrichment implements EnrichmentService {

    private final WorkflowService workflowService;
    private final LibrarianDataConnection connection;

    private final Pattern REQUESTER_INFO_DEPARTMENT_PATTERN;
    private final String REQUESTER_BUT_NO_DEPARTMENT_USERNAME;

    RequesterInfoLibrarianEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        connection = new LibrarianDataConnection(config);

        REQUESTER_BUT_NO_DEPARTMENT_USERNAME = config.getLibrarian().getRequesterInfo().getRequesterButNoDepartmentUsername();
        REQUESTER_INFO_DEPARTMENT_PATTERN = config.getLdap().getRequesterInfoDepartmentPattern();

        manager.addListener(this, 1000);
        log.debug("RequesterInfoLibrarianEnrichment ready.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getLibrarianUsername() != null) {
            log.debug("Librarian already assigned: " + purchaseRequest.getLibrarianUsername());
            return;
        }

        if (REQUESTER_INFO_DEPARTMENT_PATTERN != null 
            && purchaseRequest.getRequesterInfo() != null) {

            String requesterInfo = purchaseRequest.getRequesterInfo();
            log.debug("Enriching librarian by requester info: " + requesterInfo);
            Matcher matcher = REQUESTER_INFO_DEPARTMENT_PATTERN.matcher(requesterInfo);
            if (matcher.find()) {
                String requesterDepartment = matcher.group("DEPARTMENT");
                if (requesterDepartment != null) {

                    List<String> librarianUsernames = getLibrariansForDepartment(requesterDepartment);
                    if (librarianUsernames.size() > 0) {
                        workflowService.enrich(purchaseRequest, EnrichmentType.LIBRARIANS, librarianUsernames);
                        return;
                    }
                }
            }
            // No matching department
            log.debug("No matching librarian found, defaulting to " + REQUESTER_BUT_NO_DEPARTMENT_USERNAME);
            workflowService.enrich(purchaseRequest, EnrichmentType.LIBRARIANS, 
                Arrays.asList(new String[] { REQUESTER_BUT_NO_DEPARTMENT_USERNAME} ));
        }
        else {
            log.debug("No requester info [" + purchaseRequest.getRequesterInfo() 
                + "] or no pattern [" + REQUESTER_INFO_DEPARTMENT_PATTERN + "]");
        }
    }

    private List<String> getLibrariansForDepartment(String department) {
        String url = "/search?department=" + ConnectionUtil.encodeUrl(department);
        JSONArray responseArray;
        try {
            responseArray = connection.executeGetForArray(url);
        }
        catch (Exception e) {
            throw new RuntimeException("Caught exception getting librarians for department.", e);
        }

        List<String> usernames = new ArrayList<String>();
        for (int i=0; i < responseArray.length(); i++) {
            JSONObject librarian = responseArray.getJSONObject(i);
            String username = librarian.getString("username");
            log.debug("Found a Librarian: " + username);
            usernames.add(username);
        }
        return usernames;
    }
    
}
