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
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JiraWorkflowService implements WorkflowService {

    private JiraRestClient client;
    private Config config;

    private String CONTRIBUTOR_FIELD_ID;

    public JiraWorkflowService(Config config) {
        this.config = config;
        initMetadata();
        initConnection();
    }

    private void initMetadata() {
        CONTRIBUTOR_FIELD_ID = config.getJira().getContributorFieldId();
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
        SearchResult result = client.getSearchClient().searchJql(
            "project=PR"
        )
        .claim();
        List<PurchaseRequest> list = new LinkedList<PurchaseRequest>();
        result.getIssues().forEach((issue) -> {
            PurchaseRequest purchaseRequest = toPurchaseRequest(issue);
            log.debug("Found purchase request: " + purchaseRequest);
            list.add(purchaseRequest);
        });
        return list;
    }

    @Override
    public PurchaseRequest findByKey(String key) {
        try {
            Issue issue = client.getIssueClient().getIssue(key).claim();
            return toPurchaseRequest(issue);
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
        String key = client.getIssueClient().createIssue(issueBuilder.build()).claim().getKey();

        PurchaseRequest createdRequest = findByKey(key);
        return createdRequest;
    }

    private PurchaseRequest toPurchaseRequest(Issue issue) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setKey(issue.getKey());
        purchaseRequest.setId(issue.getId());
        purchaseRequest.setTitle(issue.getSummary());
        purchaseRequest.setContributor((String)issue.getField(CONTRIBUTOR_FIELD_ID).getValue());
        return purchaseRequest;
    }
    
}
