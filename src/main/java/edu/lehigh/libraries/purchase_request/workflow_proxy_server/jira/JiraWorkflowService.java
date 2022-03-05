package edu.lehigh.libraries.purchase_request.workflow_proxy_server.jira;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.httpclient.api.HttpStatus;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.SearchQuery;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JiraWorkflowService implements WorkflowService {

    private JiraRestClient client;
    private Config config;

    private String CONTRIBUTOR_FIELD_ID;
    private String ISBN_FIELD_ID;
    private String CLIENT_NAME_FIELD_ID;
    private String REPORTER_NAME_FIELD_ID;
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
        CLIENT_NAME_FIELD_ID = config.getJira().getClientNameFieldId();
        REPORTER_NAME_FIELD_ID = config.getJira().getReporterNameFieldId();
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
        issueBuilder.setFieldValue(CLIENT_NAME_FIELD_ID, purchaseRequest.getClientName());
        setReporter(issueBuilder, purchaseRequest);
        String key = client.getIssueClient().createIssue(issueBuilder.build()).claim().getKey();

        PurchaseRequest createdRequest = findByKey(key);
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
        purchaseRequest.setClientName((String)issue.getField(CLIENT_NAME_FIELD_ID).getValue());
        return purchaseRequest;
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
