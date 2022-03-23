package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class MatchQuery {

    public static final String SANITIZED_QUOTED_TITLE_PATTERN = 
        PurchaseRequest.MATCHING_CHARS_START 
        + "\"" + PurchaseRequest.SANITIZED_TITLE_CHARACTERS 
        + "\"" + PurchaseRequest.MATCHING_CHARS_END;

    public static final String SANITIZED_QUOTED_CONTRIBUTOR_PATTERN = 
        PurchaseRequest.MATCHING_CHARS_START 
        + "\"" + PurchaseRequest.SANITIZED_CONTRIBUTOR_CHARACTERS 
        + "\"" + PurchaseRequest.MATCHING_CHARS_END;

    @NotNull
    @Pattern(regexp = SANITIZED_QUOTED_TITLE_PATTERN)
    private String title;
    
    @Pattern(regexp = SANITIZED_QUOTED_CONTRIBUTOR_PATTERN)
    private String contributor;

}
