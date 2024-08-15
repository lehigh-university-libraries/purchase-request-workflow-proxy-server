package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.jira;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.connection.JiraConnection;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.AbstractWorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name="workflow.storage", havingValue="jira")
@ConditionalOnWebApplication
@Slf4j
public class JiraWorkflowService extends AbstractWorkflowService {

    private JiraConnection client;
    private Config config;

    private static final int SUMMARY_MAX_LENGTH = 254;

    private String REQUEST_TYPE_FIELD_ID = "labels";

    private String PROJECT_CODE;
    private String CONTRIBUTOR_FIELD_ID;
    private String ISBN_FIELD_ID;
    private String OCLC_NUMBER_FIELD_ID;
    private String CALL_NUMBER_FIELD_ID;
    private String FORMAT_FIELD_ID;
    private String SPEED_FIELD_ID;
    private String DESTINATION_FIELD_ID;
    private String CLIENT_NAME_FIELD_ID;
    private String REQUESTER_USERNAME_FIELD_ID;
    private String REQUESTER_INFO_FIELD_ID;
    private String FUND_CODE_FIELD_ID;
    private String OBJECT_CODE_FIELD_ID;
    private String POST_PURCHASE_ID_FIELD_ID;
    private String DECISION_REASON_FIELD_ID;
    private String DEFERRED_STATUS_NAME;
    private Integer DEFERRED_STATUS_TRANSITION_ID;
    private Long APPROVED_STATUS_ID;
    private String APPROVED_STATUS_NAME;
    private Integer APPROVED_STATUS_TRANSITION_ID;
    private List<Long> DENIED_STATUS_ID;
    private Long ARRIVED_STATUS_ID;
    private Integer MAX_SEARCH_RESULTS;
    private String MULTIPLE_LIBRARIANS_USERNAME;
    private String DEFAULT_REPORTER_USERNAME;

    private String TITLE_ISBN_ONLY_PREFIX;

    public JiraWorkflowService(Config config) {
        super();
        this.config = config;
        initMetadata();
        initConnection();
        initUsers();
        log.debug("JiraWorkflowService ready.");
    }

    private void initMetadata() {
        PROJECT_CODE = config.getJira().getProject();
        CONTRIBUTOR_FIELD_ID = config.getJira().getContributorFieldId();
        ISBN_FIELD_ID = config.getJira().getIsbnFieldId();
        OCLC_NUMBER_FIELD_ID = config.getJira().getOclcNumberFieldId();
        CALL_NUMBER_FIELD_ID = config.getJira().getCallNumberFieldId();
        FORMAT_FIELD_ID = config.getJira().getFormatFieldId();
        SPEED_FIELD_ID = config.getJira().getSpeedFieldId();
        DESTINATION_FIELD_ID = config.getJira().getDestinationFieldId();
        CLIENT_NAME_FIELD_ID = config.getJira().getClientNameFieldId();
        REQUESTER_USERNAME_FIELD_ID = config.getJira().getRequesterUsernameFieldId();
        REQUESTER_INFO_FIELD_ID = config.getJira().getRequesterInfoFieldId();
        FUND_CODE_FIELD_ID = config.getJira().getFundCodeFieldId();
        OBJECT_CODE_FIELD_ID = config.getJira().getObjectCodeFieldId();
        POST_PURCHASE_ID_FIELD_ID = config.getJira().getPostPurchaseIdFieldId();
        DECISION_REASON_FIELD_ID = config.getJira().getDecisionReasonFieldId();
        DEFERRED_STATUS_NAME = config.getJira().getDeferredStatusName();
        DEFERRED_STATUS_TRANSITION_ID = config.getJira().getDeferredStatusTransitionId();
        APPROVED_STATUS_ID = config.getJira().getApprovedStatusId();
        APPROVED_STATUS_NAME = config.getJira().getApprovedStatusName();
        APPROVED_STATUS_TRANSITION_ID = config.getJira().getApprovedStatusTransitionId();
        DENIED_STATUS_ID = config.getJira().getDeniedStatusId();
        ARRIVED_STATUS_ID = config.getJira().getArrivedStatusId();
        MAX_SEARCH_RESULTS = config.getJira().getMaxSearchResults();
        MULTIPLE_LIBRARIANS_USERNAME = config.getJira().getMultipleLibrariansUsername();
        DEFAULT_REPORTER_USERNAME = config.getJira().getDefaultReporterUsername();

        TITLE_ISBN_ONLY_PREFIX = config.getCoreData().getTitle().getIsbnOnlyPrefix();
    }

