package edu.lehigh.libraries.purchase_request.workflow_proxy_server.match;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.validation.NoHtml;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class MatchQuery {

    @NoHtml
    private String title;
    public void setTitle(String title) {
        this.title = PurchaseRequest.normalizeTitle(title);
    }
    
    @NoHtml
    private String contributor;
    public void setContributor(String contributor) {
        this.contributor = PurchaseRequest.normalizeContributor(contributor);
    }

    @NoHtml
    private String isbn;
    public void setIsbn(String isbn) {
        this.isbn = PurchaseRequest.normalizeIsbn(isbn);
    }

}
