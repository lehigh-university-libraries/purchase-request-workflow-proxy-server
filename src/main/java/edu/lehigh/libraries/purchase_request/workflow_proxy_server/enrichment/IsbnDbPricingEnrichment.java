package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IsbnDbPricingEnrichment implements EnrichmentService {

    private static final String BASE_URL = "https://api2.isbndb.com";

    private final WorkflowService workflowService;
    private final IsbnDbConnection connection;

    IsbnDbPricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        this.workflowService = workflowService;
        this.connection = new IsbnDbConnection(config);

        manager.addListener(this);
        log.debug("IsbnDbPricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String isbn = purchaseRequest.getIsbn();
        if (isbn == null) {
            log.debug("Skipping pricing enrichment, no ISBN provided.");
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

    private static class IsbnDbConnection {

        private final Config config;

        private String API_KEY;
        private CloseableHttpClient client;    
    
        private IsbnDbConnection(Config config) {
            this.config = config;
            initConnection();
        }

        private void initConnection() {
            API_KEY = config.getIsbnDb().getApiKey();
            client = HttpClientBuilder.create()
                .build();
        }

        private JSONObject execute(String url) {
            HttpUriRequest getRequest = RequestBuilder.get()
            .setUri(url)
            .setHeader(HttpHeaders.AUTHORIZATION, API_KEY)
            .build();
        
            CloseableHttpResponse response;
            String responseString;
            try {
                response = client.execute(getRequest);
                HttpEntity entity = response.getEntity();
                responseString = EntityUtils.toString(entity);
                log.debug("got response string: " + responseString);
            }
            catch (Exception e) {
                log.error("Could not get book data from IsbnDb.", e);
                return null;
            }
            JSONObject jsonObject = new JSONObject(responseString);
            return jsonObject;
        }
    
    }

}
