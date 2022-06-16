package edu.lehigh.libraries.purchase_request.workflow_proxy_server.post_purchase;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.PurchasedItem;

public interface PostPurchaseService {
    
    public PurchasedItem getFromRequest(PurchaseRequest purchaseRequest);

}
