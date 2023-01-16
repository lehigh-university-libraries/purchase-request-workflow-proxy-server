package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.oasis;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.ConnectionUtil;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name = "workflow.oasis.enabled", havingValue = "true")
@ConditionalOnWebApplication
public class OasisPricingEnrichment implements EnrichmentService {

    private final OasisConnection connection;
    private final WorkflowService workflowService;

    private final String LOCAL_CURRENCY;
    private final int MAX_RESULTS;

    OasisPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        this.connection = new OasisConnection(config);

        LOCAL_CURRENCY = config.getOasis().getLocalCurrency();
        MAX_RESULTS = config.getOasis().getMaxResults();

        manager.addListener(this);
        log.debug("OasisPricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title == null) {
            log.debug("Skipping pricing enrichment, no title provided.");
            return;
        }

        String contributor = purchaseRequest.getContributor();
        if (contributor == null) {
            log.debug("Skipping pricing enrichment, no contributor provided.");
            return;
        }

        List<OasisResult> results = search(title, contributor);
        log.debug("results size: " + results.size());

        String comment;
        if (results.size() == 0) {
            comment = "OASIS: No results found for this title & contributor.";
        }
        else {
            comment = "OASIS Pricing Results";
            comment += formatAsWikiTable(results);
        }
        
        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private List<OasisResult> search(String title, String contributor) {
        List<OasisResult> oasisResults = new LinkedList<OasisResult>();
        String path = "/search"
            + "?title=" + ConnectionUtil.encodeUrl(title)
            + "&author=" + ConnectionUtil.encodeUrl(contributor);
        JSONObject jsonResult = connection.query(path);
        int status = jsonResult.getInt("status");
        if (status == OasisConnection.STATUS_NOT_FOUND) {
            log.debug("No matches found.");
            return List.of();
        }
        else if (status != OasisConnection.STATUS_FOUND) {
            log.warn("Unexpected status " + status + ": " + jsonResult.getString("statusmessage"));
            return List.of();
        }

        JSONArray results = (JSONArray)jsonResult.query("/response/results");
        for (int i=0; i < results.length(); i++ ) {
            JSONObject result = results.getJSONObject(i);
            OasisResult oasisResult = parseResult(result);
            if (oasisResult.getLocalPrice() != null) {
                oasisResults.add(oasisResult);
            }

            if (oasisResults.size() >= MAX_RESULTS) {
                break;
            }
        }
        return oasisResults;
    }

    private OasisResult parseResult(JSONObject jsonResult) {
        OasisResult result = new OasisResult();
        result.setTitle(jsonResult.optString("title", null));
        result.setAuthor(jsonResult.optString("author", null));
        result.setIsbn(jsonResult.optString("isbn", null));
        result.setPubYear(jsonResult.optString("pubYear", null));
        result.setUrl(jsonResult.optString("url", null));
        result.setCoverImage(jsonResult.optString("coverImage", null));
        result.setLocalPrice(parseLocalPrice(jsonResult));
        return result;
    }

    private String parseLocalPrice(JSONObject jsonResult) {
        JSONArray prices = jsonResult.optJSONArray("listPrice");
        if (prices == null) {
            return null;
        }

        for (int i=0; i < prices.length(); i++) {
            JSONObject price = prices.getJSONObject(i);
            if (LOCAL_CURRENCY.equals(price.getString("currency"))) {
                return price.getBigDecimal("price").toPlainString();
            }
        }
        return null;
    }

    private String formatAsWikiTable(List<OasisResult> results) {
        String comment = "\n||Title||Contributors||Publication Year||ISBN||MSRP||";
        for (OasisResult result : results) {
            comment += "\n|" 
                + "[" + result.getTitle() + "|" + result.getUrl() + "]" + "|"
                + result.getAuthor() + "|"
                + formatValue(result.getPubYear()) + "|"
                + result.getIsbn() + "|"
                + result.getLocalPrice() + "|"
                ;
        }
        return comment;
    }

    private String formatValue(String raw) {
        if (raw == null) {
            return "--";
        }
        return raw;
    }

}
