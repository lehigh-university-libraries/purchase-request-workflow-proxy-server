package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IsbnDbSearchResult {
    
    public static final String
        TITLE_FIELD = "title",
        CONTRIBUTORS_FIELD = "authors",
        PUBLICATION_YEAR_FIELD = "date_published",
        ISBN_FIELD = "isbn",
        BINDING_FIELD = "binding",
        MSRP_FIELD = "msrp";

    /** Ignoring additional response fields present in response. */

    private String title;
    private String[] contributors;
    private String publicationYear;
    private String isbn;
    private String binding;
    private String msrp;

}
