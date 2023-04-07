package edu.lehigh.libraries.purchase_request.workflow_proxy_server.post_purchase.folio;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.PurchasedItem;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.FolioConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.post_purchase.PostPurchaseService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name="workflow.post-purchase.data-source", havingValue="FOLIO")
@ConditionalOnWebApplication
public class FolioPostPurchaseService implements PostPurchaseService {

    private static final String INSTANCES_PATH = "/search/instances";

    private final String PROXY_PREFIX;

    private final FolioConnection connection;

    public FolioPostPurchaseService(Config config) throws Exception {
        this.connection = new FolioConnection(config);

        PROXY_PREFIX = config.getPostPurchase().getProxyPrefix();

        log.debug("FolioPostPurchaseService listening.");
    }

    @Override
    public PurchasedItem getFromRequest(PurchaseRequest purchaseRequest) {
        log.debug("Loading post-purchase info for request: " + purchaseRequest.getKey());
        PurchasedItem purchasedItem = new PurchasedItem(purchaseRequest);

        String hrid = purchaseRequest.getPostPurchaseId();
        if (hrid == null) { 
            log.error("Cannot retrieve PurchasedItem with null postPurchaseId: " + purchaseRequest);
            return null;
        }
        String queryString = "("
            + "hrid=" + hrid
            + ")";
        JSONObject responseObject;
        try {
            responseObject = connection.executeGet(INSTANCES_PATH, queryString);
        }
        catch (Exception e) {
            log.error("Could not retrieve FOLIO record for id: " + hrid, e);
            return null;
        }
        JSONArray instances = responseObject.getJSONArray("instances");
        if (instances.isEmpty()) {
            log.error("No instances match HRID " + hrid);
            return null;
        }
        JSONObject instance = instances.getJSONObject(0);
        JSONArray electronicAccessArray = instance.getJSONArray("electronicAccess");
        if (!electronicAccessArray.isEmpty()) {
            JSONObject electronicAccess = electronicAccessArray.getJSONObject(0);
            String uri = electronicAccess.getString("uri");
            if (PROXY_PREFIX != null) {
                uri = PROXY_PREFIX + ConnectionUtil.encodeUrl(uri);
            }
            purchasedItem.setElectronicAccessUrl(uri);
        }

        return purchasedItem;
    }
    
}
