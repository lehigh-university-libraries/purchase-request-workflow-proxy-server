package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix="workflow")
@EnableAsync
@Getter @Setter
public class Config {
    
    private Jira jira;

    @Getter @Setter
    public static class Jira {

        private String url;        
        private String username;
        private String token;
        private Long issueTypeId;
        private Long approvedStatusId;
        private String contributorFieldId;

    }
}


