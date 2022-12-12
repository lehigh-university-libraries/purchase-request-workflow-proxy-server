package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.doab;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DoabSearchResult {
    
    private String title;
    private String contributors;
    private String publisherName;
    private String url;

}
