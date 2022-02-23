package edu.lehigh.libraries.purchase_request.workflow_proxy_server.jira;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class JiraResponseController {

    @Autowired
    private JiraWorkflowService service;

    @PostMapping("/purchase-requests/status/{issueKey}")
    void statusChanged(@PathVariable String issueKey, @RequestBody String body) {
        log.debug("Received message from Jira: " + body);
        confirmAndReportStatus(issueKey);
    }

    @Async
    void confirmAndReportStatus(String issueKey) {
        service.confirmPurchaseApproved(issueKey);
    }
    
}
