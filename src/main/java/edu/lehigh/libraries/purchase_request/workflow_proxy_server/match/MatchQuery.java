package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import edu.lehigh.libraries.purchase_request.model.validation.NoHtml;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class MatchQuery {

    @NoHtml
    private String title;
    
    @NoHtml
    private String contributor;

}
