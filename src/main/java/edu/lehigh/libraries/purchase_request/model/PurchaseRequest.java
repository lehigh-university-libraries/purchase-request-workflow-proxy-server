package edu.lehigh.libraries.purchase_request.model;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PurchaseRequest {

    @NotNull
    private String title;
    
    @NotNull
    private String contributor;

}
