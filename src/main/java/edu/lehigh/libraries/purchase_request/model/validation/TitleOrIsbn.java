package edu.lehigh.libraries.purchase_request.model.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TitleOrIsbnValidator.class)
public @interface TitleOrIsbn {
    String message() default "Title or ISBN required";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
