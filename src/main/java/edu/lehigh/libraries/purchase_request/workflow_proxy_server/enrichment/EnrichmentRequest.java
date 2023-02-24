package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class EnrichmentRequest {
    
    private List<String> enrichments = Collections.emptyList();

}
