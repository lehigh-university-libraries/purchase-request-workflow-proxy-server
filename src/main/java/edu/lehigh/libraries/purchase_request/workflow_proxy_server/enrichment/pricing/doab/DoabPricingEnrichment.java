package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.doab;

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

/**
 * Enriches the purchase request with matches from the 
 * Directory of Open Access Books.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "workflow.doab.enabled", havingValue = "true")
@ConditionalOnWebApplication
public class DoabPricingEnrichment implements EnrichmentService {

    private final String BASE_URL = "https://directory.doabooks.org/rest";

    private final WorkflowService workflowService;
    private final DoabConnection connection;
    
    DoabPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;

        this.connection = new DoabConnection();
        manager.addListener(this);
        log.debug("DoabPricingEnrichment ready");
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

        List<DoabSearchResult> results = search(title, contributor);
        log.debug("results size: " + results.size());

        String comment = "Directory of Open Access Books";
        if (results.size() == 0) {
            comment += ": No matches found.";
            log.debug("No DOAB matches found.");
        }
        else {
            comment += formatAsWikiTable(results);
            log.debug("Found availability for title: " + title);
        } 

        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private List<DoabSearchResult> search(String title, String contributor) {
        String query = "dc.title:\"" + title + "\" AND "
            + "("
            + "dc.contributor.author:\"" + contributor + "\""
            + " OR "
            + "dc.contributor.editor:\"" + contributor + "\""
            + ")";
        String url = BASE_URL + "/search?expand=metadata&query=" + ConnectionUtil.encodeUrl(query);
        JSONArray jsonResult = connection.executeForArray(url);
        List<DoabSearchResult> searchResults = parseResults(jsonResult);
        return searchResults;
    }

    private List<DoabSearchResult> parseResults(JSONArray resultsJson) {
        List<DoabSearchResult> results = new LinkedList<DoabSearchResult>();
        for (int i=0; i < resultsJson.length(); i++) {
            JSONObject resultJson = resultsJson.getJSONObject(i);
            DoabSearchResult result = parseResult(resultJson);
            results.add(result);
        }
        return results;
    }

    private DoabSearchResult parseResult(JSONObject resultJson) {
        DoabSearchResult result = new DoabSearchResult();
        result.setTitle(resultJson.optString("name", null));

        JSONArray metadataList = resultJson.getJSONArray("metadata");
        List<String> contributorsList = new LinkedList<String>();
        for (int i=0; i < metadataList.length(); i++) {
            JSONObject metadataItem = metadataList.getJSONObject(i);
            String key = metadataItem.optString("key", null);
            String value = metadataItem.optString("value", null);
            if ("dc.contributor.author".equals(key)) {
                contributorsList.add(value);
            }
            if ("dc.contributor.editor".equals(key)) {
                contributorsList.add(value);
            }
            else if ("publisher.name".equals(key)) {
                result.setPublisherName(value);
            }
            else if ("dc.identifier.uri".equals(key)) {
                result.setUrl(value);
            }
        }
        result.setContributors(String.join("\n", contributorsList));
        return result;
    }

    private String formatAsWikiTable(List<DoabSearchResult> results) {
        String comment = "\n\n||Title||Contributor||Publisher||";
        for (DoabSearchResult result: results) {
            comment += "\n|" 
                + "[" + result.getTitle() + "|" + result.getUrl() + "]|"
                + result.getContributors() + "|"
                + result.getPublisherName() + "|";
        }
        return comment;
    }

}
