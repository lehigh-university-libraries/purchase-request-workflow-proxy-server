package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.oasis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OasisResult {

    private final String URL_PREFIX = "https://oasis.proquest.com/OpenURL?isbn=";
 
    private String title;
    private String author;
    private String isbn;
    private String pubYear;
    private String url;
    private String coverImage;
    private String localPrice;

    public void setUrl(String url) {
        // This format, returned by the OASIS API, does not appear to work.
        // https://oasis.proquest.com/search/isbn/9780679601517

        // This OpenURL format does work.  So ignore the method argument for now.
        this.url = URL_PREFIX + isbn; 
    }

}
