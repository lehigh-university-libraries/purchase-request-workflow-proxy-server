package edu.lehigh.libraries.purchase_request.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
public class PurchaseRequest {

    public static final String MATCHING_CHARS_START = "^[";
    public static final String MATCHING_CHARS_END = "]+$";

    // for sanitation; not validation for a specific WorkflowService key
    public static final String SANITIZED_STRING_PATTERN = "^[A-Za-z0-9-_\\s]+$";
    public static final String SANITIZED_NUMERIC_PATTERN = "^[0-9]+$";

    public static final String SANITIZED_TITLE_CHARACTERS = "A-Za-z0-9-_\\s:/";
    public static final String SANITIZED_TITLE_PATTERN = 
        MATCHING_CHARS_START + SANITIZED_TITLE_CHARACTERS + MATCHING_CHARS_END;

    public static final String SANITIZED_CONTRIBUTOR_CHARACTERS = "A-Za-z0-9-_\\s,\\.";
    public static final String SANITIZED_CONTRIBUTOR_PATTERN = 
        MATCHING_CHARS_START + SANITIZED_CONTRIBUTOR_CHARACTERS + MATCHING_CHARS_END;

    public static final String SANITIZED_OCLC_NUMBER_PATTERN = SANITIZED_NUMERIC_PATTERN;
    public static final String KEY_PATTERN = SANITIZED_STRING_PATTERN;

    private static final String OCLC_NUMBER_PREFIX = "(OCoLC)";

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String key;

    private Long id;

    @NotNull
    @Pattern(regexp = SANITIZED_TITLE_PATTERN)
    private String title;
    
    @Pattern(regexp = SANITIZED_CONTRIBUTOR_PATTERN)
    private String contributor;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String isbn;

    @Pattern(regexp = SANITIZED_OCLC_NUMBER_PATTERN)
    private String oclcNumber;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String format;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String speed;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String destination;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String clientName;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String reporterName;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String requesterUsername;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String requesterRole;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String creationDate;

    public String getPrefixedOclcNumber() {
        return OCLC_NUMBER_PREFIX + oclcNumber;
    }

}
