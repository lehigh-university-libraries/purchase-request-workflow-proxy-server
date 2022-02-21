package edu.lehigh.libraries.purchase_request.model;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class PurchaseRequest {

    @NotNull
    private String key;

    @NotNull
    private Long id;

    @NotNull
    private String title;
    
    @NotNull
    private String contributor;

}
