package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.budget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.FolioConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.budget-code", havingValue="FOLIO")
public class FolioBudgetEnrichment implements EnrichmentService {

    private final WorkflowService workflowService;
    private FolioConnection connection;

    FolioBudgetEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config)
        throws Exception {

        this.workflowService = workflowService;
        connection = new FolioConnection(config);

        manager.addListener(this, 550);
        log.debug("FolioBudgetEnrichment ready.");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String selector = purchaseRequest.getLibrarianUsername();
        if (selector == null) {
            log.warn("Cannot enrich budget info; no selector specified.");
            return;
        }

        String url = "/bl-users";
        String queryString = "include=users&query=username=" + purchaseRequest.getLibrarianUsername();
        try {
            JSONObject responseObject = connection.executeGet(url, queryString);
            JSONArray compositeUsers = responseObject.getJSONArray("compositeUsers");
            JSONObject compositeUser = compositeUsers.getJSONObject(0);
            JSONObject user = compositeUser.getJSONObject("users");
            JSONObject customFields = user.getJSONObject("customFields");

            try {
                String defaultFundCode = customFields.getString("defaultFundCode");
                workflowService.enrich(purchaseRequest, EnrichmentType.FUND_CODE, defaultFundCode);
    
                String defaultObjectCode = customFields.getString("defaultObjectCode");
                workflowService.enrich(purchaseRequest, EnrichmentType.OBJECT_CODE, defaultObjectCode);
            }
            catch (JSONException e) {
                log.warn("No default budget info for " + purchaseRequest.getLibrarianUsername());
            }
        }
        catch (Exception e) {
            log.error("Could not enrich with budget info: ", e);
        }
    }
    
}
