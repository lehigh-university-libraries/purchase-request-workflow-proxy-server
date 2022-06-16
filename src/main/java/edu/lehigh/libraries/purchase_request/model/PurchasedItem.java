package edu.lehigh.libraries.purchase_request.model;

import javax.validation.constraints.NotNull;

import edu.lehigh.libraries.purchase_request.model.validation.NoHtml;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class PurchasedItem {

    public PurchasedItem(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    @NotNull
    private PurchaseRequest purchaseRequest;

    @NoHtml
    private String electronicAccessUrl;
    
}
