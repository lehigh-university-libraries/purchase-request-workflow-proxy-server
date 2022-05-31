package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing;

import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnExpression("'${workflow.pricing}'.equals('IsbnDb') and '${workflow.isbn-db.method}'.equals('isbn')")
@ConditionalOnWebApplication
public class IsbnDbIsbnPricingEnrichment extends IsbnDbPricingEnrichment {

    IsbnDbIsbnPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        super(manager, workflowService, config);
        log.debug("IsbnDbIsbnPricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String isbn = purchaseRequest.getIsbn();
        if (isbn == null) {
            log.debug("Skipping pricing enrichment by ISBN, no ISBN provided.");
            return;
        }

        String listPrice = getListPrice(isbn);
        String comment = "List Price from ISBNdb: " + listPrice;
        log.debug("Found pricing for ISBN: " + isbn);

        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private String getListPrice(String isbn) {
        String url = BASE_URL + "/book/" + isbn;
        JSONObject result = connection.execute(url);
        Object msrp;
        try {
            msrp = result.query("/book/msrp");
        }
        catch (Exception e) {
            log.debug("can't get MSRP");
            return null;
        }
        if (msrp == null) {
            log.debug("no MSRP");
            return null;
        }
        String listPrice = msrp.toString();
        return listPrice;    
    }

}
