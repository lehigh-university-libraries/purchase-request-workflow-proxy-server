package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="workflow")
public class Config {
    
    private Jira jira;

    public Jira getJira() { return jira; }
    public void setJira(Jira jira) { this.jira = jira; }

    public static class Jira {

        private String url;
        private String username;
        private String token;
    
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getUsername() { return username; }   
        public void setUsername(String username) { this.username = username; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}


