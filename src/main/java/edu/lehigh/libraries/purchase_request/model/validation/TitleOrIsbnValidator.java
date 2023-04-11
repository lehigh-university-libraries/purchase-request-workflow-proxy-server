package edu.lehigh.libraries.purchase_request.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;

public class TitleOrIsbnValidator implements ConstraintValidator<TitleOrIsbn, PurchaseRequest> {
    
    @Override
    public boolean isValid(PurchaseRequest purchaseRequest, ConstraintValidatorContext context) {
        if (purchaseRequest == null) {
            return false;
        }

        if (purchaseRequest.getTitle() != null || purchaseRequest.getIsbn() != null) {
            return true;
        }

        return false;
    }

}
