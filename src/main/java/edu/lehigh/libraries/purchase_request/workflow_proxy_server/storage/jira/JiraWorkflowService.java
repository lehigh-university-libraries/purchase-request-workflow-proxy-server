package edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.jira;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.httpclient.api.HttpStatus;
import com.atlassian.httpclient.api.Request.Builder;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.AbstractWorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name="workflow.storage", havingValue="jira")
@ConditionalOnWebApplication
@Slf4j
public class JiraWorkflowService extends AbstractWorkflowService {

    private JiraRestClient client;
    private Config config;

    private static final String 
        HOSTING_CLOUD = "cloud",
        HOSTING_SERVER = "server";
    private static final int SUMMARY_MAX_LENGTH = 254;

    private String HOSTING;
    private String PROJECT_CODE;
    private String CONTRIBUTOR_FIELD_ID;
    private String ISBN_FIELD_ID;
    private String OCLC_NUMBER_FIELD_ID;
    private String CALL_NUMBER_FIELD_ID;
    private String FORMAT_FIELD_ID;
    private String SPEED_FIELD_ID;
    private String DESTINATION_FIELD_ID;
    private String CLIENT_NAME_FIELD_ID;
    private String REPORTER_NAME_FIELD_ID;
    private String REQUESTER_USERNAME_FIELD_ID;
    private String REQUESTER_ROLE_FIELD_ID;
    private String FUND_CODE_FIELD_ID;
    private String OBJECT_CODE_FIELD_ID;
    private String DEFERRED_STATUS_NAME;
    private Integer DEFERRED_STATUS_TRANSITION_ID;
    private Long APPROVED_STATUS_ID;
    private String APPROVED_STATUS_NAME;
    private Integer APPROVED_STATUS_TRANSITION_ID;
    private Long DENIED_STATUS_ID;
    private Long ARRIVED_STATUS_ID;
    private Integer MAX_SEARCH_RESULTS;
    private String MULTIPLE_LIBRARIANS_USERNAME;

    public JiraWorkflowService(Config config) {
        super();
        this.config = config;
        initMetadata();
        initConnection();
        log.debug("JiraWorkflowService ready.");
    }

    private void initMetadata() {
        HOSTING = config.getJira().getHosting();
        PROJECT_CODE = config.getJira().getProject();
        CONTRIBUTOR_FIELD_ID = config.getJira().getContributorFieldId();
        ISBN_FIELD_ID = config.getJira().getIsbnFieldId();
        OCLC_NUMBER_FIELD_ID = config.getJira().getOclcNumberFieldId();
        CALL_NUMBER_FIELD_ID = config.getJira().getCallNumberFieldId();
        FORMAT_FIELD_ID = config.getJira().getFormatFieldId();
        SPEED_FIELD_ID = config.getJira().getSpeedFieldId();
        DESTINATION_FIELD_ID = config.getJira().getDestinationFieldId();
        CLIENT_NAME_FIELD_ID = config.getJira().getClientNameFieldId();
        REPORTER_NAME_FIELD_ID = config.getJira().getReporterNameFieldId();
        REQUESTER_USERNAME_FIELD_ID = config.getJira().getRequesterUsernameFieldId();
        REQUESTER_ROLE_FIELD_ID = config.getJira().getRequesterRoleFieldId();
        FUND_CODE_FIELD_ID = config.getJira().getFundCodeFieldId();
        OBJECT_CODE_FIELD_ID = config.getJira().getObjectCodeFieldId();
        DEFERRED_STATUS_NAME = config.getJira().getDeferredStatusName();
        DEFERRED_STATUS_TRANSITION_ID = config.getJira().getDeferredStatusTransitionId();
        APPROVED_STATUS_ID = config.getJira().getApprovedStatusId();
        APPROVED_STATUS_NAME = config.getJira().getApprovedStatusName();
        APPROVED_STATUS_TRANSITION_ID = config.getJira().getApprovedStatusTransitionId();
        DENIED_STATUS_ID = config.getJira().getDeniedStatusId();
        ARRIVED_STATUS_ID = config.getJira().getArrivedStatusId();
        MAX_SEARCH_RESULTS = config.getJira().getMaxSearchResults();
        MULTIPLE_LIBRARIANS_USERNAME = config.getJira().getMultipleLibrariansUsername();
    }

