package edu.lehigh.libraries.purchase_request.model;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import edu.lehigh.libraries.purchase_request.model.validation.NoHtml;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class PurchaseRequest {

    public static final String MATCHING_CHARS_START = "^[";
    public static final String MATCHING_CHARS_END = "]+$";

    // for sanitation; not validation for a specific WorkflowService key
    public static final String SANITIZED_STRING_PATTERN = "^[A-Za-z0-9_:\\.\\s-]+$";
    public static final String SANITIZED_NUMERIC_PATTERN = "^[0-9]+$";
    public static final String SANITIZED_USERNAME_PATTERN = "^[A-Za-z0-9]+$";

    public static final String SANITIZED_OCLC_NUMBER_PATTERN = SANITIZED_NUMERIC_PATTERN;
    public static final String SANITIZED_ISBN_PATTERN = "^[0-9X\\-]+$";
    public static final String SANITIZED_CALL_NUMBER_PATTERN = SANITIZED_STRING_PATTERN;
    public static final String KEY_PATTERN = SANITIZED_STRING_PATTERN;

    private static final String OCLC_NUMBER_PREFIX = "(OCoLC)";

    @Pattern(regexp = KEY_PATTERN)
    private String key;

    private Long id;

    @NoHtml
    private String status;

    @NotNull
    @NoHtml
    private String title;
    
    @NoHtml
    private String contributor;

    @Pattern(regexp = SANITIZED_ISBN_PATTERN)
    private String isbn;

    @Pattern(regexp = SANITIZED_OCLC_NUMBER_PATTERN)
    private String oclcNumber;

    @Pattern(regexp = SANITIZED_CALL_NUMBER_PATTERN)
    private String callNumber;

    @NoHtml
    private String format;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String speed;

    @NoHtml
    private String destination;

    @NoHtml
    private String clientName;

    @Pattern(regexp = SANITIZED_USERNAME_PATTERN)
    private String reporterName;

    @NoHtml
    private String requesterUsername;

    @NoHtml
    private String requesterComments;

    @NoHtml
    private String requesterRole;

    @NoHtml
    private String librarianUsername;

    @NoHtml
    private String fundCode;

    @NoHtml
    private String objectCode;

    private List<Comment> postRequestComments;

    @NoHtml
    private String postPurchaseId;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String creationDate;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String updateDate;

    public String getPrefixedOclcNumber() {
        return OCLC_NUMBER_PREFIX + oclcNumber;
    }

    @Getter @Setter @EqualsAndHashCode @ToString
    public static class Comment {

        @NoHtml
        private String text;

        @Pattern(regexp = SANITIZED_STRING_PATTERN)
        private String creationDate;

    }

}
