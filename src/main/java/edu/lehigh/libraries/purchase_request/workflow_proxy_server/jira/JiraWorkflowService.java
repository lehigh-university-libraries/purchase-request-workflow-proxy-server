package edu.lehigh.libraries.purchase_request.workflow_proxy_server.jira;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.httpclient.api.HttpStatus;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JiraWorkflowService implements WorkflowService {

    private JiraRestClient client;
    private Config config;

    private String CONTRIBUTOR_FIELD_ID;
    private String ISBN_FIELD_ID;
    private String OCLC_NUMBER_FIELD_ID;
    private String FORMAT_FIELD_ID;
    private String SPEED_FIELD_ID;
    private String DESTINATION_FIELD_ID;
    private String CLIENT_NAME_FIELD_ID;
    private String REPORTER_NAME_FIELD_ID;
    private String REQUESTER_USERNAME_FIELD_ID;
    private String REQUESTER_ROLE_FIELD_ID;
    private Long APPROVED_STATUS_ID;

    private List<WorkflowServiceListener> listeners;

    public JiraWorkflowService(Config config) {
        this.config = config;
        initMetadata();
        initConnection();

        listeners = new LinkedList<WorkflowServiceListener>();
    }

    private void initMetadata() {
        CONTRIBUTOR_FIELD_ID = config.getJira().getContributorFieldId();
        ISBN_FIELD_ID = config.getJira().getIsbnFieldId();
        OCLC_NUMBER_FIELD_ID = config.getJira().getOclcNumberFieldId();
        FORMAT_FIELD_ID = config.getJira().getFormatFieldId();
        SPEED_FIELD_ID = config.getJira().getSpeedFieldId();
        DESTINATION_FIELD_ID = config.getJira().getDestinationFieldId();
        CLIENT_NAME_FIELD_ID = config.getJira().getClientNameFieldId();
        REPORTER_NAME_FIELD_ID = config.getJira().getReporterNameFieldId();
        REQUESTER_USERNAME_FIELD_ID = config.getJira().getRequesterUsernameFieldId();
        REQUESTER_ROLE_FIELD_ID = config.getJira().getRequesterRoleFieldId();
        APPROVED_STATUS_ID = config.getJira().getApprovedStatusId();
    }

    private void initConnection() {
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

    @Override
    public List<PurchaseRequest> findAll() {
        String jql = "project=PR";
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
        IssueInputBuilder issueBuilder = new IssueInputBuilder("PR", config.getJira().getIssueTypeId());        
        issueBuilder.setSummary(purchaseRequest.getTitle());
        issueBuilder.setFieldValue(CONTRIBUTOR_FIELD_ID, purchaseRequest.getContributor());
        issueBuilder.setFieldValue(ISBN_FIELD_ID, purchaseRequest.getIsbn());
        issueBuilder.setFieldValue(OCLC_NUMBER_FIELD_ID, purchaseRequest.getOclcNumber());
        issueBuilder.setFieldValue(FORMAT_FIELD_ID, purchaseRequest.getFormat());
        issueBuilder.setFieldValue(SPEED_FIELD_ID, purchaseRequest.getSpeed());
        issueBuilder.setFieldValue(DESTINATION_FIELD_ID, purchaseRequest.getDestination());
        issueBuilder.setFieldValue(CLIENT_NAME_FIELD_ID, purchaseRequest.getClientName());
        issueBuilder.setFieldValue(REQUESTER_USERNAME_FIELD_ID, purchaseRequest.getRequesterUsername());
        issueBuilder.setFieldValue(REQUESTER_ROLE_FIELD_ID, purchaseRequest.getRequesterRole());
        setReporter(issueBuilder, purchaseRequest);
        String key = client.getIssueClient().createIssue(issueBuilder.build()).claim().getKey();

        PurchaseRequest createdRequest = findByKey(key);
        for (WorkflowServiceListener listener : listeners) {
            listener.purchaseRequested(createdRequest);
        }

        return createdRequest;
    }

    private void setReporter(IssueInputBuilder issueBuilder, PurchaseRequest purchaseRequest) {
        if (REPORTER_NAME_FIELD_ID == null) {
            // Using the built-in setReporterName() should work with Jira Server, just not Jira Cloud.
            // https://community.atlassian.com/t5/Jira-questions/Create-an-issue-with-rest-api-set-reporter-name/qaq-p/1535911
            issueBuilder.setReporterName(purchaseRequest.getReporterName());
        }
        else {
            issueBuilder.setFieldValue(REPORTER_NAME_FIELD_ID, purchaseRequest.getReporterName());
        }
    }

    @Override
    public List<PurchaseRequest> search(SearchQuery query) {
        String jql = "project=PR and " +
            formatCustomFieldIdForQuery(REPORTER_NAME_FIELD_ID) + " ~ '" + query.getReporterName() + "' " +
            "order by created DESC";
        log.debug("jql: " + jql);
        return searchJql(jql);
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
        SearchResult result = client.getSearchClient().searchJql(jql).claim();
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
        else if (EnrichmentType.PRICING == type) {
            enrichComment(purchaseRequest, (String)data);
        }
        else if (EnrichmentType.REQUESTER_ROLE == type) {
            enrichRequesterType(purchaseRequest, (String)data);
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

    private void enrichRequesterType(PurchaseRequest purchaseRequest, String requesterType) {
        IssueInput input = new IssueInputBuilder()
            .setFieldValue(REQUESTER_ROLE_FIELD_ID, requesterType)
            .build();
        client.getIssueClient().updateIssue(purchaseRequest.getKey(), input).claim();
    }

    @Override
    public void addListener(WorkflowServiceListener listener) {
        listeners.add(listener);
    }

    private PurchaseRequest toPurchaseRequest(Issue issue) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setKey(issue.getKey());
        purchaseRequest.setId(issue.getId());
        purchaseRequest.setTitle(issue.getSummary());
        purchaseRequest.setContributor((String)issue.getField(CONTRIBUTOR_FIELD_ID).getValue());
        purchaseRequest.setIsbn((String)issue.getField(ISBN_FIELD_ID).getValue());
        purchaseRequest.setOclcNumber((String)issue.getField(OCLC_NUMBER_FIELD_ID).getValue());
        purchaseRequest.setFormat((String)issue.getField(FORMAT_FIELD_ID).getValue());
        purchaseRequest.setSpeed((String)issue.getField(SPEED_FIELD_ID).getValue());
        purchaseRequest.setDestination((String)issue.getField(DESTINATION_FIELD_ID).getValue());
        purchaseRequest.setClientName((String)issue.getField(CLIENT_NAME_FIELD_ID).getValue());
        purchaseRequest.setRequesterUsername((String)issue.getField(REQUESTER_USERNAME_FIELD_ID).getValue());
        purchaseRequest.setRequesterRole((String)issue.getField(REQUESTER_ROLE_FIELD_ID).getValue());
        purchaseRequest.setCreationDate(formatDateTime(issue.getCreationDate()));
        return purchaseRequest;
    }

    private String formatDateTime(DateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss aa");
        return fmt.print(dateTime);
    }

    void confirmPurchaseApproved(String key) {
        Issue issue = getByKey(key);
        if (issue == null) {
            log.warn("Got purchase approved message for unknown key: " + key);
        }
        else if (APPROVED_STATUS_ID.equals(issue.getStatus().getId())) {
            issue.getChangelog();
            // TODO Confirm that the status was just set in the last minute or so, to verify.
            PurchaseRequest purchaseRequest = toPurchaseRequest(issue);
            for (WorkflowServiceListener listener : listeners) {
                listener.purchaseApproved(purchaseRequest);
            }
        }
        else {
            log.warn("Ignoring purchase approval with wrong status: " + issue.getStatus().getId());
        }
    }
    
}
