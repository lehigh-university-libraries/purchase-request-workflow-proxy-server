package edu.lehigh.libraries.purchase_request.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import lombok.extern.slf4j.Slf4j;

// Adapted from https://thoughtfulsoftware.wordpress.com/2013/05/26/sanitizing-user-input-part-ii-validation-with-spring-rest/
@Slf4j
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    private static final String ALLOW_CHARS = "['&]";

    private final PolicyFactory PROHIBIT_ALL_HTML = new HtmlPolicyBuilder().toFactory();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        value = value.replaceAll(ALLOW_CHARS, "");
        String sanitized = PROHIBIT_ALL_HTML.sanitize(value);
        if (!sanitized.equals(value)) {
            log.debug("Difference between original: " + value + " and sanitized: " + sanitized);
        }
        return sanitized.equals(value);
    }
    
}
