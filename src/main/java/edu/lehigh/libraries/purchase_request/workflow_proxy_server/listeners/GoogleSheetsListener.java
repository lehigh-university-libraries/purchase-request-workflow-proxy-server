package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleSheetsListener implements WorkflowServiceListener {

    private static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static String APPLICATION_NAME = "Purchase Request Workflow Proxy Server";
    private static String VALUE_INPUT_OPTION_RAW = "RAW";

    private String SPREADSHEET_ID;
    private String CREDENTIALS_FILE_PATH;
    private String ISBN_COLUMN_HEADER;

    private Config config;

    GoogleSheetsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        this.config = config;
        initMetadata();
        initConnection();

        workflowService.addListener(this);
        log.debug("GoogleSheetsListener listening.");
    }

    private void initMetadata() {
        SPREADSHEET_ID = config.getGoogleSheets().getSpreadsheetId();
        CREDENTIALS_FILE_PATH = config.getGoogleSheets().getCredentialsFilePath();
        ISBN_COLUMN_HEADER = config.getGoogleSheets().getIsbnColumnHeader();
    }

    private Sheets sheetsService;

    private void initConnection() throws GeneralSecurityException, IOException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = GoogleAuthorizeUtil.getCredential(CREDENTIALS_FILE_PATH, HTTP_TRANSPORT);
        sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
        
        confirmWritePermission(credential);
    }

    private void confirmWritePermission(Credential credential) throws IOException {
        // Set the column header as both a convenience and a start-time test of write permissions.
        ValueRange body = singleValueRange(ISBN_COLUMN_HEADER);
        sheetsService.spreadsheets().values()
            .update(SPREADSHEET_ID, "A1:A1", body)
            .setValueInputOption(VALUE_INPUT_OPTION_RAW)
            .execute();
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        // no op
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        try {
            String isbn = purchaseRequest.getIsbn();
            if (isbn == null) {
                log.warn("Purchase approved with empty ISBN; cannot add to Google Sheet.");
                return;
            }
            ValueRange body = singleValueRange(isbn);
            sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, "A1:A1", body)
                .setValueInputOption(VALUE_INPUT_OPTION_RAW)
                .execute();
            log.debug("Wrote approved purchase to Google Sheet: " + isbn);
        }
        catch (IOException ex) {
            log.error("Caught IOException: ", ex);
            return;
        }
    }

    private static ValueRange singleValueRange(String value) {
        List<List<Object>> values = Arrays.asList(
            Arrays.asList(new Object[] { value } )
        );
        return new ValueRange().setValues(values);
    }

    private static class GoogleAuthorizeUtil {

        private static List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

        /**
         * Return the credential to access Google APIs.
         * 
         * Ignores deprecation per https://stackoverflow.com/questions/64135720/how-do-you-access-a-google-sheet-with-a-service-account-from-java 
         */
        @SuppressWarnings("deprecation")
        public static Credential getCredential(final String CREDENTIALS_FILE_PATH, 
            final NetHttpTransport HTTP_TRANSPORT) throws IOException, GeneralSecurityException {
            InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            com.google.api.client.googleapis.auth.oauth2.GoogleCredential credential = 
                com.google.api.client.googleapis.auth.oauth2.GoogleCredential.fromStream(in).createScoped(SCOPES);
            return credential;
        }

    } 

}
