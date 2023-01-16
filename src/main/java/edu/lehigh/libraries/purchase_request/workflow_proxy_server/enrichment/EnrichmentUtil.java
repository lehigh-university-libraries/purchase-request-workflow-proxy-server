package edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment;

import java.math.BigDecimal;
import java.text.NumberFormat;

public final class EnrichmentUtil {
    
    private static final NumberFormat PRICE_FORMATTER = NumberFormat.getCurrencyInstance();

    public static String formatString(String raw) {
        if (raw == null) {
            return "--";
        }
        return raw;
    }

    public static String formatPrice(Object rawObject) {
        if (rawObject == null) {
            return "--";
        }
        String raw;
        try {
            raw = (String)rawObject;
        }
        catch (ClassCastException e) {
            return rawObject.toString();
        }
        BigDecimal parsed = new BigDecimal(raw);
        return PRICE_FORMATTER.format(parsed);
    }

}
