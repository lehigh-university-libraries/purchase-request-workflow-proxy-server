package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.google_sheets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class GoogleSheetsListener implements WorkflowServiceListener {

    private static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static String APPLICATION_NAME = "Purchase Request Workflow Proxy Server";
    static String VALUE_INPUT_OPTION_RAW = "RAW";

    String REQUESTED_SPREADSHEET_ID;
    String APPROVED_SPREADSHEET_ID;
    List<String> ALL_SHEETS_TO_TEST;
    private String CREDENTIALS_FILE_PATH;
    private String ISBN_COLUMN_HEADER;

    Config config;
    Sheets sheetsService;

    GoogleSheetsListener(WorkflowService workflowService, Config config) throws IOException, GeneralSecurityException {
        this.config = config;
        initMetadata();
        initConnection();

        workflowService.addListener(this);
    }

    void initMetadata() {
        CREDENTIALS_FILE_PATH = config.getGoogleSheets().getCredentialsFilePath();
        ISBN_COLUMN_HEADER = config.getGoogleSheets().getIsbnColumnHeader();
    }

    List<String> sheetsToTest(String[] ids) {
        List<String> sheets = new ArrayList<String>();
        for (int i=0; i < ids.length; i++) {
            if (ids[i] != null) {
                sheets.add(ids[i]);
            }
        }
        return sheets;
    }

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
        for (String spreadsheetId: ALL_SHEETS_TO_TEST) {
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, "A1:A1", body)
                .setValueInputOption(VALUE_INPUT_OPTION_RAW)
                .execute();
        }
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        if (REQUESTED_SPREADSHEET_ID != null) {
            log.debug("Writing purchase request.");
            writePurchase(purchaseRequest, REQUESTED_SPREADSHEET_ID);
        }
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        if (APPROVED_SPREADSHEET_ID != null) {
            log.debug("Writing approved purchase.");
            writePurchase(purchaseRequest, APPROVED_SPREADSHEET_ID);
        }
    }

    abstract void writePurchase(PurchaseRequest purchaseRequest, String spreadsheetId);

    static ValueRange valueRange(List<Object> row) {
        List<List<Object>> values = Arrays.asList(row);
        return new ValueRange().setValues(values);
    }

    static ValueRange singleValueRange(String value) {
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
            InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
            com.google.api.client.googleapis.auth.oauth2.GoogleCredential credential = 
                com.google.api.client.googleapis.auth.oauth2.GoogleCredential.fromStream(in).createScoped(SCOPES);
            in.close();
            return credential;
        }

    } 

}
