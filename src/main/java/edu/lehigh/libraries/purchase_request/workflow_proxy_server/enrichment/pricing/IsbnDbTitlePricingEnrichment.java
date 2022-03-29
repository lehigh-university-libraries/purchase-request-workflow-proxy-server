package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IsbnDbTitlePricingEnrichment extends IsbnDbPricingEnrichment {

    IsbnDbTitlePricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        super(manager, workflowService, config);
        log.debug("IsbnDbTitlePricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title == null) {
            log.debug("Skipping pricing enrichment by title, no title provided.");
            return;
        }

        List<IsbnDbSearchResult> results = searchByTitle(title);
        String comment = "Pricing by title: " + title;
        if (results.size() == 0) {
            comment += "\n No ISBNs with list price found for this title.";
            log.debug("No pricing found for title: " + title);
        }
        else {
            for (IsbnDbSearchResult result : results) {
                comment += "\n- " + result.toString();
            }
            log.debug("Found pricing for title: " + title);
        }

        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private List<IsbnDbSearchResult> searchByTitle(String title) {
        String encodedTitle = connection.encode("\"" + title + "\"");
        String url = BASE_URL + "/books/" + encodedTitle + "?column=title";
        JSONObject jsonResult = connection.execute(url);
        List<IsbnDbSearchResult> searchResults = parseResults(jsonResult);
        return searchResults;
    }

    private List<IsbnDbSearchResult> parseResults(JSONObject jsonResult) {
        JSONArray booksJson = jsonResult.getJSONArray("books");
        List<IsbnDbSearchResult> books = new LinkedList<IsbnDbSearchResult>();
        for (int i=0; i < booksJson.length(); i++) {
            JSONObject bookJson = booksJson.getJSONObject(i);
            IsbnDbSearchResult book = parseSearchResult(bookJson);
            if (book.getMsrp() != null) {
                books.add(book);
            }
        }
        return books;
    }

    private IsbnDbSearchResult parseSearchResult(JSONObject bookJson) {
        IsbnDbSearchResult book = new IsbnDbSearchResult();
        book.setContributors(parseStringArray(bookJson.getJSONArray(IsbnDbSearchResult.CONTRIBUTORS_FIELD)));
        book.setBinding(bookJson.getString(IsbnDbSearchResult.BINDING_FIELD));
        book.setIsbn(bookJson.getString(IsbnDbSearchResult.ISBN_FIELD));
        book.setMsrp(bookJson.optString(IsbnDbSearchResult.MSRP_FIELD, null));
        book.setTitle(bookJson.getString(IsbnDbSearchResult.TITLE_FIELD));

        // Various records have dates in formats from YYYY to YYYYMMDD to full 
        // StandardDateFormat.  Just pull out the year.
        String dateString = bookJson.optString(IsbnDbSearchResult.PUBLICATION_YEAR_FIELD, null);
        if (dateString != null && dateString.length() > 0) {
            log.debug("parsing non null date string: " + dateString + " For isbn " + book.getIsbn());
            book.setPublicationYear(dateString.substring(0, 4));
        }

        return book;
    }   

    private String[] parseStringArray(JSONArray jsonArray) {
        String[] values = new String[jsonArray.length()];
        for (int i=0; i < jsonArray.length(); i++) {
            String value = jsonArray.getString(i);
            values[i] = value;
        }
        return values;
    }

}