    private void initConnection() {
        this.client = new JiraConnection(this.config);
    }

    Map<String, String> userEmailToAccountId;

    private void initUsers() {
        userEmailToAccountId = new HashMap<String, String>();
        JsonObject response;
        try {
            response = client.executeGet("user/search/query", Map.of(
                "query", "is assignee of " + PROJECT_CODE + 
                    " or is commenter of " + PROJECT_CODE +
                    " or is reporter of " + PROJECT_CODE
            ));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        JsonArray users = response.get("values").getAsJsonArray();        
        for (JsonElement userElement : users) {
            JsonObject user = userElement.getAsJsonObject();
            userEmailToAccountId.put(
                user.get("emailAddress").getAsString(),
                user.get("accountId").getAsString()
            );
        }
    }

    @Override
    public List<PurchaseRequest> findAll() {
        String jql = "project=" + PROJECT_CODE;
        return searchJql(jql);
    }

    @Override
    public PurchaseRequest findByKey(String key) {
        JsonObject issue = getByKey(key);
        if (issue == null) {
            return null;
        }
        return toPurchaseRequest(issue);
    }

    private JsonObject getByKey(String key) {
        try {
            return client.executeGet("issue/" + key);
        }
        catch (Exception ex) {
            log.error("Error in getByKey", ex);
            return null;
        }
    }

    @Override
    public String getWebUrl(PurchaseRequest purchaseRequest) {
        return config.getJira().getUrl() + "browse/" + purchaseRequest.getKey();
    }

    @Override
    public PurchaseRequest save(PurchaseRequest purchaseRequest) {
        JsonObject issue = new JsonObject();
        JsonObject fields = new JsonObject();
        fields.add("project", createStringObject("key", PROJECT_CODE));
        fields.add("issuetype", createStringObject("id", Long.toString(config.getJira().getIssueTypeId())));
        setSummary(fields, purchaseRequest);
   
        // Set basic fields
        fields.addProperty(CONTRIBUTOR_FIELD_ID, purchaseRequest.getContributor());
        fields.addProperty(ISBN_FIELD_ID, purchaseRequest.getIsbn());
        fields.addProperty(OCLC_NUMBER_FIELD_ID, purchaseRequest.getOclcNumber());
        fields.addProperty(CALL_NUMBER_FIELD_ID, purchaseRequest.getCallNumber());
        fields.addProperty(FORMAT_FIELD_ID, purchaseRequest.getFormat());
        fields.addProperty(SPEED_FIELD_ID, purchaseRequest.getSpeed());
        fields.addProperty(DESTINATION_FIELD_ID, purchaseRequest.getDestination());
        fields.addProperty(CLIENT_NAME_FIELD_ID, purchaseRequest.getClientName());
        fields.addProperty(REQUESTER_USERNAME_FIELD_ID, purchaseRequest.getRequesterUsername());
        fields.addProperty(REQUESTER_INFO_FIELD_ID, purchaseRequest.getRequesterInfo());

        // Set conditional fields
        setAssignee(fields, purchaseRequest);
        setReporter(fields, purchaseRequest);
        setRequestType(fields, purchaseRequest);
        if (purchaseRequest.getRequesterComments() != null) {            
            fields.addProperty("description", purchaseRequest.getRequesterComments());  
        }
        issue.add("fields", fields);

        // Save stub issue
        JsonObject response;
        try {
            response = client.executePost("issue", issue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String key = response.get("key").getAsString();
        JsonObject createdIssue = getByKey(key);
        PurchaseRequest createdRequest = toPurchaseRequest(createdIssue);

        // Set status if appropriate
        if (purchaseRequest.getStatus() != null) {
            setInitialStatus(purchaseRequest);
            createdRequest = findByKey(key);
        }

        return createdRequest;
    }

    private void setSummary(JsonObject fields, PurchaseRequest purchaseRequest) {
        setSummary(fields, purchaseRequest.getTitle(), purchaseRequest.getIsbn());
    }

    private void setSummary(JsonObject fields, String title, String isbn) {
        if (title != null) {
            if (title.length() > SUMMARY_MAX_LENGTH) {
                log.info("Truncating title to " + SUMMARY_MAX_LENGTH + " characters: " + title);
                title = title.substring(0, SUMMARY_MAX_LENGTH);
            }
            fields.addProperty("summary", title);
        }
        else {
            fields.addProperty("summary", TITLE_ISBN_ONLY_PREFIX + isbn);
        }
    }

    private void setAssignee(JsonObject fields, PurchaseRequest purchaseRequest) {
        String assigneeName = purchaseRequest.getLibrarianUsername();
        if (assigneeName == null) {
            return;
        }

        String assigneeId = userEmailToAccountId.get(usernameToEmail(assigneeName));
        if (assigneeId == null) {
            log.error("Setting assignee fails, no user exists: " + assigneeName);
            return;
        }
        fields.add("reporter", createStringObject("id", assigneeId));
    }

    private void setReporter(JsonObject fields, PurchaseRequest purchaseRequest) {
        String reporterName = purchaseRequest.getReporterName();
        if (reporterName == null) {
            if (DEFAULT_REPORTER_USERNAME != null) {
                log.debug("Using default reporter: " + DEFAULT_REPORTER_USERNAME);
                reporterName = DEFAULT_REPORTER_USERNAME;
            }
            else {
                log.error("Cannot set reporter, null");
                return;
            }
        }

        String reporterId = userEmailToAccountId.get(usernameToEmail(reporterName));
        if (reporterId == null) {
            log.warn("Unknown reporter, so using default reporter: " + DEFAULT_REPORTER_USERNAME);
            reporterId = userEmailToAccountId.get(usernameToEmail(DEFAULT_REPORTER_USERNAME));
        }

        fields.add("reporter", createStringObject("id", reporterId));
    }

    private String usernameToEmail(String username) {
        if (username.contains("@")) {
            return username;
        }
        return username + "@" + config.getEmail().getAddressDomain();
    }

    private void setRequestType(JsonObject issue, PurchaseRequest purchaseRequest) {
        String requestType = purchaseRequest.getRequestType();
        if (requestType == null) {
            return;
        }

        JsonArray labels = new JsonArray();
        labels.add(requestType);
        issue.add(REQUEST_TYPE_FIELD_ID, labels);
    }

    private void setInitialStatus(PurchaseRequest purchaseRequest) {
        Integer transitionId;
        if (DEFERRED_STATUS_NAME != null && DEFERRED_STATUS_NAME.equals(purchaseRequest.getStatus())) {
            transitionId = DEFERRED_STATUS_TRANSITION_ID;
        }
        else if (APPROVED_STATUS_NAME.equals(purchaseRequest.getStatus())) {
            transitionId = APPROVED_STATUS_TRANSITION_ID;
        }
        else {
            return;
        }

        JsonObject transition = createObject("transition", createStringObject("id", Integer.toString(transitionId)));
        log.info("Setting initial status to " + purchaseRequest.getStatus());
        try {
            client.executePost("issue/" + purchaseRequest.getKey() + "transitions", transition);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject createObject(String key, JsonElement value) {
        JsonObject obj = new JsonObject();
        obj.add(key, value);
        return obj;
    }

    private JsonObject createStringObject(String key, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty(key, value);
        return obj;
    }

    @Override
    public List<PurchaseRequest> search(SearchQuery query) {
        String jql = "project=" + PROJECT_CODE;
        if (query.getIsbn() != null) {
            jql += " and " + "isbn ~ '" + query.getIsbn() + "' ";
        }
        if (query.getReporterName() != null) {
            String reporterEmail = usernameToEmail(query.getReporterName());
            jql += " and " + "reporter" + "='" + reporterEmail + "' ";
        }
        jql += "order by created DESC";
        log.debug("jql: " + jql);
        return searchJql(jql);
    }

    private List<PurchaseRequest> searchJql(String jql) {
        try {
            JsonObject result = client.executeGet("search", Map.of(
                "jql", jql,
                "maxResults", MAX_SEARCH_RESULTS.toString()
            ));
            List<PurchaseRequest> list = new LinkedList<PurchaseRequest>();
            result.getAsJsonArray("issues").forEach((issue) -> {
                PurchaseRequest purchaseRequest = toPurchaseRequest(issue.getAsJsonObject());
                list.add(purchaseRequest);
            });
            return list;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void enrich(PurchaseRequest purchaseRequest, EnrichmentType type, Object data) {
        if (EnrichmentType.LOCAL_HOLDINGS == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.OCLC_NUMBER == type) {
            enrichOclcNumber(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.CALL_NUMBER == type) {
            enrichCallNumber(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.PRICING == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.REQUESTER_INFO == type) {
            enrichRequesterInfo(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.LIBRARIANS == type) {
            enrichAssignee(purchaseRequest, data);
        }
        else if (EnrichmentType.FUND_CODE == type) {
            enrichFundCode(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.OBJECT_CODE == type) {
            enrichObjectCode(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.LINKS == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.PRIORITY == type) {
            enrichPriority(purchaseRequest, (Long)data);
        }
        else if (EnrichmentType.TITLE == type) {
            enrichTitle(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.CONTRIBUTOR == type) {
            enrichContributor(purchaseRequest, (String)data);
        }
        else {
            throw new IllegalArgumentException("Unknown enrichment type " + type);
        }
    }

    private void enrichComment(PurchaseRequest purchaseRequest, String message) {
        JsonObject comment = new JsonObject();
        comment.addProperty("body", message);
        try {
            client.executePost("issue/" + purchaseRequest.getKey() + "/comment", comment, 2);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.debug("Added comment");
    }

    private void enrichOclcNumber(PurchaseRequest purchaseRequest, String oclcNumber) {
        enrichField(purchaseRequest, OCLC_NUMBER_FIELD_ID, oclcNumber);
    }

    private void enrichCallNumber(PurchaseRequest purchaseRequest, String callNumber) {
        enrichField(purchaseRequest, CALL_NUMBER_FIELD_ID, callNumber);
    }

    private void enrichRequesterInfo(PurchaseRequest purchaseRequest, String requesterInfo) {
        enrichField(purchaseRequest, REQUESTER_INFO_FIELD_ID, requesterInfo);
    }

    @SuppressWarnings("unchecked")
    private void enrichAssignee(PurchaseRequest purchaseRequest, Object usernames) {
        enrichAssignee(purchaseRequest, (List<String>)usernames);
    }

    private void enrichAssignee(PurchaseRequest purchaseRequest, List<String> usernames) {
        String username;
        if (usernames.size() == 1) {
            username = usernames.get(0);
        }
        else {
            log.debug("Enriching with " + MULTIPLE_LIBRARIANS_USERNAME + " due to multiple librarians");
            username = MULTIPLE_LIBRARIANS_USERNAME;
        }

        String userId = userEmailToAccountId.get(usernameToEmail(username));
        if (userId == null) {
            log.error("Enriching assignee fails, no user exists: " + username);
            return;
        }

        enrichField(purchaseRequest, "assignee", createStringObject("id", userId));
    }

    private void enrichFundCode(PurchaseRequest purchaseRequest, String fundCode) {
        enrichField(purchaseRequest, FUND_CODE_FIELD_ID, fundCode);
    }

    private void enrichObjectCode(PurchaseRequest purchaseRequest, String objectCode) {
        enrichField(purchaseRequest, OBJECT_CODE_FIELD_ID, objectCode);
    }

    private void enrichPriority(PurchaseRequest purchaseRequest, Long priority) {
        enrichField(purchaseRequest, "priority", createStringObject("id", Long.toString(priority)));
    }

    private void enrichTitle(PurchaseRequest purchaseRequest, String title) {
        JsonObject issueChanges = new JsonObject();
        JsonObject fields = new JsonObject();
        setSummary(fields, title, null);
        issueChanges.add("fields", fields);
        updateIssue(purchaseRequest, issueChanges);
    }

    private void enrichContributor(PurchaseRequest purchaseRequest, String contributor) {
        enrichField(purchaseRequest, CONTRIBUTOR_FIELD_ID, contributor);
    }

    @Override
    public PurchaseRequest addComment(PurchaseRequest purchaseRequest, PurchaseRequest.Comment comment) {
        enrichComment(purchaseRequest, comment.getText());

        JsonObject issue = getByKey(purchaseRequest.getKey());
        PurchaseRequest updatedRequest = toPurchaseRequest(issue);
        return updatedRequest;
    }

    private void enrichField(PurchaseRequest purchaseRequest, String fieldName, JsonElement value) {
        JsonObject issueChanges = new JsonObject();
        JsonObject fields = new JsonObject();
        fields.add(fieldName, value);
        issueChanges.add("fields", fields);
        updateIssue(purchaseRequest, issueChanges);
    }

    private void enrichField(PurchaseRequest purchaseRequest, String fieldName, String value) {
        JsonObject issueChanges = new JsonObject();
        JsonObject fields = new JsonObject();
        fields.addProperty(fieldName, value);
        issueChanges.add("fields", fields);
        updateIssue(purchaseRequest, issueChanges);
    }

    private void updateIssue(PurchaseRequest purchaseRequest, JsonObject issueChanges) {
        try {
            client.executePut("issue/" + purchaseRequest.getKey(), issueChanges);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PurchaseRequest toPurchaseRequest(JsonObject issue) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setKey(issue.get("key").getAsString());
        purchaseRequest.setId(issue.get("id").getAsLong());
        purchaseRequest.setStatus(getIssueStatusName(issue));
        purchaseRequest.setTitle(getStringValue(issue, "summary"));
        purchaseRequest.setContributor(getStringValue(issue, CONTRIBUTOR_FIELD_ID));
        purchaseRequest.setIsbn(getStringValue(issue, ISBN_FIELD_ID));
        purchaseRequest.setOclcNumber(getStringValue(issue, OCLC_NUMBER_FIELD_ID));
        purchaseRequest.setCallNumber(getStringValue(issue, CALL_NUMBER_FIELD_ID));
        purchaseRequest.setFormat(getStringValue(issue, FORMAT_FIELD_ID));
        purchaseRequest.setSpeed(getStringValue(issue, SPEED_FIELD_ID));
        purchaseRequest.setDestination(getStringValue(issue, DESTINATION_FIELD_ID));
        purchaseRequest.setClientName(getStringValue(issue, CLIENT_NAME_FIELD_ID));
        purchaseRequest.setRequestType(getIssueRequestType(issue));
        purchaseRequest.setRequesterUsername(getStringValue(issue, REQUESTER_USERNAME_FIELD_ID));
        purchaseRequest.setRequesterInfo(getStringValue(issue, REQUESTER_INFO_FIELD_ID));
        purchaseRequest.setRequesterComments(getIssueRequesterComments(issue));
        purchaseRequest.setLibrarianUsername(getIssueAssignee(issue));
        purchaseRequest.setFundCode(getStringValue(issue, FUND_CODE_FIELD_ID));
        purchaseRequest.setObjectCode(getStringValue(issue, OBJECT_CODE_FIELD_ID));
        purchaseRequest.setPostRequestComments(getIssueComments(issue));
        purchaseRequest.setPostPurchaseId(getStringValue(issue, POST_PURCHASE_ID_FIELD_ID));
        purchaseRequest.setDecisionReason(getStringValue(issue, DECISION_REASON_FIELD_ID));
        purchaseRequest.setCreationDate(getStringValue(issue, "created"));
        purchaseRequest.setUpdateDate(getStringValue(issue, "updated"));
        return purchaseRequest;
    }

    private String getStringValue(JsonObject issue, String fieldName) {
        JsonObject fields = issue.get("fields").getAsJsonObject();
        JsonElement value = fields.get(fieldName);
        if (value.isJsonNull()) {
            return null;
        }
        else if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        else if (value.isJsonObject()) {
            // Jira drop-down field values are JSONObjects with a 'value' String within
            return value.getAsJsonObject().get("value").getAsString();
        }
        else {
            throw new RuntimeException("What kind of value is this? " + value);
        }
    }

    private String getIssueStatusName(JsonObject issue) {
        JsonObject status = issue.getAsJsonObject("fields")
            .getAsJsonObject("status");
        return status.get("name").getAsString();
    }

    private Long getIssueStatusId(JsonObject issue) {
        JsonObject status = issue.getAsJsonObject("fields")
            .getAsJsonObject("status");
        return status.get("id").getAsLong();
    }

    private String getIssueRequestType(JsonObject issue) {
        JsonObject fields = issue.get("fields").getAsJsonObject();
        JsonArray labels = fields.getAsJsonArray("labels");
        if (labels.size() == 0) {
            return null;
        }

        return labels.get(0).getAsString();
    }

    private String getIssueRequesterComments(JsonObject issue) {
        JsonElement descriptionElement = issue.get("fields").getAsJsonObject()
            .get("description");
        if (descriptionElement.isJsonNull()) return null;
        JsonObject description = descriptionElement.getAsJsonObject();
        return getAtlassianDocumentText(description);
    }

    private String getIssueAssignee(JsonObject issue) {
        try {
            JsonElement assigneeElement = issue.get("fields").getAsJsonObject()
                .get("assignee");
            if (assigneeElement.isJsonNull()) {
                return null;
            }
            return assigneeElement.getAsJsonObject()
                .get("emailAddress").getAsString();
        }
        catch (IllegalStateException e) {
            log.debug("IllegalStateException in getIssueAssignee", e);
            return null;
        }
    }

    private List<PurchaseRequest.Comment> getIssueComments(JsonObject issue) {
        List<PurchaseRequest.Comment> prComments = new LinkedList<PurchaseRequest.Comment>();
        JsonObject response;
        try {
            response = client.executeGet("issue/" + issue.get("key").getAsString() + "/comment");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        JsonArray comments = response.get("comments").getAsJsonArray();
        for (JsonElement commentElement : comments) {
            JsonObject comment = commentElement.getAsJsonObject();
            PurchaseRequest.Comment prComment = new PurchaseRequest.Comment();
            prComment.setText(getAtlassianDocumentText(comment.get("body").getAsJsonObject()));
            prComment.setCreationDate(comment.get("created").getAsString());
            prComments.add(prComment);
        }
        return prComments;
    }

    private String getAtlassianDocumentText(JsonObject body) {
        JsonArray commentLines = body.get("content").getAsJsonArray()
            .get(0).getAsJsonObject()
            .get("content").getAsJsonArray();
        StringBuffer comments = new StringBuffer();
        for (JsonElement commentElement : commentLines) {
            JsonElement commentLineText = commentElement.getAsJsonObject()
                .get("text");
            if (commentLineText != null) {
                comments.append(commentLineText.getAsString()).append("\n");
            }
        }
        return comments.toString();
    }

    void purchaseRequestUpdated(String key) {
        JsonObject issue = getByKey(key);
        if (issue == null) {
            log.warn("Got purchase updated message for unknown key: " + key);
            return;
        }
        
        PurchaseRequest purchaseRequest = toPurchaseRequest(issue);
        Long statusId = getIssueStatusId(issue);
        if (APPROVED_STATUS_ID.equals(statusId)) {
            notifyPurchaseRequestApproved(purchaseRequest);
        }
        else if (DENIED_STATUS_ID.contains(statusId)) {
            notifyPurchaseRequestDenied(purchaseRequest);
        }
        else if (ARRIVED_STATUS_ID.equals(statusId)) {
            notifyPurchaseRequestArrived(purchaseRequest);
        }
        else {
            log.warn("Ignoring purchase request updated with unhandled status: " + statusId);
        }
    }
    
}
