package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
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
@ConditionalOnExpression("'${workflow.pricing}'.equals('IsbnDB') and '${workflow.isbn-db.method}'.equals('title')")
@ConditionalOnWebApplication
public class IsbnDbTitlePricingEnrichment extends IsbnDbPricingEnrichment {

    private static final Comparator<IsbnDbSearchResult> RESULTS_ORDER = new ResultsOrder();

    private final boolean FILTER_ON_CONTRIBUTOR;

    IsbnDbTitlePricingEnrichment(EnrichmentManager manager, WorkflowService workflowService, Config config) {
        super(manager, workflowService, config);
        FILTER_ON_CONTRIBUTOR = config.getIsbnDb().getTitleSearch().isFilterOnContributor();
        log.debug("IsbnDbTitlePricingEnrichment ready");
    }

    @Override
    public void enrichPurchaseRequest(PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title == null) {
            log.debug("Skipping pricing enrichment by title, no title provided.");
            return;
        }

        List<IsbnDbSearchResult> results = search(purchaseRequest);
        log.debug("results size: " + results.size());
        if (FILTER_ON_CONTRIBUTOR) {
            String contributor = purchaseRequest.getContributor();
            if (contributor == null) {
                log.debug("cannot filter on null contributor");
            }
            else {
                results = filterByContributor(results, purchaseRequest.getContributor());
            }
            log.debug("after filtering on contributor, results size: " + results.size());
        }
        results.sort(RESULTS_ORDER);

        String comment = "Pricing by title: " + title;
        if (results.size() == 0) {
            comment += "\n No ISBNs with list price found for this title.";
            log.debug("No pricing found for title: " + title);
        }
        else {
            comment += formatAsWikiTable(results);
            log.debug("Found pricing for title: " + title);
        }

        workflowService.enrich(purchaseRequest, EnrichmentType.PRICING, comment);
    }

    private String formatAsWikiTable(List<IsbnDbSearchResult> results) {
        String comment = "\n||Title||Contributors||Publication Year||ISBN||Binding||MSRP||";
        for (IsbnDbSearchResult result : results) {
            comment += "\n|" 
                + result.getTitle() + "|"
                + String.join(", ", result.getContributors()) + "|"
                + result.getPublicationYear() + "|"
                + result.getIsbn() + "|"
                + result.getBinding() + "|"
                + result.getMsrp() + "|"
                ;
        }
        return comment;
    }

    private List<IsbnDbSearchResult> search(PurchaseRequest purchaseRequest) {
        String encodedTitle = connection.encode("\"" + purchaseRequest.getTitle() + "\"");
        String url = BASE_URL + "/books/" + encodedTitle + "?column=title";
        JSONObject jsonResult = connection.execute(url);
        List<IsbnDbSearchResult> searchResults = parseResults(jsonResult);
        return searchResults;
    }

    private List<IsbnDbSearchResult> parseResults(JSONObject jsonResult) {
        List<IsbnDbSearchResult> books = new LinkedList<IsbnDbSearchResult>();
        if (jsonResult.has("books")) {
            JSONArray booksJson = jsonResult.getJSONArray("books");
            for (int i=0; i < booksJson.length(); i++) {
                JSONObject bookJson = booksJson.getJSONObject(i);
                IsbnDbSearchResult book = parseSearchResult(bookJson);
                if (book.getMsrp() != null) {
                    books.add(book);
                }
            }
        }
        return books;
    }

    private IsbnDbSearchResult parseSearchResult(JSONObject bookJson) {
        IsbnDbSearchResult book = new IsbnDbSearchResult();
        book.setContributors(parseStringArray(bookJson.optJSONArray(IsbnDbSearchResult.CONTRIBUTORS_FIELD)));
        book.setBinding(bookJson.optString(IsbnDbSearchResult.BINDING_FIELD, null));
        book.setIsbn(bookJson.optString(IsbnDbSearchResult.ISBN_FIELD, null));
        book.setMsrp(bookJson.optString(IsbnDbSearchResult.MSRP_FIELD, null));
        book.setTitle(bookJson.getString(IsbnDbSearchResult.TITLE_FIELD));

        // Various records have dates in formats from YYYY to YYYYMMDD to full 
        // StandardDateFormat.  Just pull out the year.
        String dateString = bookJson.optString(IsbnDbSearchResult.PUBLICATION_YEAR_FIELD, null);
        if (dateString != null && dateString.length() > 0) {
            book.setPublicationYear(dateString.substring(0, 4));
        }

        return book;
    }   

    private String[] parseStringArray(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        String[] values = new String[jsonArray.length()];
        for (int i=0; i < jsonArray.length(); i++) {
            String value = jsonArray.getString(i);
            values[i] = value;
        }
        return values;
    }

    private List<IsbnDbSearchResult> filterByContributor(List<IsbnDbSearchResult> results, String targetContributor) {
        Iterator<IsbnDbSearchResult> resultsIterator = results.iterator();
        List<String> targetContributorNames = Arrays.asList(targetContributor.split("[, ]"));
        while (resultsIterator.hasNext()) {
            boolean foundMatch = false;
            String[] resultContributors = resultsIterator.next().getContributors();
            if (resultContributors != null) {
                for (int i=0; i < resultContributors.length; i++) {
                    String resultContributor = resultContributors[i];
                    Iterator<String> targetContributorNamesIterator = targetContributorNames.iterator();
                    while (targetContributorNamesIterator.hasNext()) {
                        String targetName = targetContributorNamesIterator.next();
                        if (resultContributor.contains(targetName)) {
                            foundMatch = true;
                        }
                    }
                }
            }
            if (!foundMatch) {
                resultsIterator.remove();
            }
        }
        return results;
    }

    private static class ResultsOrder implements Comparator<IsbnDbSearchResult> {

        private BindingOrder BINDING_ORDER = new BindingOrder();

        @Override
        public int compare(IsbnDbSearchResult result1, IsbnDbSearchResult result2) {
            // Sort first by binding.
            int bindingCompare = BINDING_ORDER.compare(result1.getBinding(), result2.getBinding());
            if (bindingCompare != 0) {
                return bindingCompare;
            }

            // Within a binding, sort by publication year (recent first)
            if (result1.getPublicationYear() == null) {
                return 1;
            }
            return -1 * result1.getPublicationYear().compareTo(result2.getPublicationYear());
        }

        
    }

    private static class BindingOrder implements Comparator<String> {

        private static final String HARDCOVER = "Hardcover";

        @Override
        public int compare(String binding1, String binding2) {
            // Sort hardcovers at the top
            if (!Objects.equals(binding1, binding2)) {
                if (HARDCOVER.equals(binding1)) {
                    return -1;
                }
                else if (HARDCOVER.equals(binding2)) {
                    return 1;
                }
            }
            return binding1.compareTo(binding2);
        }

    }

}