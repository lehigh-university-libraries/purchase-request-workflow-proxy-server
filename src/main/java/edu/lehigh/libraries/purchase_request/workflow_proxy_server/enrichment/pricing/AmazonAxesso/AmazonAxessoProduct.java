package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.pricing.AmazonAxesso;

import java.util.LinkedList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter class AmazonAxessoProduct {

    @Setter(AccessLevel.NONE)
    private List<AmazonAxessoProduct.Variation> variations;

    private String description;
    private String pageUrl;
    private String imageUrl;

    AmazonAxessoProduct() {
        variations = new LinkedList<AmazonAxessoProduct.Variation>();
    }

    void add(AmazonAxessoProduct.Variation variation) {
        variations.add(variation);
    }

    @Getter @Setter
    public static class Variation {
        private String format;
        private String price;
    }

}