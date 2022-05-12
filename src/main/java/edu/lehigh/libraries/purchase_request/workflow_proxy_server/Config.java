package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix="workflow")
@Validated
@EnableAsync
@Getter @Setter
public class Config {

    @AssertTrue
    @NotNull
    private Boolean enabled;
    
    private Jira jira;
    private Database db;
    private Oclc oclc;
    private LocalHoldings localHoldings;
    private GroupHoldings groupHoldings;
    private Folio folio;
    private VuFind vuFind;
    private Ldap ldap;
    private IsbnDb isbnDb;
    private LibrarianCallNumbers librarianCallNumbers;
    private Email email;
    private GoogleSheets googleSheets;

    @Getter @Setter
    public static class Jira {

        /**
         * Hosting environment.  "cloud" or "server".
         */
        private String hosting;

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
         * Jira project code
         */
        private String project;

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

        /**
         * ID of the Jira custom field representing the item's OCLC number.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String oclcNumberFieldId;

        /**
         * ID of the Jira custom field representing the item's Dewey or LC call number.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String callNumberFieldId;

        /**
         * ID of the Jira custom field representing the item's requested format: print, electronic or any.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String formatFieldId;

        /**
         * ID of the Jira custom field representing the item's requested delivery speed.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String speedFieldId;

        /**
         * ID of the Jira custom field representing the item's requested pick-up or delivery destination.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String destinationFieldId;

        /**
         * ID of the Jira custom field representing the client system that submitted a purchase request.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String clientNameFieldId;

        /**
         * ID of the Jira custom field representing the human reporter who submitted a purchase request.
         * 
         * A custom field is only necessary for Jira Cloud, where the native Reporter field only supports
         * people with actual Jira accounts.  For self-hosted Jira, the default Reporter field can be 
         * used and set to any string name.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String reporterNameFieldId;

        /**
         * ID of the Jira custom field representing the username of the person who requested the item.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String requesterUsernameFieldId;

        /**
         * ID of the Jira custom field representing the role (faculty, undergraduate, etc.) of the person 
         * who requested the item.
         * 
         * A list of fields with IDs can be retrieved with {{jira.url}}/field
         */
        private String requesterRoleFieldId;

        /**
         * ID of the Jira custom field representing a budget fund code for this request.
         */
        private String fundCodeFieldId;

        /**
         * ID of the Jira custom field representing a budget object or purpose code for this request.
         */
        private String objectCodeFieldId;

        /**
         * Maximum results to return from a call to /search
         */
        private Integer maxSearchResults;

        /**
         * Username to use as Jira assignee if multiple librarians are relevant to the request.
         * Intended to be a forwarding email address.
         */
        private String multipleLibrariansUsername;

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
    public static class Oclc {

        /**
         * The WSKey, or Oauth2 Client ID (Client Credentials grant type), for OCLC APIs
         */
        private String wsKey;

        /**
         * Oauth2 Client Secret (Client Credentials grant type) for OCLC APIs
         */
        private String secret;

        /**
         * Three-character(?) OCLC institution symbol for local holdings
         */
        private String localInstitutionSymbol;

        /**
         * "dewey" or "lc"
         */
        private String classificationType;
    }

    @Getter @Setter
    public static class LocalHoldings {

        public enum DataSource {
            FOLIO, OCLC;
        }

        public enum LinkDestination {
            VuFind, FOLIO;
        }

        /**
         * DataSource to use for Local Holdings enrichment.  Exclude property to disabled this enrichment.
         */
        private DataSource dataSource;

        /**
         * System to link to in the LocalHoldingsEnrichment text: "VuFind" or "FOLIO"
         */
        private LinkDestination linkTo;

    }

    @Getter @Setter
    public static class GroupHoldings {

        /**
         * List of OCLC symbols for affiliated groups to check for holdings, comma-separated.
         */
        private List<String> oclcSymbols;

    }

    @Getter @Setter
    public static class Folio {

        /**
         * FOLIO API username
         */
        private String username;

        /**
         * FOLIO API password
         */
        private String password;

        /**
         * FOLIO API tenant ID
         */
        private String tenantId;

        /**
         * FOLIO API base OKAPI url
         */
        private String okapiBaseUrl;

        /**
         * FOLIO website base URL
         */
        private String websiteBaseUrl;

    }

    @Getter @Setter
    public static class VuFind {

        /**
         * Base URL of VuFind instance.
         */
        private String baseUrl;

    }

    @Getter @Setter
    public static class Ldap {

        /**
         * LDAP field to query with a given username.
         */
        private String usernameQueryField;

        /**
         * LDAP field in the query result containing the user's role.
         */
        private String roleResultField;

    }

    @Getter @Setter
    public static class IsbnDb {

        private TitleSearch titleSearch;

        /**
         * IsbnDb.com API key for pricing information
         */
        private String apiKey;
        
        @Getter @Setter
        public static class TitleSearch {

            /**
             * Filters the title matches on contributor as well, matching any part of the contributor's name.
             */
            private boolean filterOnContributor;

        }
        
    }

    @Getter @Setter
    public static class LibrarianCallNumbers {

        /**
         * Base URL of a web service that retrieves a list of librarians given a call number
         */
        private String baseUrl;

    }

    @Getter @Setter
    public static class Email {

        /**
         * From address to use in email notifications.
         */
        private String fromAddress;

        /**
         * List of comma-separated emailed addresses to notify when a new purchase request is submitted.
         */
        private String purchaseRequestedAddresses;

    }

    @Getter @Setter
    public static class GoogleSheets {

        private OutputType matchMarc;
        private OutputType fullRecord;

        /**
         * Path to the google-sheets-client-secret.json file used to connect to the API.
         */
        private String credentialsFilePath;
        
        /**
         * Header text for the column of ISBNs.
         */
        private String isbnColumnHeader;

        @Getter @Setter
        public static class OutputType {

            /**
             * ID of the Google Sheets spreadsheet on which to write the ISBNs of requested items.
             */
            private String requestedSpreadsheetId;

            /**
             * ID of the Google Sheets spreadsheet on which to write the ISBNs of approved items.
             */
            private String approvedSpreadsheetId;
            
        }

    }

}


