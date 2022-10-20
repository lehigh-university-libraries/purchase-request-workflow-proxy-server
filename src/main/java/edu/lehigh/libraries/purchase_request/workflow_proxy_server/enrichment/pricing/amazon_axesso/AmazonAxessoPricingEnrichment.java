package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.amazon_axesso;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
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
@ConditionalOnProperty(name = "workflow.amazon-axesso.enabled", havingValue = "true")
@ConditionalOnWebApplication
public class AmazonAxessoPricingEnrichment implements EnrichmentService {

    private final String API_HOST = "axesso-axesso-amazon-data-service-v1.p.rapidapi.com";
    private final String BASE_URL = "https://" + API_HOST + "/amz";

    private final String API_DOMAIN_CODE;
    private final String PAGE_URL_PREFIX;
    private final Integer MAX_PRODUCTS;

    private final WorkflowService workflowService;
    private final AmazonAxessoConnection connection;
    
    AmazonAxessoPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;

        API_DOMAIN_CODE = config.getAmazonAxesso().getApiDomainCode();
        PAGE_URL_PREFIX = config.getAmazonAxesso().getPageUrlPrefix();
        MAX_PRODUCTS = config.getAmazonAxesso().getMaxProducts();

        this.connection = new AmazonAxessoConnection(config, API_HOST);
        manager.addListener(this);
        log.debug("AmazonAxessoPricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title == null) {
            log.debug("Skipping pricing enrichment by title, no title provided.");
            return;
        }
        String keywords = title;

        String contributor = purchaseRequest.getContributor();
        if (contributor != null) {
            keywords += " " + contributor;
        }

        List<AmazonAxessoProduct> results = search(keywords);
        log.debug("results size: " + results.size());

        String comment = "Amazon (via Axesso) Pricing"
            + "\nfirst " + MAX_PRODUCTS + " matches";
        if (results.size() == 0) {
            comment += "\n No Amazon products found for this title.";
            log.debug("No Amazon products found for this title: " + title);
        }
        else {
            comment += formatAsWikiTables(results);
            log.debug("Found pricing for title: " + title);
        } 

        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private List<AmazonAxessoProduct> search(String keywords) {
        String encodedKeywords = connection.encode(keywords);
        String url = BASE_URL 
            + "/amazon-search-by-keyword-asin"
            + "?domainCode=" + API_DOMAIN_CODE
            + "&page=1" 
            + "&excludeSponsored=true"
            + "&sortBy=relevanceblender"
            + "&withCache=true"
            + "&keyword=" + encodedKeywords;
        JSONObject jsonResult = connection.execute(url);
        List<AmazonAxessoProduct> searchResults = parseResults(jsonResult);
        return searchResults;
    }

    private List<AmazonAxessoProduct> parseResults(JSONObject jsonResult) {
        List<AmazonAxessoProduct> results = new LinkedList<AmazonAxessoProduct>();
        JSONArray productsJson = jsonResult.getJSONArray("searchProductDetails");
        for (int i=0; i < MAX_PRODUCTS && i < productsJson.length(); i++) {
            JSONObject productJson = productsJson.getJSONObject(i);
            AmazonAxessoProduct product = parseProduct(productJson);
            results.add(product);
        }
        return results;
    }

    private AmazonAxessoProduct parseProduct(JSONObject productJson) {
        AmazonAxessoProduct product = new AmazonAxessoProduct();
        product.setDescription(productJson.optString("productDescription", null));
        product.setImageUrl(productJson.optString("imgUrl", null));
        product.setPageUrl(PAGE_URL_PREFIX + productJson.optString("asin", null));
   
        JSONArray variationsJson = productJson.getJSONArray("variations");
        for (int i=0; i < variationsJson.length(); i++) {
            JSONObject variationJson = variationsJson.getJSONObject(i);
            AmazonAxessoProduct.Variation variation = parseVariation(variationJson);
            product.add(variation);
        }
        return product;
    }

    private AmazonAxessoProduct.Variation parseVariation(JSONObject variationJson) {
        AmazonAxessoProduct.Variation variation = new AmazonAxessoProduct.Variation();
        variation.setFormat(variationJson.optString("value", null));
        variation.setPrice(variationJson.optString("price", null));
        return variation;
    }

    private String formatAsWikiTables(List<AmazonAxessoProduct> products) {
        String comment = "";
        for (AmazonAxessoProduct product : products) {
            comment += formatAsWikiTable(product);
        }
        return comment;
    }

    private String formatAsWikiTable(AmazonAxessoProduct product) {
        String comment = 
            "\n\nTitle: [" + product.getDescription() + "|" + product.getPageUrl() + "]"
            + "\n!" + product.getImageUrl() + "|align=right, vspace=10, hspace=10, height=150!"
            + "\n||Binding||Price||";
        for (AmazonAxessoProduct.Variation variation : product.getVariations()) {
            comment += "\n|" 
                + variation.getFormat() + "|"
                + variation.getPrice() + "|";
        }
        return comment;
    }

}
