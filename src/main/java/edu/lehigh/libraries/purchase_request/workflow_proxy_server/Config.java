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
    private Database db;
    private GoogleSheets googleSheets;

    @Getter @Setter
    public static class Jira {

        /**
         * URL to Jira, i.e. https://mydomain.atlassian.net/
         */
        private String url;        

        /**
         * Jira username
         */
        private String username;

        /**
         * Jira API token.  
         * 
         * See https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/
         */
        private String token;

        /**
         * ID of the Jira issue type that should be created for purchase requests.
         * 
         * A list of types with IDs can be retrieved with {{jira.url}}/issuetype
         */
        private Long issueTypeId;

        /**
         * ID of the Jira status that indicates a purchase request has been approved.
         * 
         * A list of statuses with IDs can be retrieved with {{jira.url}}/status
         */
        private Long approvedStatusId;

        /**
         * ID of the Jira custom field representing the item's contributor (author etc.).
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String contributorFieldId;

        /**
         * ID of the Jira custom field representing the item's ISBN.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String isbnFieldId;

    }

    @Getter @Setter
    /**
     * Connection properties for database used for workflow proxy metadata.
     * 
     * This database is not used for the actual workflow items backed by a WorkflowService.
     */
    public static class Database {

        /**
         * Database hostname
         */
        private String host;

        /**
         * Database name
         */
        private String name;

        /**
         * Database username
         */
        private String username;

        /**
         * Database password
         */
        private String password;

    }

    @Getter @Setter
    public static class GoogleSheets {

        /**
         * ID of the Google Sheets spreadsheet on which to write the ISBNs.
         */
        private String spreadsheetId;

        /**
         * Path to the google-sheets-client-secret.json file used to connect to the API.
         */
        private String credentialsFilePath;
        
        /**
         * Header text for the column of ISBNs.
         */
        private String isbnColumnHeader;
    }

}


