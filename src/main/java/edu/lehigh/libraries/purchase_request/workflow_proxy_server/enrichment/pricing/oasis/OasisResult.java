package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.oasis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OasisResult {
 
    private String title;
    private String author;
    private String isbn;
    private String pubYear;
    private String url;
    private String coverImage;
    private String localPrice;

}
