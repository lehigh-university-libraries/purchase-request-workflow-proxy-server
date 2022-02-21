package edu.lehigh.libraries.purchase_request.model;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class PurchaseRequest {

    @Id @GeneratedValue
    private Long id;

    @NotNull
    private String title;
    
    @NotNull
    private String contributor;

}
