package edu.lehigh.libraries.purchase_request.model;

import java.util.List;
import java.util.regex.Matcher;

import javax.validation.constraints.Pattern;

import edu.lehigh.libraries.purchase_request.model.validation.NoHtml;
import edu.lehigh.libraries.purchase_request.model.validation.TitleOrIsbn;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode @ToString
@TitleOrIsbn
public class PurchaseRequest {

    public static final String MATCHING_CHARS_START = "^[";
    public static final String MATCHING_CHARS_END = "]+$";

    // for sanitation; not validation for a specific WorkflowService key
    public static final String SANITIZED_STRING_PATTERN = "^[A-Za-z0-9,_:\\.\\s-]+$";
    public static final String SANITIZED_NUMERIC_PATTERN = "^[0-9]+$";
    public static final String SANITIZED_USERNAME_PATTERN = "^[A-Za-z0-9]+$";
    public static final String SANITIZED_REQUEST_TYPE_PATTERN = "^[A-Za-z0-9_]+$";

    public static final String SANITIZED_OCLC_NUMBER_PATTERN = SANITIZED_NUMERIC_PATTERN;
    public static final String SANITIZED_ISBN_PATTERN = "^[0-9X\\-]+$";
    public static final String SANITIZED_CALL_NUMBER_PATTERN = SANITIZED_STRING_PATTERN;
    public static final String KEY_PATTERN = SANITIZED_STRING_PATTERN;

    private static final String OCLC_NUMBER_PREFIX = "(OCoLC)";

    private static final java.util.regex.Pattern TRAILING_SLASH_PATTERN = java.util.regex.Pattern.compile(
        "(?<BASE>.*?)(?<SLASH>\\s*/)");
    // Regex representing the life years sometimes appended to a contributor.
    private static final java.util.regex.Pattern TRAILING_YEARS_PATTERN = java.util.regex.Pattern.compile(
        "(?<BASE>.*?)(?<COMMAnSPACE>[,]*\\s+)(?<YEARS>[\\(]?[-\\d]{3,9}[\\)]?)");

    @Pattern(regexp = KEY_PATTERN)
    private String key;

    private Long id;

    @NoHtml
    private String status;

    @NoHtml
    private String title;
    public void setTitle(String title) {
        this.title = normalizeTitle(title);
    }

    @NoHtml
    private String contributor;
    public void setContributor(String contributor) {
        this.contributor = normalizeContributor(contributor);
    }

    @Pattern(regexp = SANITIZED_ISBN_PATTERN)
    private String isbn;
    public void setIsbn(String isbn) {
        this.isbn = normalizeIsbn(isbn);
    }

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

    @Pattern(regexp = SANITIZED_REQUEST_TYPE_PATTERN)
    private String requestType;

    @NoHtml
    private String clientName;

    @Pattern(regexp = SANITIZED_USERNAME_PATTERN)
    private String reporterName;

    @NoHtml
    private String requesterUsername;

    @NoHtml
    private String requesterComments;

    @NoHtml
    private String requesterInfo;

    @NoHtml
    private String librarianUsername;

    @NoHtml
    private String fundCode;

    @NoHtml
    private String objectCode;

    private List<Comment> postRequestComments;

    @NoHtml
    private String postPurchaseId;

    @NoHtml
    private String decisionReason;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String creationDate;

    @Pattern(regexp = SANITIZED_STRING_PATTERN)
    private String updateDate;

    public String getPrefixedOclcNumber() {
        return OCLC_NUMBER_PREFIX + oclcNumber;
    }

    static private String sanitize(String raw) {
        // Remove any double-quotes
        return raw.replaceAll("\"", "");
    }

    static public String normalizeTitle(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        raw = sanitize(raw);
        Matcher matcher = TRAILING_SLASH_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group("BASE");
        }
        return raw;
    }

    static private String normalizeContributor(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        Matcher matcher = TRAILING_YEARS_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group("BASE");
        }
        return raw;
    }

    static private String normalizeIsbn(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        return raw;
    }

    @Getter @Setter @EqualsAndHashCode @ToString
    public static class Comment {

        @NoHtml
        private String text;

        @Pattern(regexp = SANITIZED_STRING_PATTERN)
        private String creationDate;

    }

}