    private void initConnection() {
        if (HOSTING_CLOUD.equals(HOSTING)) {
            initCloudConnection();
        }
        else if (HOSTING_SERVER.equals(HOSTING)) {
            initServerConnection();
        }
        else {
            log.error("Unknown hosting environment: " + HOSTING);
        }
    }

    private void initCloudConnection() {
        String url = config.getJira().getUrl();
        String username = config.getJira().getUsername();
        String password = config.getJira().getToken();

        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        try {
            URI uri = new URI(url);
            client = factory.createWithBasicHttpAuthentication(uri, username, password);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initServerConnection() {
        String url = config.getJira().getUrl();
        String token = config.getJira().getToken();

        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        try {
            URI uri = new URI(url);
            client = factory.create(uri, new AuthenticationHandler() {
                public void configure(Builder builder) {
                    builder.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            });
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PurchaseRequest> findAll() {
        String jql = "project=" + PROJECT_CODE;
        return searchJql(jql);
    }

    @Override
    public PurchaseRequest findByKey(String key) {
        Issue issue = getByKey(key);
        return toPurchaseRequest(issue);
    }

    private Issue getByKey(String key) {
        try {
            return client.getIssueClient().getIssue(key).claim();
        }
        catch (RestClientException ex) {
            if (ex.getStatusCode().get() == HttpStatus.NOT_FOUND.code) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public PurchaseRequest save(PurchaseRequest purchaseRequest) {
        IssueInputBuilder issueBuilder = new IssueInputBuilder(PROJECT_CODE, config.getJira().getIssueTypeId());        
        setSummary(issueBuilder, purchaseRequest);
   
        // Set basic fields
        issueBuilder.setFieldValue(CONTRIBUTOR_FIELD_ID, purchaseRequest.getContributor());
        issueBuilder.setFieldValue(ISBN_FIELD_ID, purchaseRequest.getIsbn());
        issueBuilder.setFieldValue(OCLC_NUMBER_FIELD_ID, purchaseRequest.getOclcNumber());
        issueBuilder.setFieldValue(CALL_NUMBER_FIELD_ID, purchaseRequest.getCallNumber());
        issueBuilder.setFieldValue(FORMAT_FIELD_ID, purchaseRequest.getFormat());
        issueBuilder.setFieldValue(SPEED_FIELD_ID, purchaseRequest.getSpeed());
        issueBuilder.setFieldValue(DESTINATION_FIELD_ID, purchaseRequest.getDestination());
        issueBuilder.setFieldValue(CLIENT_NAME_FIELD_ID, purchaseRequest.getClientName());
        issueBuilder.setFieldValue(REQUESTER_USERNAME_FIELD_ID, purchaseRequest.getRequesterUsername());
        issueBuilder.setFieldValue(REQUESTER_ROLE_FIELD_ID, purchaseRequest.getRequesterRole());

        // Set conditional fields
        if (purchaseRequest.getLibrarianUsername() != null && userExists(purchaseRequest.getLibrarianUsername())) {
            issueBuilder.setAssigneeName(purchaseRequest.getLibrarianUsername());
        }
        setReporter(issueBuilder, purchaseRequest);
        if (purchaseRequest.getRequesterComments() != null) {            
            issueBuilder.setDescription("Patron Comment: " + purchaseRequest.getRequesterComments());        
        }

        // Save stub issue
        String key = client.getIssueClient().createIssue(issueBuilder.build()).claim().getKey();
        Issue issue = getByKey(key);
        PurchaseRequest createdRequest = toPurchaseRequest(issue);

        // Set status if appropriate
        if (purchaseRequest.getStatus() != null) {
            setInitialStatus(purchaseRequest, issue.getTransitionsUri());
            createdRequest = findByKey(key);
        }

        notifyPurchaseRequestCreated(createdRequest);

        return createdRequest;
    }

    private void setSummary(IssueInputBuilder issueBuilder, PurchaseRequest purchaseRequest) {
        String title = purchaseRequest.getTitle();
        if (title.length() > SUMMARY_MAX_LENGTH) {
            log.info("Truncating title to " + SUMMARY_MAX_LENGTH + " characters: " + title);
            title = title.substring(0, SUMMARY_MAX_LENGTH);
        }
        issueBuilder.setSummary(title);
    }

    private void setReporter(IssueInputBuilder issueBuilder, PurchaseRequest purchaseRequest) {
        String reporterName = purchaseRequest.getReporterName();
        if (reporterName == null) {
            log.debug("Cannot set reporter, null");
            return;
        }
        
        if (HOSTING_CLOUD.equals(HOSTING)) {
            issueBuilder.setFieldValue(REPORTER_NAME_FIELD_ID, reporterName);
        }
        else if (HOSTING_SERVER.equals(HOSTING)) {
            if (userExists(reporterName)) {
                issueBuilder.setReporterName(reporterName);
            }
            else {
                log.warn("Tried to setReporter on unknown username: " + reporterName);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown hosting environment: " + HOSTING);
        }
    }

    private void setInitialStatus(PurchaseRequest purchaseRequest, URI transitionsUri) {
        TransitionInput transitionInput = null;
        if (DEFERRED_STATUS_NAME != null && DEFERRED_STATUS_NAME.equals(purchaseRequest.getStatus())) {
            transitionInput = new TransitionInput(DEFERRED_STATUS_TRANSITION_ID);
        }
        else if (APPROVED_STATUS_NAME.equals(purchaseRequest.getStatus())) {
            transitionInput = new TransitionInput(APPROVED_STATUS_TRANSITION_ID);
        }
        else {
            return;
        }

        log.info("Setting initial status to " + purchaseRequest.getStatus());
        client.getIssueClient().transition(transitionsUri, transitionInput).claim(); 
    }

    private boolean userExists(String username) {
        try {
            client.getUserClient().getUser(username).claim();
        }
        catch (RestClientException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<PurchaseRequest> search(SearchQuery query) {
        if (HOSTING_CLOUD.equals(HOSTING)) {
            String jql = "project=" + PROJECT_CODE + " and " +
                formatCustomFieldIdForQuery(REPORTER_NAME_FIELD_ID) + " ~ '" + query.getReporterName() + "' " +
                "order by created DESC";
            log.debug("jql: " + jql);
            return searchJql(jql);
        }
        else if (HOSTING_SERVER.equals(HOSTING)) {
            String jql = "project=" + PROJECT_CODE + " and " +
                "reporter = '" + query.getReporterName() + "' " +
                "order by created DESC";
            log.debug("jql: " + jql);
            return searchJql(jql);
        }
        else {
            throw new IllegalArgumentException("Unknown hosting environment: " + HOSTING);
        }
    }

    /**
     * Translate to custom field ID format used in JQL.
     * 
     * i.e. from "customfield_12345" to "cf[12345]"
     */
    private String formatCustomFieldIdForQuery(String customFieldId) {
        return "cf[" + customFieldId.replaceAll("[\\D]*", "") + "]";
    }

    private List<PurchaseRequest> searchJql(String jql) {
        SearchResult result = client.getSearchClient().searchJql(jql, MAX_SEARCH_RESULTS, null, null).claim();
        List<PurchaseRequest> list = new LinkedList<PurchaseRequest>();
        result.getIssues().forEach((issue) -> {
            PurchaseRequest purchaseRequest = toPurchaseRequest(issue);
            log.debug("Found purchase request: " + purchaseRequest);
            list.add(purchaseRequest);
        });
        return list;
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
        else if (EnrichmentType.REQUESTER_ROLE == type) {
            enrichRequesterType(purchaseRequest, (String)data);
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
        else {
            throw new IllegalArgumentException("Unknown enrichment type " + type);
        }
    }

    private void enrichComment(PurchaseRequest purchaseRequest, String message) {
        URI uri;
        String baseUrl = config.getJira().getUrl();
        try {
            uri = new URIBuilder(baseUrl)
                .setPath("rest/api/2/issue/" + purchaseRequest.getId() + "/comment")
                .build();
        }
        catch (URISyntaxException e) {
            log.error("URI Syntax Exception when trying to enrich comment. ", e);
            return;
        }
        Comment comment = Comment.valueOf(message);
        client.getIssueClient().addComment(uri, comment).claim();
        log.debug("Added comment");
    }

    private void enrichOclcNumber(PurchaseRequest purchaseRequest, String oclcNumber) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(OCLC_NUMBER_FIELD_ID, oclcNumber)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    private void enrichCallNumber(PurchaseRequest purchaseRequest, String callNumber) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(CALL_NUMBER_FIELD_ID, callNumber)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    private void enrichRequesterType(PurchaseRequest purchaseRequest, String requesterType) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(REQUESTER_ROLE_FIELD_ID, requesterType)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
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

        if (!userExists(username)) {
            log.error("Enriching assignee fails, no user exists: " + username);
            return;
        }

        IssueInput input = new IssueInputBuilder()
            .setAssigneeName(username)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    private void enrichFundCode(PurchaseRequest purchaseRequest, String fundCode) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(FUND_CODE_FIELD_ID, fundCode)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    private void enrichObjectCode(PurchaseRequest purchaseRequest, String objetCode) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(OBJECT_CODE_FIELD_ID, objetCode)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    private PurchaseRequest toPurchaseRequest(Issue issue) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setKey(issue.getKey());
        purchaseRequest.setId(issue.getId());
        purchaseRequest.setStatus(issue.getStatus().getName());
        purchaseRequest.setTitle(issue.getSummary());
        purchaseRequest.setContributor((String)issue.getField(CONTRIBUTOR_FIELD_ID).getValue());
        purchaseRequest.setIsbn((String)issue.getField(ISBN_FIELD_ID).getValue());
        if (issue.getField(OCLC_NUMBER_FIELD_ID) != null) {
            purchaseRequest.setOclcNumber((String)issue.getField(OCLC_NUMBER_FIELD_ID).getValue());
            purchaseRequest.setCallNumber((String)issue.getField(CALL_NUMBER_FIELD_ID).getValue());
        }
        purchaseRequest.setFormat((String)issue.getField(FORMAT_FIELD_ID).getValue());
        purchaseRequest.setSpeed((String)issue.getField(SPEED_FIELD_ID).getValue());
        purchaseRequest.setDestination((String)issue.getField(DESTINATION_FIELD_ID).getValue());
        purchaseRequest.setClientName((String)issue.getField(CLIENT_NAME_FIELD_ID).getValue());
        purchaseRequest.setRequesterUsername((String)issue.getField(REQUESTER_USERNAME_FIELD_ID).getValue());
        if (issue.getField(REQUESTER_ROLE_FIELD_ID) != null) {
            purchaseRequest.setRequesterRole((String)issue.getField(REQUESTER_ROLE_FIELD_ID).getValue());
        }
        purchaseRequest.setRequesterComments(issue.getDescription());
        purchaseRequest.setLibrarianUsername(issue.getAssignee() != null ? issue.getAssignee().getName() : null);
        if (issue.getField(FUND_CODE_FIELD_ID) != null) {
            purchaseRequest.setFundCode((String)issue.getField(FUND_CODE_FIELD_ID).getValue());
            purchaseRequest.setObjectCode((String)issue.getField(OBJECT_CODE_FIELD_ID).getValue());
        }
        purchaseRequest.setCreationDate(formatDateTime(issue.getCreationDate()));
        purchaseRequest.setUpdateDate(formatDateTime(issue.getUpdateDate()));
        return purchaseRequest;
    }

    private String formatDateTime(DateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss aa");
        return fmt.print(dateTime);
    }

    void purchaseRequestUpdated(String key) {
        Issue issue = getByKey(key);
        if (issue == null) {
            log.warn("Got purchase updated message for unknown key: " + key);
            return;
        }
        
        PurchaseRequest purchaseRequest = toPurchaseRequest(issue);
        if (APPROVED_STATUS_ID.equals(issue.getStatus().getId())) {
            notifyPurchaseRequestApproved(purchaseRequest);
        }
        else if (DENIED_STATUS_ID.equals(issue.getStatus().getId())) {
            notifyPurchaseRequestDenied(purchaseRequest);
        }
        else if (ARRIVED_STATUS_ID.equals(issue.getStatus().getId())) {
            notifyPurchaseRequestArrived(purchaseRequest);
        }
        else {
            log.warn("Ignoring purchase request updated with unhandled status: " + issue.getStatus().getId());
        }
    }
    
}
